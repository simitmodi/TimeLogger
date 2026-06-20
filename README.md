# Time Logger (Java Desktop App)

A Java Swing application for study/work tracking with:

- Stopwatch with subject selection before start
- Countdown timer with pause/resume/stop/reset
- Subject management in Settings
- Session logging to files in the same folder
- Weekly export on Monday (previous week) to `.xlsx` with analysis
- Monthly export on first day of month (previous month) to `.xlsx` with analysis

## Data Files (same folder where app runs)

- `subjects.txt` - subject list
- `sessions.log` - session history
- `timelog-report-YYYY-MM-DD.xlsx` - Monday export report

## Build Runnable JAR

Run in PowerShell:

```powershell
.\build-jar.ps1
```

Output:

- `dist\time-logger.jar`

## Run

One-click build and run (double-click in Explorer):

```powershell
.\start-time-logger.bat
```

```powershell
java -jar .\dist\time-logger.jar
```

Or:

```powershell
.\run-app.ps1
```
