import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class ScraperUI extends JFrame {
    private final MessageHandler messageHandler;
    private final JLabel topInstrumentLabel = new JLabel("Top Instrument: Loading...");
    private final DefaultTableModel tableModel = new DefaultTableModel(new Object[]{"Instrument (Slug)", "Hourly Buy Volume ($)"}, 0);
    private final JTextArea logArea = new JTextArea(10, 50);

    public ScraperUI(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
        setTitle("Polymarket Live Trade Tracker");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Top Panel
        JPanel topPanel = new JPanel(new BorderLayout());
        topInstrumentLabel.setFont(new Font("Arial", Font.BOLD, 32));
        topInstrumentLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        topPanel.add(topInstrumentLabel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        // Center Panel - Table
        JTable table = new JTable(tableModel);
        table.setFont(new Font("Arial", Font.BOLD, 16));
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Hourly Stats"));
        add(tableScroll, BorderLayout.CENTER);

        // Bottom Panel - Log
        logArea.setEditable(false);
        logArea.setBackground(Color.BLACK);
        logArea.setForeground(Color.GREEN);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 20));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Live Trade Log"));
        add(logScroll, BorderLayout.SOUTH);

        setSize(1400, 1000);
        setLocationRelativeTo(null);

        // Refresh timer
        Timer timer = new Timer(2000, e -> refresh());
        timer.start();
    }

    private void refresh() {
        // Update Top Label
        topInstrumentLabel.setText("Top Instrument (1h): " + messageHandler.getMostBoughtInstrumentLastHour());

        // Update Table
        List<Map.Entry<String, MessageHandler.Transaction>> stats = messageHandler.getHourlyStats();
        tableModel.setRowCount(0);
        for (var entry : stats) {
            tableModel.addRow(new Object[]{entry.getValue().title(), String.format("%.2f", entry.getValue().value())});
        }
    }

    public void addLog(String message) {
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
