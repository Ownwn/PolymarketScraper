import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Vector;

public class TradingPanel extends JPanel {
    private final JTable tradesTable;
    private final DefaultTableModel tableModel;

    public TradingPanel() {
        setLayout(new BorderLayout());

        // top panel
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = new JButton("Refresh Trades");
        refreshButton.addActionListener(e -> refreshTrades());
        topPanel.add(refreshButton);
        add(topPanel, BorderLayout.NORTH);

        // trades table
        String[] columnNames = {"ID", "Price", "Size", "Side", "Timestamp"};
        tableModel = new DefaultTableModel(columnNames, 0);
        tradesTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(tradesTable);
        add(scrollPane, BorderLayout.CENTER);

        refreshTrades();
    }

    private void refreshTrades() {
        new Thread(() -> {
            try {
                JsonArray trades = Http.getJsonArray("http://localhost:3000/trades");
                SwingUtilities.invokeLater(() -> {
                    tableModel.setRowCount(0);
                    if (trades != null) {
                        for (Json tradeElement : trades.elements()) {
                            if (tradeElement instanceof JsonObject trade) {
                                Vector<Object> row = new Vector<>();
                                String id = trade.getString("id");
                                row.add(id != null ? id : "N/A");
                                String price = trade.getString("price");
                                row.add(price != null ? price : "N/A");
                                String size = trade.getString("size");
                                row.add(size != null ? size : "N/A");
                                String side = trade.getString("side");
                                row.add(side != null ? side : "N/A");
                                String timestamp = trade.getString("timestamp");
                                row.add(timestamp != null ? timestamp : "N/A");
                                tableModel.addRow(row);
                            }
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this, "Error fetching trades: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
                );
            }
        }).start();
    }
}
