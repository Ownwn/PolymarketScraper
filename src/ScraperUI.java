import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
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
    private final DefaultTableModel smartModel = new DefaultTableModel(new Object[]{"Smart Trader", "PnL ($)", "Vol ($)", "address"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTextArea logArea = new JTextArea(15, 60);
    private boolean paused = false;
    private String smartPeriod = "WEEK";
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final JComboBox<String> marketSelector = new JComboBox<>();
    private final JLabel deepDiveLabel = new JLabel("Select a market to deep dive", SwingConstants.CENTER);
    private final DefaultTableModel orderBookModel = new DefaultTableModel(new Object[]{"Price", "Size", "Side"}, 0);
    private final DefaultTableModel volumePriceModel = new DefaultTableModel(new Object[]{"Price Level", "Total Volume ($)"}, 0);
    private final JLabel liquidityLabel = new JLabel("Liquidity: $0.00", SwingConstants.CENTER);

    private final LeaderboardTracker leaderboard = new LeaderboardTracker();

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
            messageHandler.getStats().clear();
            refresh();
            addLog("--- Stats Cleared ---");
        });

        topPanel.add(pauseBtn);
        topPanel.add(loadLogBtn);
        topPanel.add(filterBtn);
        topPanel.add(clearBtn);

        JComboBox<String> periodBox = new JComboBox<>(new String[]{"WEEK", "MONTH"});
        periodBox.setFont(new Font("Arial", Font.BOLD, 24));
        periodBox.addActionListener(e -> {
            smartPeriod = (String) periodBox.getSelectedItem();
            new Thread(() -> leaderboard.refresh(smartPeriod)).start();
        });
        topPanel.add(new JLabel("Smart Period:") {{ setFont(new Font("Arial", Font.BOLD, 20)); }});
        topPanel.add(periodBox);

        add(topPanel, BorderLayout.NORTH);

        // Dashboard Panel
        JPanel dashboardPanel = new JPanel(new BorderLayout(0, 15));

        // Center Panel - Tables
        JPanel centerPanel = new JPanel(new GridLayout(2, 2, 20, 20));
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
                String address = messageHandler.getStats().getFullAddress(user);
                
                if (e.getClickCount() == 1) {
                    showUserTrades(user, address);
                } else if (e.getClickCount() == 2) {
                    if (address != null) {
                        JLink.openWebpage("https://polymarket.com/profile/" + address);
                    }
                }
            }
        });
        JScrollPane spenderScroll = new JScrollPane(spenderTable);
        spenderScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Top Spenders (1h) - Click for trades", 0, 0, new Font("Arial", Font.BOLD, 20)));
        centerPanel.add(spenderScroll);

        JTable smartTable = new JTable(smartModel);
        smartTable.setFont(new Font("Arial", Font.PLAIN, 20));
        smartTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 22));
        smartTable.setRowHeight(35);
        smartTable.removeColumn(smartTable.getColumnModel().getColumn(3)); // Hide address
        smartTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                int row = smartTable.rowAtPoint(e.getPoint());
                if (row == -1) return;
                String addr = (String) smartModel.getValueAt(row, 3);
                String user = (String) smartModel.getValueAt(row, 0);
                if (e.getClickCount() == 1) {
                    showUserTrades(user, addr);
                } else if (e.getClickCount() == 2) {
                    JLink.openWebpage("https://polymarket.com/profile/" + addr);
                }
            }
        });
        JScrollPane smartScroll = new JScrollPane(smartTable);
        smartScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Smart Traders (Weekly Profitable) - Double Click Profile", 0, 0, new Font("Arial", Font.BOLD, 20)));
        centerPanel.add(smartScroll);

        dashboardPanel.add(centerPanel, BorderLayout.CENTER);

        // Bottom Panel - Log
        logArea.setEditable(false);
        logArea.setBackground(Color.BLACK);
        logArea.setForeground(Color.GREEN);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 24));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Live Trade Log", 0, 0, new Font("Arial", Font.BOLD, 20)));
        dashboardPanel.add(logScroll, BorderLayout.SOUTH);

        tabbedPane.setFont(new Font("Arial", Font.BOLD, 24));
        tabbedPane.addTab("Dashboard", dashboardPanel);
        tabbedPane.addTab("Market Deep Dive", createDeepDivePanel());

        add(tabbedPane, BorderLayout.CENTER);

        setSize(1800, 1200);
        setLocationRelativeTo(null);

        javax.swing.Timer timer = new javax.swing.Timer(2000, e -> refresh());
        timer.start();

        javax.swing.Timer leaderboardTimer = new javax.swing.Timer(300000, e -> new Thread(() -> leaderboard.refresh(smartPeriod)).start());
        leaderboardTimer.start();

        new Thread(() -> leaderboard.refresh(smartPeriod)).start();
    }

    private void showUserTrades(String user, String address) {
        new Thread(() -> {
            List<Transaction> trades = messageHandler.getStats().getRecentTradesForUser(user, t -> !messageHandler.isExcluded(t));
            
            if (trades.isEmpty() && address != null) {
                addLog("--- Fetching recent trades for " + user + " from API ---");
                trades = LeaderboardTracker.fetchRecentTrades(address);
            }

            final List<Transaction> finalTrades = trades;
            SwingUtilities.invokeLater(() -> {
                if (finalTrades.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No recent trades found for " + user);
                    return;
                }
                
                JDialog dialog = new JDialog(this, "Trades for " + user, true);
                dialog.setLayout(new BorderLayout());
                
                String[] cols = {"Time", "Side", "Instrument", "Value", "Action"};
                DefaultTableModel model = new DefaultTableModel(cols, 0) {
                    @Override public boolean isCellEditable(int row, int column) { return column == 4; }
                };
                
                for (var t : finalTrades) {
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
                
                table.getColumn("Action").setCellRenderer(new ButtonRenderer());
                table.getColumn("Action").setCellEditor(new ButtonEditor(new JCheckBox(), v -> {
                    int row = table.getSelectedRow();
                    if (row != -1) openManualTrade(finalTrades.get(row));
                }));

                dialog.add(new JScrollPane(table), BorderLayout.CENTER);
                dialog.setSize(1000, 600);
                dialog.setLocationRelativeTo(this);
                dialog.setVisible(true);
            });
        }).start();
    }

    private void openManualTrade(Transaction t) {
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
        TransactionStats stats = messageHandler.getStats();
        java.util.function.Predicate<Transaction> filter = t -> !messageHandler.isExcluded(t);

        List<Map.Entry<String, Transaction>> buys = stats.getTopBoughtStats(filter);
        buyModel.setRowCount(0);
        for (var entry : buys) {
            buyModel.addRow(new Object[]{entry.getValue().title(), String.format("%.2f", entry.getValue().value()), entry.getKey()});
        }

        List<Map.Entry<String, Transaction>> sells = stats.getTopSoldStats(filter);
        sellModel.setRowCount(0);
        for (var entry : sells) {
            sellModel.addRow(new Object[]{entry.getValue().title(), String.format("%.2f", entry.getValue().value()), entry.getKey()});
        }

        List<Map.Entry<String, Double>> spenders = stats.getTopSpenders(filter);
        spenderModel.setRowCount(0);
        for (var entry : spenders) {
            spenderModel.addRow(new Object[]{entry.getKey(), String.format("%.2f", entry.getValue())});
        }

        List<LeaderboardTracker.Entry> smarts = leaderboard.getSmartTraders();
        smartModel.setRowCount(0);
        for (var s : smarts) {
            smartModel.addRow(new Object[]{s.userName(), String.format("%.2f", s.pnl()), String.format("%.2f", s.vol()), s.proxyWallet()});
        }

        // Update Market Selector
        String currentSelection = (String) marketSelector.getSelectedItem();
        Set<String> markets = new TreeSet<>();
        for (var entry : buys) markets.add(entry.getValue().title());
        for (var entry : sells) markets.add(entry.getValue().title());
        
        if (markets.size() > 0) {
            DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) marketSelector.getModel();
            boolean changed = false;
            for (String m : markets) {
                if (model.getIndexOf(m) == -1) {
                    model.addElement(m);
                    changed = true;
                }
            }
            if (changed && currentSelection != null) {
                marketSelector.setSelectedItem(currentSelection);
            }
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
            messageHandler.getStats().clear();
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

    private JPanel createDeepDivePanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel topSelection = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        marketSelector.setFont(new Font("Arial", Font.PLAIN, 20));
        marketSelector.setPreferredSize(new Dimension(800, 40));
        
        JButton loadBtn = new JButton("Load Market Stats");
        loadBtn.setFont(new Font("Arial", Font.BOLD, 20));
        loadBtn.addActionListener(e -> {
            String selected = (String) marketSelector.getSelectedItem();
            if (selected != null) {
                deepDiveLabel.setText("Deep Dive: " + selected);
                loadPlaceholderData(selected);
            }
        });

        topSelection.add(new JLabel("Select Market:") {{ setFont(new Font("Arial", Font.BOLD, 20)); }});
        topSelection.add(marketSelector);
        topSelection.add(loadBtn);
        panel.add(topSelection, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new GridLayout(1, 3, 20, 20));
        
        // Order Book
        JTable obTable = new JTable(orderBookModel);
        obTable.setFont(new Font("Arial", Font.PLAIN, 18));
        obTable.setRowHeight(30);
        JScrollPane obScroll = new JScrollPane(obTable);
        obScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Order Book (Bids/Asks)", 0, 0, new Font("Arial", Font.BOLD, 20)));
        contentPanel.add(obScroll);

        // Liquidity & Info
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        infoPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Market Liquidity & Info", 0, 0, new Font("Arial", Font.BOLD, 20)));
        liquidityLabel.setFont(new Font("Arial", Font.BOLD, 24));
        infoPanel.add(liquidityLabel);
        infoPanel.add(new JLabel("Spread: Placeholder", SwingConstants.CENTER) {{ setFont(new Font("Arial", Font.PLAIN, 20)); }});
        infoPanel.add(new JLabel("24h Volume: Placeholder", SwingConstants.CENTER) {{ setFont(new Font("Arial", Font.PLAIN, 20)); }});
        contentPanel.add(infoPanel);

        // Volume at Price
        JTable vapTable = new JTable(volumePriceModel);
        vapTable.setFont(new Font("Arial", Font.PLAIN, 18));
        vapTable.setRowHeight(30);
        JScrollPane vapScroll = new JScrollPane(vapTable);
        vapScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Volume by Price Level", 0, 0, new Font("Arial", Font.BOLD, 20)));
        contentPanel.add(vapScroll);

        panel.add(contentPanel, BorderLayout.CENTER);
        
        deepDiveLabel.setFont(new Font("Arial", Font.ITALIC, 28));
        panel.add(deepDiveLabel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadPlaceholderData(String marketTitle) {
        orderBookModel.setRowCount(0);
        orderBookModel.addRow(new Object[]{"0.55", "1500", "ASK"});
        orderBookModel.addRow(new Object[]{"0.54", "800", "ASK"});
        orderBookModel.addRow(new Object[]{"0.52", "1200", "BID"});
        orderBookModel.addRow(new Object[]{"0.51", "3000", "BID"});

        volumePriceModel.setRowCount(0);
        volumePriceModel.addRow(new Object[]{"0.50-0.55", "15400.00"});
        volumePriceModel.addRow(new Object[]{"0.45-0.50", "8200.50"});
        volumePriceModel.addRow(new Object[]{"0.40-0.45", "3100.00"});

        liquidityLabel.setText("Liquidity: $45,230.00");
    }
}
