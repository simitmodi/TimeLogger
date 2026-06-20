package com.timelogger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ExportService {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void exportReport(Path outputFile, List<SessionRecord> sessions) {
        exportReport(outputFile, sessions, "All Data");
    }

    public void exportReport(Path outputFile, List<SessionRecord> sessions, String reportPeriod) {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(outputFile))) {
            writeEntry(zip, "[Content_Types].xml", contentTypesXml());
            writeEntry(zip, "_rels/.rels", rootRelsXml());
            writeEntry(zip, "xl/workbook.xml", workbookXml());
            writeEntry(zip, "xl/_rels/workbook.xml.rels", workbookRelsXml());
            writeEntry(zip, "xl/styles.xml", stylesXml());
            writeEntry(zip, "xl/worksheets/sheet1.xml", sessionsSheetXml(sessions));
            writeEntry(zip, "xl/worksheets/sheet2.xml", analysisSheetXml(sessions, reportPeriod));
        } catch (IOException e) {
            throw new RuntimeException("Unable to export XLSX report", e);
        }
    }

    private String sessionsSheetXml(List<SessionRecord> sessions) {
        StringBuilder rows = new StringBuilder();
        int rowIndex = 1;

        appendHeaderRow(rows, rowIndex++, List.of(
            "Type", "Subject", "Start Time", "End Time", "Duration (sec)", "Duration (hh:mm:ss)"
        ));

        List<SessionRecord> sorted = sessions.stream()
            .sorted(Comparator.comparing(SessionRecord::getStartTime))
            .collect(Collectors.toList());

        for (SessionRecord session : sorted) {
            rows.append("<row r=\"").append(rowIndex).append("\">")
                .append(textCell(1, rowIndex, session.getType().name(), false))
                .append(textCell(2, rowIndex, session.getSubject(), false))
                .append(textCell(3, rowIndex, session.getStartTime().format(DATE_TIME_FORMAT), false))
                .append(textCell(4, rowIndex, session.getEndTime().format(DATE_TIME_FORMAT), false))
                .append(numberCell(5, rowIndex, session.getDurationSeconds(), false))
                .append(textCell(6, rowIndex, formatDuration(session.getDurationSeconds()), false))
                .append("</row>");
            rowIndex++;
        }

        return worksheetXml(rows.toString());
    }

    private String analysisSheetXml(List<SessionRecord> sessions, String reportPeriod) {
        StringBuilder rows = new StringBuilder();
        int rowIndex = 1;
        appendHeaderRow(rows, rowIndex++, List.of("Metric", "Value"));

        long totalDuration = sessions.stream().mapToLong(SessionRecord::getDurationSeconds).sum();

        rowIndex = writeMetric(rows, rowIndex, "Export Date", LocalDate.now().toString(), false);
        rowIndex = writeMetric(rows, rowIndex, "Report Period", reportPeriod, false);
        rowIndex = writeMetric(rows, rowIndex, "Total Sessions", String.valueOf(sessions.size()), false);
        rowIndex = writeMetric(rows, rowIndex, "Total Duration (sec)", String.valueOf(totalDuration), false);
        rowIndex = writeMetric(rows, rowIndex, "Total Duration", formatDuration(totalDuration), false);

        rowIndex++;
        rows.append("<row r=\"").append(rowIndex).append("\">")
            .append(textCell(1, rowIndex, "By Subject", true))
            .append("</row>");
        rowIndex++;

        Map<String, Long> bySubject = sessions.stream()
            .collect(Collectors.groupingBy(SessionRecord::getSubject, Collectors.summingLong(SessionRecord::getDurationSeconds)));
        for (Map.Entry<String, Long> entry : bySubject.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            rowIndex = writeMetric(rows, rowIndex, entry.getKey(), formatDuration(entry.getValue()), false);
        }

        rowIndex++;
        rows.append("<row r=\"").append(rowIndex).append("\">")
            .append(textCell(1, rowIndex, "By Type", true))
            .append("</row>");
        rowIndex++;

        Map<SessionRecord.SessionType, Long> byType = sessions.stream()
            .collect(Collectors.groupingBy(SessionRecord::getType, Collectors.summingLong(SessionRecord::getDurationSeconds)));
        for (Map.Entry<SessionRecord.SessionType, Long> entry : byType.entrySet()) {
            rowIndex = writeMetric(rows, rowIndex, entry.getKey().name(), formatDuration(entry.getValue()), false);
        }

        return worksheetXml(rows.toString());
    }

    private int writeMetric(StringBuilder rows, int rowIndex, String metric, String value, boolean header) {
        rows.append("<row r=\"").append(rowIndex).append("\">")
            .append(textCell(1, rowIndex, metric, header))
            .append(textCell(2, rowIndex, value, false))
            .append("</row>");
        return rowIndex + 1;
    }

    private void appendHeaderRow(StringBuilder rows, int rowIndex, List<String> headers) {
        rows.append("<row r=\"").append(rowIndex).append("\">");
        for (int i = 0; i < headers.size(); i++) {
            rows.append(textCell(i + 1, rowIndex, headers.get(i), true));
        }
        rows.append("</row>");
    }

    private String worksheetXml(String rowsXml) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
            + "<sheetData>" + rowsXml + "</sheetData>"
            + "</worksheet>";
    }

    private String textCell(int col, int row, String value, boolean header) {
        String ref = cellRef(col, row);
        int style = header ? 1 : 0;
        return "<c r=\"" + ref + "\" t=\"inlineStr\" s=\"" + style + "\"><is><t>" + escapeXml(value) + "</t></is></c>";
    }

    private String numberCell(int col, int row, long value, boolean header) {
        String ref = cellRef(col, row);
        int style = header ? 1 : 0;
        return "<c r=\"" + ref + "\" s=\"" + style + "\"><v>" + value + "</v></c>";
    }

    private String cellRef(int col, int row) {
        StringBuilder letter = new StringBuilder();
        int current = col;
        while (current > 0) {
            int remainder = (current - 1) % 26;
            letter.insert(0, (char) ('A' + remainder));
            current = (current - 1) / 26;
        }
        return letter + String.valueOf(row);
    }

    private String contentTypesXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
            + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
            + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
            + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
            + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
            + "<Override PartName=\"/xl/worksheets/sheet2.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
            + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
            + "</Types>";
    }

    private String rootRelsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
            + "</Relationships>";
    }

    private String workbookXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
            + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
            + "<sheets>"
            + "<sheet name=\"Sessions\" sheetId=\"1\" r:id=\"rId1\"/>"
            + "<sheet name=\"Analysis\" sheetId=\"2\" r:id=\"rId2\"/>"
            + "</sheets>"
            + "</workbook>";
    }

    private String workbookRelsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
            + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet2.xml\"/>"
            + "<Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>"
            + "</Relationships>";
    }

    private String stylesXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
            + "<fonts count=\"2\">"
            + "<font><sz val=\"11\"/><name val=\"Calibri\"/></font>"
            + "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font>"
            + "</fonts>"
            + "<fills count=\"1\"><fill><patternFill patternType=\"none\"/></fill></fills>"
            + "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>"
            + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
            + "<cellXfs count=\"2\">"
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>"
            + "</cellXfs>"
            + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
            + "</styleSheet>";
    }

    private void writeEntry(ZipOutputStream zip, String path, String content) {
        try {
            zip.putNextEntry(new ZipEntry(path));
            zip.write(content.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String escapeXml(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }

    private String formatDuration(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
