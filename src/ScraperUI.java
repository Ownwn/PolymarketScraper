import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class ScraperUI extends JFrame {
    private final MessageHandler messageHandler;
    private final DefaultTableModel buyModel = new DefaultTableModel(new Object[]{"Instrument", "Buy Vol ($)", "slug"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final DefaultTableModel sellModel = new DefaultTableModel(new Object[]{"Instrument", "Sell Vol ($)", "slug"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final DefaultTableModel spenderModel = new DefaultTableModel(new Object[]{"User (Wallet)", "Hourly Volume ($)"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTextArea logArea = new JTextArea(15, 60);
    private boolean paused = false;

    private final PolymarketAPI api = new PolymarketAPI();

    public ScraperUI(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
        setTitle("Polymarket Live Trade Tracker");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(15, 15));

        // Top Panel
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton pauseBtn = new JButton("Pause Feed");
        pauseBtn.setFont(new Font("Arial", Font.BOLD, 24));
        pauseBtn.addActionListener(e -> {
            paused = !paused;
            pauseBtn.setText(paused ? "Resume Feed" : "Pause Feed");
            pauseBtn.setBackground(paused ? Color.RED : null);
        });

        JButton loadLogBtn = new JButton("Load Raw Log");
        loadLogBtn.setFont(new Font("Arial", Font.BOLD, 24));
        loadLogBtn.addActionListener(e -> loadLog());

        JButton filterBtn = new JButton("Filters");
        filterBtn.setFont(new Font("Arial", Font.BOLD, 24));
        filterBtn.addActionListener(e -> showFilters());

        JButton clearBtn = new JButton("Clear Stats");
        clearBtn.setFont(new Font("Arial", Font.BOLD, 24));
        clearBtn.addActionListener(e -> {
            messageHandler.clearStats();
            refresh();
            addLog("--- Stats Cleared ---");
        });

        topPanel.add(pauseBtn);
        topPanel.add(loadLogBtn);
        topPanel.add(filterBtn);
        topPanel.add(clearBtn);
        add(topPanel, BorderLayout.NORTH);
        
        // ... rest of constructor ...

        // Center Panel - Tables
        JPanel centerPanel = new JPanel(new GridLayout(1, 3, 20, 20));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));

        JTable buyTable = new JTable(buyModel);
        buyTable.setFont(new Font("Arial", Font.PLAIN, 20));
        buyTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 22));
        buyTable.setRowHeight(35);
        buyTable.removeColumn(buyTable.getColumnModel().getColumn(2)); // Hide slug
        buyTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = buyTable.rowAtPoint(e.getPoint());
                    if (row != -1) {
                        String slug = (String) buyModel.getValueAt(row, 2);
                        addLog(">>> Opening: " + buyModel.getValueAt(row, 0));
                        JLink.openWebpage("https://polymarket.com/market/" + slug);
                    }
                }
            }
        });
        JScrollPane buyScroll = new JScrollPane(buyTable);
        buyScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Top Bought (1h) - Double click to visit", 0, 0, new Font("Arial", Font.BOLD, 20)));
        centerPanel.add(buyScroll);

        JTable sellTable = new JTable(sellModel);
        sellTable.setFont(new Font("Arial", Font.PLAIN, 20));
        sellTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 22));
        sellTable.setRowHeight(35);
        sellTable.removeColumn(sellTable.getColumnModel().getColumn(2)); // Hide slug
        sellTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = sellTable.rowAtPoint(e.getPoint());
                    if (row != -1) {
                        String slug = (String) sellModel.getValueAt(row, 2);
                        addLog(">>> Opening: " + sellModel.getValueAt(row, 0));
                        JLink.openWebpage("https://polymarket.com/market/" + slug);
                    }
                }
            }
        });
        JScrollPane sellScroll = new JScrollPane(sellTable);
        sellScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Top Sold (1h) - Double click to visit", 0, 0, new Font("Arial", Font.BOLD, 20)));
        centerPanel.add(sellScroll);

        JTable spenderTable = new JTable(spenderModel);
        spenderTable.setFont(new Font("Arial", Font.PLAIN, 20));
        spenderTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 22));
        spenderTable.setRowHeight(35);
        spenderTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        spenderTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                int row = spenderTable.rowAtPoint(e.getPoint());
                if (row == -1) return;
                spenderTable.setRowSelectionInterval(row, row);
                String user = (String) spenderModel.getValueAt(row, 0);
                
                if (e.getClickCount() == 1) {
                    showUserTrades(user);
                } else if (e.getClickCount() == 2) {
                    String fullAddress = messageHandler.getFullAddressForUser(user);
                    if (fullAddress != null) {
                        JLink.openWebpage("https://polymarket.com/profile/" + fullAddress);
                    }
                }
            }
        });
        JScrollPane spenderScroll = new JScrollPane(spenderTable);
        spenderScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Top Spenders (1h) - Click for trades", 0, 0, new Font("Arial", Font.BOLD, 20)));
        centerPanel.add(spenderScroll);

        add(centerPanel, BorderLayout.CENTER);

        // Bottom Panel - Log
        logArea.setEditable(false);
        logArea.setBackground(Color.BLACK);
        logArea.setForeground(Color.GREEN);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 24));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Live Trade Log", 0, 0, new Font("Arial", Font.BOLD, 20)));
        add(logScroll, BorderLayout.SOUTH);

        setSize(1800, 1200);
        setLocationRelativeTo(null);

        Timer timer = new Timer(2000, e -> refresh());
        timer.start();
    }

    private void showUserTrades(String user) {
        List<MessageHandler.Transaction> trades = messageHandler.getRecentTradesForUser(user);
        
        JDialog dialog = new JDialog(this, "Trades for " + user, true);
        dialog.setLayout(new BorderLayout());
        
        String[] cols = {"Time", "Side", "Instrument", "Value", "Action"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int column) { return column == 4; }
        };
        
        for (var t : trades) {
            model.addRow(new Object[]{
                new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date(t.timestamp())),
                t.side(),
                t.title(),
                String.format("$%.2f", t.value()),
                "VIEW"
            });
        }
        
        JTable table = new JTable(model);
        table.setFont(new Font("Arial", Font.PLAIN, 18));
        table.setRowHeight(40);
        
        // Add View Button to the table
        table.getColumn("Action").setCellRenderer(new ButtonRenderer());
        table.getColumn("Action").setCellEditor(new ButtonEditor(new JCheckBox(), t -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                openManualTrade(trades.get(row));
            }
        }));

        dialog.add(new JScrollPane(table), BorderLayout.CENTER);
        dialog.setSize(1000, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void openManualTrade(MessageHandler.Transaction t) {
        String slug = t.slug();
        addLog(">>> Opening: " + t.title() + " (" + t.side() + ")");
        
        // Use /market/ for specific trades as it's more reliable for individual contract slugs
        JLink.openWebpage("https://polymarket.com/market/" + slug);
    }

    // Helper classes for Button in Table
    class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() { setOpaque(true); }
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private String label;
        private java.util.function.Consumer<Void> action;

        public ButtonEditor(JCheckBox checkBox, java.util.function.Consumer<Void> action) {
            super(checkBox);
            this.action = action;
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            return button;
        }

        public Object getCellEditorValue() {
            action.accept(null);
            return label;
        }
    }

    private void refresh() {
        if (paused) return;
        List<Map.Entry<String, MessageHandler.Transaction>> buys = messageHandler.getTopBoughtStats();
        buyModel.setRowCount(0);
        for (var entry : buys) {
            buyModel.addRow(new Object[]{entry.getValue().title(), String.format("%.2f", entry.getValue().value()), entry.getKey()});
        }

        List<Map.Entry<String, MessageHandler.Transaction>> sells = messageHandler.getTopSoldStats();
        sellModel.setRowCount(0);
        for (var entry : sells) {
            sellModel.addRow(new Object[]{entry.getValue().title(), String.format("%.2f", entry.getValue().value()), entry.getKey()});
        }

        List<Map.Entry<String, Double>> spenders = messageHandler.getTopSpenders();
        spenderModel.setRowCount(0);
        for (var entry : spenders) {
            spenderModel.addRow(new Object[]{entry.getKey(), String.format("%.2f", entry.getValue())});
        }
    }

    private void showFilters() {
        String current = messageHandler.getExcludedPatternsString();
        String input = JOptionPane.showInputDialog(this, 
            "Enter comma-separated patterns to exclude (slug or title):\n(e.g. -5m-, -15m-, sport, election)", 
            current);
        if (input != null) {
            messageHandler.setExcludedPatterns(input);
            refresh();
            addLog("--- Filters Updated: " + messageHandler.getExcludedPatternsString() + " ---");
        }
    }

    private void loadLog() {
        String date = JOptionPane.showInputDialog(this, "Enter date to load (yyyy-MM-dd):", 
                java.time.LocalDate.now().toString());
        if (date != null && !date.isBlank()) {
            messageHandler.clearStats();
            new Thread(() -> {
                try {
                    List<String> lines = DataLogger.readRawLog(date);
                    if (lines.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "No log found for " + date);
                        return;
                    }
                    addLog("--- Replaying log from " + date + " (" + lines.size() + " messages) ---");
                    for (String line : lines) {
                        messageHandler.handle(line, false);
                    }
                    addLog("--- Finished replaying log ---");
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Error loading log: " + e.getMessage());
                }
            }).start();
        }
    }

    public void addLog(String message) {
        if (paused && !message.startsWith("---")) return; // Always allow system messages like loading logs
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            if (logArea.getLineCount() > 100) {
                try {
                    int end = logArea.getLineStartOffset(1);
                    logArea.replaceRange("", 0, end);
                } catch (Exception ignored) {}
            }
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}
