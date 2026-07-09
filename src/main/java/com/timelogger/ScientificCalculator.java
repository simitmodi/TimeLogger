package com.timelogger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.time.format.DateTimeFormatter;
import java.util.Stack;

public class ScientificCalculator extends JDialog {
    private final JTextField historyField;
    private final JTextField displayField;
    private final JRadioButton degRadio;
    private final JRadioButton radRadio;
    
    private double memoryValue = 0.0;
    private boolean isDeg = true;
    private int posX = 0;
    private int posY = 0;
    private boolean isFreshInput = true;

    public ScientificCalculator(Frame owner) {
        super(owner, "Scientific Calculator", false);
        setUndecorated(true);
        setSize(820, 540);
        setLocationRelativeTo(owner);
        
        // Root panel with border
        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBorder(BorderFactory.createLineBorder(new Color(120, 120, 120), 2));
        rootPanel.setBackground(new Color(230, 230, 230));
        
        // 1. Custom Title Bar matching screenshot
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(new Color(43, 130, 217)); // Bright blue
        titleBar.setPreferredSize(new Dimension(getWidth(), 42));
        
        JLabel titleLabel = new JLabel("  Scientific Calculator");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        titleLabel.setForeground(Color.WHITE);
        titleBar.add(titleLabel, BorderLayout.WEST);
        
        JPanel titleControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        titleControls.setOpaque(false);
        
        JButton helpBtn = new JButton("Help");
        helpBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        helpBtn.setForeground(Color.WHITE);
        helpBtn.setBackground(new Color(75, 163, 227)); // Lighter blue
        helpBtn.setFocusPainted(false);
        helpBtn.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        helpBtn.addActionListener(e -> showHelpDialog());
        titleControls.add(helpBtn);
        
        // Minimize Button
        JLabel minBtn = new JLabel("—", SwingConstants.CENTER);
        minBtn.setFont(new Font("SansSerif", Font.BOLD, 16));
        minBtn.setForeground(Color.WHITE);
        minBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        minBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setState(Frame.ICONIFIED);
            }
        });
        titleControls.add(minBtn);
        
        // Close Button
        JLabel closeBtn = new JLabel("✕", SwingConstants.CENTER);
        closeBtn.setFont(new Font("SansSerif", Font.BOLD, 16));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setVisible(false);
            }
        });
        titleControls.add(closeBtn);
        
        titleBar.add(titleControls, BorderLayout.EAST);
        
        // Draggable window behaviour
        titleBar.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                posX = e.getX();
                posY = e.getY();
            }
        });
        titleBar.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                setLocation(e.getXOnScreen() - posX, e.getYOnScreen() - posY);
            }
        });
        
        rootPanel.add(titleBar, BorderLayout.NORTH);
        
        // 2. Display area (both displays right-aligned for GATE virtual calculator)
        JPanel displayPanel = new JPanel(new GridLayout(2, 1, 4, 4));
        displayPanel.setBackground(new Color(230, 230, 230));
        displayPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        historyField = new JTextField();
        historyField.setEditable(false);
        historyField.setFocusable(false);
        historyField.setHorizontalAlignment(JTextField.RIGHT);
        historyField.setFont(new Font("Monospaced", Font.PLAIN, 18));
        historyField.setBackground(Color.WHITE);
        historyField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));
        displayPanel.add(historyField);
        
        displayField = new JTextField("0");
        displayField.setEditable(false);
        displayField.setFocusable(false);
        displayField.setHorizontalAlignment(JTextField.RIGHT);
        displayField.setFont(new Font("Monospaced", Font.BOLD, 28));
        displayField.setBackground(Color.WHITE);
        displayField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));
        displayPanel.add(displayField);
        
        // Disable keyboard input completely by consuming key events
        KeyAdapter keyFilter = new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                e.consume();
            }
            @Override
            public void keyPressed(KeyEvent e) {
                e.consume();
            }
        };
        historyField.addKeyListener(keyFilter);
        displayField.addKeyListener(keyFilter);
        this.addKeyListener(keyFilter);
        
        // 3. Grid of buttons
        JPanel buttonsPanel = new JPanel(new GridBagLayout());
        buttonsPanel.setBackground(new Color(230, 230, 230));
        buttonsPanel.setBorder(new EmptyBorder(0, 10, 10, 10));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        
        // ROW 0: mod, radio buttons, MC, MR, MS, M+, M-
        gbc.gridy = 0;
        
        JButton modBtn = createStyledButton("mod", new Color(240, 240, 240));
        gbc.gridx = 0; gbc.gridwidth = 1;
        buttonsPanel.add(modBtn, gbc);
        
        // Deg/Rad Radio panel
        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        radioPanel.setOpaque(false);
        degRadio = new JRadioButton("Deg", true);
        degRadio.setFont(new Font("SansSerif", Font.PLAIN, 12));
        degRadio.setOpaque(false);
        degRadio.setFocusable(false);
        degRadio.addActionListener(e -> isDeg = true);
        
        radRadio = new JRadioButton("Rad", false);
        radRadio.setFont(new Font("SansSerif", Font.PLAIN, 12));
        radRadio.setOpaque(false);
        radRadio.setFocusable(false);
        radRadio.addActionListener(e -> isDeg = false);
        
        ButtonGroup radioGroup = new ButtonGroup();
        radioGroup.add(degRadio);
        radioGroup.add(radRadio);
        
        radioPanel.add(degRadio);
        radioPanel.add(radRadio);
        
        gbc.gridx = 1; gbc.gridwidth = 4;
        buttonsPanel.add(radioPanel, gbc);
        
        // Memory buttons MC, MR, MS, M+, M-
        String[] memLabels = {"MC", "MR", "MS", "M+", "M-"};
        for (int i = 0; i < memLabels.length; i++) {
            JButton btn = createStyledButton(memLabels[i], new Color(240, 240, 240));
            gbc.gridx = 5 + i; gbc.gridwidth = 1;
            buttonsPanel.add(btn, gbc);
        }
        
        // ROW 1: sinh, cosh, tanh, Exp, (, ), <- (red), C (red), +/- (red), sqrt
        gbc.gridy = 1;
        
        JButton sinhBtn = createStyledButton("sinh", new Color(240, 240, 240));
        gbc.gridx = 0; gbc.gridwidth = 1;
        buttonsPanel.add(sinhBtn, gbc);
        
        JButton coshBtn = createStyledButton("cosh", new Color(240, 240, 240));
        gbc.gridx = 1;
        buttonsPanel.add(coshBtn, gbc);
        
        JButton tanhBtn = createStyledButton("tanh", new Color(240, 240, 240));
        gbc.gridx = 2;
        buttonsPanel.add(tanhBtn, gbc);
        
        JButton expBtn = createStyledButton("Exp", new Color(240, 240, 240));
        gbc.gridx = 3;
        buttonsPanel.add(expBtn, gbc);
        
        JButton openParenBtn = createStyledButton("(", new Color(240, 240, 240));
        gbc.gridx = 4;
        buttonsPanel.add(openParenBtn, gbc);
        
        JButton closeParenBtn = createStyledButton(")", new Color(240, 240, 240));
        gbc.gridx = 5;
        buttonsPanel.add(closeParenBtn, gbc);
        
        JButton backspaceBtn = createStyledButton("←", new Color(229, 57, 53)); // Red
        backspaceBtn.setForeground(Color.WHITE);
        gbc.gridx = 6; gbc.gridwidth = 2;
        buttonsPanel.add(backspaceBtn, gbc);
        
        JButton clearBtn = createStyledButton("C", new Color(229, 57, 53)); // Red
        clearBtn.setForeground(Color.WHITE);
        gbc.gridx = 8; gbc.gridwidth = 1;
        buttonsPanel.add(clearBtn, gbc);
        
        JButton negateBtn = createStyledButton("+/-", new Color(229, 57, 53)); // Red
        negateBtn.setForeground(Color.WHITE);
        gbc.gridx = 9;
        buttonsPanel.add(negateBtn, gbc);
        
        JButton sqrtBtn = createStyledButton("√", new Color(240, 240, 240));
        gbc.gridx = 10;
        buttonsPanel.add(sqrtBtn, gbc);
        
        // ROW 2: sinh-1, cosh-1, tanh-1, log2, ln, log, 7, 8, 9, /, %
        gbc.gridy = 2; gbc.gridwidth = 1;
        
        JButton asinhBtn = createStyledButton("sinh⁻¹", new Color(240, 240, 240));
        gbc.gridx = 0; buttonsPanel.add(asinhBtn, gbc);
        
        JButton acoshBtn = createStyledButton("cosh⁻¹", new Color(240, 240, 240));
        gbc.gridx = 1; buttonsPanel.add(acoshBtn, gbc);
        
        JButton atanhBtn = createStyledButton("tanh⁻¹", new Color(240, 240, 240));
        gbc.gridx = 2; buttonsPanel.add(atanhBtn, gbc);
        
        JButton log2Btn = createStyledButton("log₂x", new Color(240, 240, 240));
        gbc.gridx = 3; buttonsPanel.add(log2Btn, gbc);
        
        JButton lnBtn = createStyledButton("ln", new Color(240, 240, 240));
        gbc.gridx = 4; buttonsPanel.add(lnBtn, gbc);
        
        JButton logBtn = createStyledButton("log", new Color(240, 240, 240));
        gbc.gridx = 5; buttonsPanel.add(logBtn, gbc);
        
        JButton btn7 = createStyledButton("7", Color.WHITE);
        gbc.gridx = 6; buttonsPanel.add(btn7, gbc);
        
        JButton btn8 = createStyledButton("8", Color.WHITE);
        gbc.gridx = 7; buttonsPanel.add(btn8, gbc);
        
        JButton btn9 = createStyledButton("9", Color.WHITE);
        gbc.gridx = 8; buttonsPanel.add(btn9, gbc);
        
        JButton divBtn = createStyledButton("/", new Color(240, 240, 240));
        gbc.gridx = 9; buttonsPanel.add(divBtn, gbc);
        
        JButton pctBtn = createStyledButton("%", new Color(240, 240, 240));
        gbc.gridx = 10; buttonsPanel.add(pctBtn, gbc);
        
        // ROW 3: pi, e, n!, logy, e^x, 10^x, 4, 5, 6, *, 1/x
        gbc.gridy = 3;
        
        JButton piBtn = createStyledButton("π", new Color(240, 240, 240));
        gbc.gridx = 0; buttonsPanel.add(piBtn, gbc);
        
        JButton eBtn = createStyledButton("e", new Color(240, 240, 240));
        gbc.gridx = 1; buttonsPanel.add(eBtn, gbc);
        
        JButton factBtn = createStyledButton("n!", new Color(240, 240, 240));
        gbc.gridx = 2; buttonsPanel.add(factBtn, gbc);
        
        JButton logyBtn = createStyledButton("logʸx", new Color(240, 240, 240));
        gbc.gridx = 3; buttonsPanel.add(logyBtn, gbc);
        
        JButton expXBtn = createStyledButton("eˣ", new Color(240, 240, 240));
        gbc.gridx = 4; buttonsPanel.add(expXBtn, gbc);
        
        JButton tenXBtn = createStyledButton("10ˣ", new Color(240, 240, 240));
        gbc.gridx = 5; buttonsPanel.add(tenXBtn, gbc);
        
        JButton btn4 = createStyledButton("4", Color.WHITE);
        gbc.gridx = 6; buttonsPanel.add(btn4, gbc);
        
        JButton btn5 = createStyledButton("5", Color.WHITE);
        gbc.gridx = 7; buttonsPanel.add(btn5, gbc);
        
        JButton btn6 = createStyledButton("6", Color.WHITE);
        gbc.gridx = 8; buttonsPanel.add(btn6, gbc);
        
        JButton mulBtn = createStyledButton("*", new Color(240, 240, 240));
        gbc.gridx = 9; buttonsPanel.add(mulBtn, gbc);
        
        JButton recipBtn = createStyledButton("1/x", new Color(240, 240, 240));
        gbc.gridx = 10; buttonsPanel.add(recipBtn, gbc);
        
        // ROW 4: sin, cos, tan, x^y, x^3, x^2, 1, 2, 3, -, =
        gbc.gridy = 4;
        
        JButton sinBtn = createStyledButton("sin", new Color(240, 240, 240));
        gbc.gridx = 0; buttonsPanel.add(sinBtn, gbc);
        
        JButton cosBtn = createStyledButton("cos", new Color(240, 240, 240));
        gbc.gridx = 1; buttonsPanel.add(cosBtn, gbc);
        
        JButton tanBtn = createStyledButton("tan", new Color(240, 240, 240));
        gbc.gridx = 2; buttonsPanel.add(tanBtn, gbc);
        
        JButton xToYBtn = createStyledButton("xʸ", new Color(240, 240, 240));
        gbc.gridx = 3; buttonsPanel.add(xToYBtn, gbc);
        
        JButton xTo3Btn = createStyledButton("x³", new Color(240, 240, 240));
        gbc.gridx = 4; buttonsPanel.add(xTo3Btn, gbc);
        
        JButton xTo2Btn = createStyledButton("x²", new Color(240, 240, 240));
        gbc.gridx = 5; buttonsPanel.add(xTo2Btn, gbc);
        
        JButton btn1 = createStyledButton("1", Color.WHITE);
        gbc.gridx = 6; buttonsPanel.add(btn1, gbc);
        
        JButton btn2 = createStyledButton("2", Color.WHITE);
        gbc.gridx = 7; buttonsPanel.add(btn2, gbc);
        
        JButton btn3 = createStyledButton("3", Color.WHITE);
        gbc.gridx = 8; buttonsPanel.add(btn3, gbc);
        
        JButton subBtn = createStyledButton("-", new Color(240, 240, 240));
        gbc.gridx = 9; buttonsPanel.add(subBtn, gbc);
        
        // Equals button (spans 2 rows)
        JButton eqBtn = createStyledButton("=", new Color(46, 125, 50)); // Green
        eqBtn.setForeground(Color.WHITE);
        eqBtn.setFont(new Font("SansSerif", Font.BOLD, 26));
        gbc.gridx = 10; gbc.gridheight = 2;
        buttonsPanel.add(eqBtn, gbc);
        
        // ROW 5: sin-1, cos-1, tan-1, y-root, cbrt, |x|, 0 (double-width), ., +
        gbc.gridy = 5; gbc.gridheight = 1;
        
        JButton asinBtn = createStyledButton("sin⁻¹", new Color(240, 240, 240));
        gbc.gridx = 0; buttonsPanel.add(asinBtn, gbc);
        
        JButton acosBtn = createStyledButton("cos⁻¹", new Color(240, 240, 240));
        gbc.gridx = 1; buttonsPanel.add(acosBtn, gbc);
        
        JButton atanBtn = createStyledButton("tan⁻¹", new Color(240, 240, 240));
        gbc.gridx = 2; buttonsPanel.add(atanBtn, gbc);
        
        JButton yRootBtn = createStyledButton("ʸ√x", new Color(240, 240, 240));
        gbc.gridx = 3; buttonsPanel.add(yRootBtn, gbc);
        
        JButton cbrtBtn = createStyledButton("³√", new Color(240, 240, 240));
        gbc.gridx = 4; buttonsPanel.add(cbrtBtn, gbc);
        
        JButton absBtn = createStyledButton("|x|", new Color(240, 240, 240));
        gbc.gridx = 5; buttonsPanel.add(absBtn, gbc);
        
        JButton btn0 = createStyledButton("0", Color.WHITE);
        gbc.gridx = 6; gbc.gridwidth = 2;
        buttonsPanel.add(btn0, gbc);
        
        JButton dotBtn = createStyledButton(".", Color.WHITE);
        gbc.gridx = 8; gbc.gridwidth = 1;
        buttonsPanel.add(dotBtn, gbc);
        
        JButton addBtn = createStyledButton("+", new Color(240, 240, 240));
        gbc.gridx = 9; buttonsPanel.add(addBtn, gbc);
        
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);
        contentPanel.add(displayPanel, BorderLayout.NORTH);
        contentPanel.add(buttonsPanel, BorderLayout.CENTER);
        rootPanel.add(contentPanel, BorderLayout.CENTER);
        
        setContentPane(rootPanel);
        
        // Add Button Action Handlers
        mapActionListeners(
            modBtn, sinhBtn, coshBtn, tanhBtn, expBtn, openParenBtn, closeParenBtn, backspaceBtn, clearBtn, negateBtn, sqrtBtn,
            asinhBtn, acoshBtn, atanhBtn, log2Btn, lnBtn, logBtn, btn7, btn8, btn9, divBtn, pctBtn,
            piBtn, eBtn, factBtn, logyBtn, expXBtn, tenXBtn, btn4, btn5, btn6, mulBtn, recipBtn,
            sinBtn, cosBtn, tanBtn, xToYBtn, xTo3Btn, xTo2Btn, btn1, btn2, btn3, subBtn,
            asinBtn, acosBtn, atanBtn, yRootBtn, cbrtBtn, absBtn, btn0, dotBtn, addBtn, eqBtn
        );
    }
    
    private void setState(int state) {
        // Iconify owner frame since JDialog is undecorated
        if (getOwner() instanceof Frame) {
            ((Frame) getOwner()).setState(state);
        }
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text) {
            private boolean isHovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        isHovered = true;
                        repaint();
                    }
                    @Override
                    public void mouseExited(MouseEvent e) {
                        isHovered = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                Color drawBg = bg;
                if (getModel().isPressed()) {
                    drawBg = getDarkerColor(bg);
                } else if (isHovered) {
                    drawBg = getHoverColor(bg);
                }
                
                g2.setColor(drawBg);
                g2.fillRect(0, 0, getWidth(), getHeight());
                
                // Thin border
                g2.setColor(new Color(180, 180, 180));
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        
        // Correct text visibility check
        if (bg.getRed() > 200 && bg.getGreen() < 100) { // Red buttons
            btn.setForeground(Color.WHITE);
        } else if (bg.getGreen() > 100 && bg.getRed() < 100) { // Green equals button
            btn.setForeground(Color.WHITE);
        } else if (bg.getBlue() > 200 && bg.getRed() < 100) { // Blue accent buttons (e.g. Help)
            btn.setForeground(Color.WHITE);
        } else {
            btn.setForeground(new Color(33, 33, 33)); // Dark text for white/gray buttons
        }
        
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
    
    private Color getHoverColor(Color c) {
        return new Color(
            Math.max(0, c.getRed() - 15),
            Math.max(0, c.getGreen() - 15),
            Math.max(0, c.getBlue() - 15)
        );
    }
    
    private Color getDarkerColor(Color c) {
        return new Color(
            Math.max(0, c.getRed() - 30),
            Math.max(0, c.getGreen() - 30),
            Math.max(0, c.getBlue() - 30)
        );
    }
    
    private void mapActionListeners(JButton... buttons) {
        for (JButton b : buttons) {
            b.addActionListener(e -> {
                String cmd = b.getText();
                handleInput(cmd);
            });
        }
    }
    
    private boolean isSingleArgFunc(String cmd) {
        return cmd.equals("sin") || cmd.equals("cos") || cmd.equals("tan") ||
               cmd.equals("sinh") || cmd.equals("cosh") || cmd.equals("tanh") ||
               cmd.equals("sin⁻¹") || cmd.equals("cos⁻¹") || cmd.equals("tan⁻¹") ||
               cmd.equals("sinh⁻¹") || cmd.equals("cosh⁻¹") || cmd.equals("tanh⁻¹") ||
               cmd.equals("ln") || cmd.equals("log") || cmd.equals("log₂x") ||
               cmd.equals("³√") || cmd.equals("|x|") || cmd.equals("√") ||
               cmd.equals("n!") || cmd.equals("1/x") || cmd.equals("x²") ||
               cmd.equals("x³") || cmd.equals("eˣ") || cmd.equals("10ˣ");
    }
    
    private String mapButtonToFunc(String btn) {
        switch (btn) {
            case "sin": return "sin";
            case "cos": return "cos";
            case "tan": return "tan";
            case "sin⁻¹": return "asin";
            case "cos⁻¹": return "acos";
            case "tan⁻¹": return "atan";
            case "sinh": return "sinh";
            case "cosh": return "cosh";
            case "tanh": return "tanh";
            case "sinh⁻¹": return "asinh";
            case "cosh⁻¹": return "acosh";
            case "tanh⁻¹": return "atanh";
            case "ln": return "ln";
            case "log": return "log";
            case "log₂x": return "log2";
            case "³√": return "cbrt";
            case "|x|": return "abs";
            case "√": return "sqrt";
            case "n!": return "fact";
            case "1/x": return "recip";
            case "x²": return "sqr";
            case "x³": return "cube";
            case "eˣ": return "exp";
            case "10ˣ": return "tenX";
            default: return "";
        }
    }
    
    private String getFuncHistoryString(String func, String valStr) {
        switch (func) {
            case "sin": return "sin(" + valStr + ")";
            case "cos": return "cos(" + valStr + ")";
            case "tan": return "tan(" + valStr + ")";
            case "asin": return "asin(" + valStr + ")";
            case "acos": return "acos(" + valStr + ")";
            case "atan": return "atan(" + valStr + ")";
            case "sinh": return "sinh(" + valStr + ")";
            case "cosh": return "cosh(" + valStr + ")";
            case "tanh": return "tanh(" + valStr + ")";
            case "asinh": return "asinh(" + valStr + ")";
            case "acosh": return "acosh(" + valStr + ")";
            case "atanh": return "atanh(" + valStr + ")";
            case "ln": return "ln(" + valStr + ")";
            case "log": return "log(" + valStr + ")";
            case "log2": return "log2(" + valStr + ")";
            case "sqrt": return "sqrt(" + valStr + ")";
            case "cbrt": return "cbrt(" + valStr + ")";
            case "abs": return "abs(" + valStr + ")";
            case "recip": return "recip(" + valStr + ")";
            case "fact": return "fact(" + valStr + ")";
            case "sqr": return "sqr(" + valStr + ")";
            case "cube": return "cube(" + valStr + ")";
            case "exp": return "exp(" + valStr + ")";
            case "tenX": return "pow10(" + valStr + ")";
            default: return valStr;
        }
    }
    
    private double calculateSingleArgFunc(String func, double val) {
        switch (func) {
            case "sin": return isDeg ? Math.sin(Math.toRadians(val)) : Math.sin(val);
            case "cos": return isDeg ? Math.cos(Math.toRadians(val)) : Math.cos(val);
            case "tan": return isDeg ? Math.tan(Math.toRadians(val)) : Math.tan(val);
            case "asin": 
                double rAsin = Math.asin(val);
                return isDeg ? Math.toDegrees(rAsin) : rAsin;
            case "acos":
                double rAcos = Math.acos(val);
                return isDeg ? Math.toDegrees(rAcos) : rAcos;
            case "atan":
                double rAtan = Math.atan(val);
                return isDeg ? Math.toDegrees(rAtan) : rAtan;
            case "sinh": return Math.sinh(val);
            case "cosh": return Math.cosh(val);
            case "tanh": return Math.tanh(val);
            case "asinh": return Math.log(val + Math.sqrt(val * val + 1));
            case "acosh": return Math.log(val + Math.sqrt(val * val - 1));
            case "atanh": return 0.5 * Math.log((1 + val) / (1 - val));
            case "ln": return Math.log(val);
            case "log": return Math.log10(val);
            case "log2": return Math.log(val) / Math.log(2);
            case "sqrt": return Math.sqrt(val);
            case "cbrt": return Math.cbrt(val);
            case "abs": return Math.abs(val);
            case "recip": return 1.0 / val;
            case "fact": return factorial((int) val);
            case "sqr": return val * val;
            case "cube": return val * val * val;
            case "exp": return Math.exp(val);
            case "tenX": return Math.pow(10, val);
            default: return val;
        }
    }

    private double factorial(int n) {
        if (n < 0) return Double.NaN;
        double f = 1;
        for (int i = 2; i <= n; i++) f *= i;
        return f;
    }
    
    private void handleInput(String cmd) {
        String history = historyField.getText().trim();
        String current = displayField.getText();
        
        if (cmd.equals("C")) {
            historyField.setText("");
            displayField.setText("0");
            isFreshInput = true;
        } else if (cmd.equals("←")) {
            if (!isFreshInput && current.length() > 0) {
                String next = current.substring(0, current.length() - 1);
                if (next.isEmpty() || next.equals("-")) next = "0";
                displayField.setText(next);
            }
        } else if (cmd.equals("+/-")) {
            if (!current.equals("0")) {
                if (current.startsWith("-")) {
                    displayField.setText(current.substring(1));
                } else {
                    displayField.setText("-" + current);
                }
            }
        } else if (cmd.equals("MC")) {
            memoryValue = 0.0;
        } else if (cmd.equals("MR")) {
            displayField.setText(formatResult(memoryValue));
            isFreshInput = true;
        } else if (cmd.equals("MS")) {
            try {
                memoryValue = Double.parseDouble(current);
            } catch (NumberFormatException ignored) {}
        } else if (cmd.equals("M+")) {
            try {
                memoryValue += Double.parseDouble(current);
            } catch (NumberFormatException ignored) {}
        } else if (cmd.equals("M-")) {
            try {
                memoryValue -= Double.parseDouble(current);
            } catch (NumberFormatException ignored) {}
        } else if (cmd.equals("=")) {
            // Evaluate history + current
            String exprToEvaluate = "";
            if (history.isEmpty()) {
                exprToEvaluate = current;
            } else if (history.endsWith("+") || history.endsWith("-") || history.endsWith("*") || history.endsWith("/") || history.endsWith("%") || history.endsWith("^") || history.endsWith("yroot")) {
                exprToEvaluate = history + " " + current;
            } else {
                exprToEvaluate = history;
            }
            try {
                double res = evaluateExpression(exprToEvaluate);
                displayField.setText(formatResult(res));
                // Set history to the evaluated formula without '=' or result
                historyField.setText(exprToEvaluate.replaceAll("\\s+", ""));
                isFreshInput = true;
            } catch (Exception ex) {
                displayField.setText("Error");
            }
        } else if (isSingleArgFunc(cmd)) {
            // Apply function to current value (Value first, Function second)
            try {
                double val = Double.parseDouble(current);
                String func = mapButtonToFunc(cmd);
                double res = calculateSingleArgFunc(func, val);
                displayField.setText(formatResult(res));
                
                // Update history
                String funcHist = getFuncHistoryString(func, current);
                if (history.isEmpty()) {
                    historyField.setText(funcHist);
                } else if (history.endsWith("+") || history.endsWith("-") || history.endsWith("*") || history.endsWith("/") || history.endsWith("%") || history.endsWith("^") || history.endsWith("yroot")) {
                    historyField.setText(history + " " + funcHist);
                } else {
                    historyField.setText(funcHist);
                }
                isFreshInput = true;
            } catch (Exception ex) {
                displayField.setText("Error");
            }
        } else if (cmd.equals("+") || cmd.equals("-") || cmd.equals("*") || cmd.equals("/") || cmd.equals("mod") || cmd.equals("%") || cmd.equals("xʸ") || cmd.equals("ʸ√x")) {
            // Binary operator
            String op = cmd;
            if (cmd.equals("xʸ")) op = "^";
            if (cmd.equals("ʸ√x")) op = "yroot";
            if (cmd.equals("mod")) op = "%";
            
            if (history.isEmpty()) {
                historyField.setText(current + " " + op);
            } else if (history.endsWith("+") || history.endsWith("-") || history.endsWith("*") || history.endsWith("/") || history.endsWith("%") || history.endsWith("^") || history.endsWith("yroot")) {
                // Replace last operator
                int lastSpace = history.lastIndexOf(' ');
                if (lastSpace != -1) {
                    historyField.setText(history.substring(0, lastSpace) + " " + op);
                } else {
                    historyField.setText(current + " " + op);
                }
            } else {
                historyField.setText(history + " " + op);
            }
            isFreshInput = true;
        } else if (cmd.equals("π")) {
            displayField.setText(formatResult(Math.PI));
            isFreshInput = true;
        } else if (cmd.equals("e")) {
            displayField.setText(formatResult(Math.E));
            isFreshInput = true;
        } else {
            // Digits, decimal point, parentheses
            if (cmd.equals("(") || cmd.equals(")")) {
                if (cmd.equals("(")) {
                    if (history.isEmpty()) {
                        historyField.setText("(");
                    } else {
                        historyField.setText(history + " (");
                    }
                } else {
                    historyField.setText(history + " " + current + ")");
                }
                isFreshInput = true;
            } else {
                // It is a digit or decimal point
                if (isFreshInput) {
                    if (cmd.equals(".")) {
                        displayField.setText("0.");
                    } else {
                        displayField.setText(cmd);
                    }
                    isFreshInput = false;
                } else {
                    if (cmd.equals(".") && current.contains(".")) {
                        // ignore
                    } else {
                        displayField.setText(current + cmd);
                    }
                }
            }
        }
    }
    
    private String formatResult(double res) {
        if (Double.isNaN(res)) return "Error";
        if (Double.isInfinite(res)) return "Infinity";
        if (res == (long) res) {
            return String.format("%d", (long) res);
        }
        return String.format(java.util.Locale.US, "%.8g", res);
    }
    
    private void showHelpDialog() {
        String msg = "GATE Virtual Calculator Guide:\n\n" +
                     "- Value First, Function Second:\n" +
                     "  To compute single-arg functions (e.g. sin(30), sqrt(16)):\n" +
                     "  1. Enter the value first (e.g., 30 or 16).\n" +
                     "  2. Click the function key (e.g., sin or √).\n" +
                     "  3. Result is computed instantly!\n\n" +
                     "- Double Display Right-Aligned:\n" +
                     "  Displays inputs and histories right-aligned for better visibility.\n\n" +
                     "- Trigonometry functions depend on Deg/Rad selection.\n" +
                     "- Keyboard input is completely disabled.";
        JOptionPane.showMessageDialog(this, msg, "GATE Calculator Help", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private double evaluateExpression(String expr) {
        String clean = expr.replaceAll("\\s+", "")
                           .replaceAll("mod", "%")
                           .replaceAll("π", "pi");
        
        Parser p = new Parser(clean, isDeg);
        return p.parse();
    }
    
    private static class Parser {
        private final String str;
        private final boolean useDegrees;
        private int pos = -1;
        private int ch;
        
        Parser(String str, boolean useDegrees) {
            this.str = str;
            this.useDegrees = useDegrees;
        }
        
        void nextChar() {
            ch = (++pos < str.length()) ? str.charAt(pos) : -1;
        }
        
        boolean eat(int charToEat) {
            while (ch == ' ') nextChar();
            if (ch == charToEat) {
                nextChar();
                return true;
            }
            return false;
        }
        
        double parse() {
            nextChar();
            double x = parseExpression();
            if (pos < str.length()) throw new RuntimeException("Unexpected character: " + (char)ch);
            return x;
        }
        
        double parseExpression() {
            double x = parseTerm();
            for (;;) {
                if      (eat('+')) x += parseTerm();
                else if (eat('-')) x -= parseTerm();
                else return x;
            }
        }
        
        double parseTerm() {
            double x = parseFactor();
            for (;;) {
                if      (eat('*')) x *= parseFactor();
                else if (eat('/')) x /= parseFactor();
                else if (eat('%')) x %= parseFactor();
                else return x;
            }
        }
        
        double parseFactor() {
            if (eat('+')) return parseFactor();
            if (eat('-')) return -parseFactor();
            
            double x;
            int startPos = this.pos;
            if (eat('(')) {
                x = parseExpression();
                eat(')');
            } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                x = Double.parseDouble(str.substring(startPos, this.pos));
            } else if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
                while ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9') || ch == '-') nextChar();
                String name = str.substring(startPos, this.pos);
                if (name.equalsIgnoreCase("pi")) {
                    x = Math.PI;
                } else if (name.equalsIgnoreCase("e")) {
                    x = Math.E;
                } else {
                    eat('(');
                    double arg1 = parseExpression();
                    double arg2 = 0;
                    if (eat(',')) {
                        arg2 = parseExpression();
                    }
                    eat(')');
                    
                    if (name.equals("sin")) {
                        x = useDegrees ? Math.sin(Math.toRadians(arg1)) : Math.sin(arg1);
                    } else if (name.equals("cos")) {
                        x = useDegrees ? Math.cos(Math.toRadians(arg1)) : Math.cos(arg1);
                    } else if (name.equals("tan")) {
                        x = useDegrees ? Math.tan(Math.toRadians(arg1)) : Math.tan(arg1);
                    } else if (name.equals("asin")) {
                        double rad = Math.asin(arg1);
                        x = useDegrees ? Math.toDegrees(rad) : rad;
                    } else if (name.equals("acos")) {
                        double rad = Math.acos(arg1);
                        x = useDegrees ? Math.toDegrees(rad) : rad;
                    } else if (name.equals("atan")) {
                        double rad = Math.atan(arg1);
                        x = useDegrees ? Math.toDegrees(rad) : rad;
                    } else if (name.equals("sinh")) {
                        x = Math.sinh(arg1);
                    } else if (name.equals("cosh")) {
                        x = Math.cosh(arg1);
                    } else if (name.equals("tanh")) {
                        x = Math.tanh(arg1);
                    } else if (name.equals("asinh")) {
                        x = Math.log(arg1 + Math.sqrt(arg1 * arg1 + 1));
                    } else if (name.equals("acosh")) {
                        x = Math.log(arg1 + Math.sqrt(arg1 * arg1 - 1));
                    } else if (name.equals("atanh")) {
                        x = 0.5 * Math.log((1 + arg1) / (1 - arg1));
                    } else if (name.equals("ln")) {
                        x = Math.log(arg1);
                    } else if (name.equals("log")) {
                        x = Math.log10(arg1);
                    } else if (name.equals("log2")) {
                        x = Math.log(arg1) / Math.log(2);
                    } else if (name.equals("logbase")) {
                        x = Math.log(arg1) / Math.log(arg2);
                    } else if (name.equals("sqrt")) {
                        x = Math.sqrt(arg1);
                    } else if (name.equals("cbrt")) {
                        x = Math.cbrt(arg1);
                    } else if (name.equals("yroot")) {
                        x = Math.pow(arg1, 1.0 / arg2);
                    } else if (name.equals("abs")) {
                        x = Math.abs(arg1);
                    } else if (name.equals("recip")) {
                        x = 1.0 / arg1;
                    } else if (name.equals("sqr")) {
                        x = arg1 * arg1;
                    } else if (name.equals("cube")) {
                        x = arg1 * arg1 * arg1;
                    } else if (name.equals("exp")) {
                        x = Math.exp(arg1);
                    } else if (name.equals("pow10")) {
                        x = Math.pow(10, arg1);
                    } else if (name.equals("fact")) {
                        x = factorial((int) arg1);
                    } else {
                        throw new RuntimeException("Unknown function: " + name);
                    }
                }
            } else {
                throw new RuntimeException("Unexpected character: " + (char)ch);
            }
            
            if (eat('^')) x = Math.pow(x, parseFactor());
            if (eat('!')) {
                x = factorial((int) x);
            }
            
            return x;
        }
        
        double factorial(int n) {
            if (n < 0) return Double.NaN;
            double f = 1;
            for (int i = 2; i <= n; i++) f *= i;
            return f;
        }
    }
}
