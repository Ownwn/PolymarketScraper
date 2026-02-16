
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class DeepDivePanel extends JPanel {
    private final JComboBox<String> marketSelector = new JComboBox<>();
    private final Map<String, String> titleToSlug = new HashMap<>();
    private final JLabel deepDiveLabel = new JLabel("Select a market to deep dive", SwingConstants.CENTER);
    private final DefaultTableModel orderBookModel = new DefaultTableModel(new Object[]{"Price", "Size", "Side"}, 0);
    private final DefaultTableModel volumePriceModel = new DefaultTableModel(new Object[]{"Price Level", "Total Volume ($)"}, 0);
    private final JLabel liquidityLabel = new JLabel("Liquidity: $0.00", SwingConstants.CENTER);

    public DeepDivePanel() {
        setLayout(new BorderLayout(20, 20));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel topSelection = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        marketSelector.setFont(new Font("Arial", Font.PLAIN, 20));
        marketSelector.setPreferredSize(new Dimension(800, 40));
        
        JButton loadBtn = new JButton("Load Market Stats");
        loadBtn.setFont(new Font("Arial", Font.BOLD, 20));
        loadBtn.addActionListener(e -> {
            String selected = (String) marketSelector.getSelectedItem();
            String slug = titleToSlug.get(selected);
            if (slug != null) {
                deepDiveLabel.setText("Loading: " + selected + "...");
                new Thread(() -> loadRealData(selected, slug)).start();
            }
        });

        topSelection.add(new JLabel("Select Market:") {{ setFont(new Font("Arial", Font.BOLD, 20)); }});
        topSelection.add(marketSelector);
        topSelection.add(loadBtn);
        add(topSelection, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new GridLayout(1, 3, 20, 20));
        
        // Order Book
        JTable obTable = new JTable(orderBookModel);
        obTable.setFont(new Font("Arial", Font.PLAIN, 18));
        obTable.setRowHeight(30);
        JScrollPane obScroll = new JScrollPane(obTable);
        obScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Order Book (Bids/Asks)", 0, 0, new Font("Arial", Font.BOLD, 20)));
        contentPanel.add(obScroll);

        // Liquidity & Info
        JPanel infoPanel = new JPanel(new GridLayout(4, 1, 10, 10));
        infoPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Market Liquidity & Info", 0, 0, new Font("Arial", Font.BOLD, 20)));
        liquidityLabel.setFont(new Font("Arial", Font.BOLD, 24));
        infoPanel.add(liquidityLabel);
        JLabel spreadLabel = new JLabel("Spread: -", SwingConstants.CENTER) {{ setFont(new Font("Arial", Font.PLAIN, 20)); }};
        JLabel volLabel = new JLabel("24h Volume: -", SwingConstants.CENTER) {{ setFont(new Font("Arial", Font.PLAIN, 20)); }};
        JLabel priceLabel = new JLabel("Last Price: -", SwingConstants.CENTER) {{ setFont(new Font("Arial", Font.PLAIN, 20)); }};
        infoPanel.add(spreadLabel);
        infoPanel.add(volLabel);
        infoPanel.add(priceLabel);
        contentPanel.add(infoPanel);

        // Volume at Price
        JTable vapTable = new JTable(volumePriceModel);
        vapTable.setFont(new Font("Arial", Font.PLAIN, 18));
        vapTable.setRowHeight(30);
        JScrollPane vapScroll = new JScrollPane(vapTable);
        vapScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Volume by Price Level", 0, 0, new Font("Arial", Font.BOLD, 20)));
        contentPanel.add(vapScroll);

        add(contentPanel, BorderLayout.CENTER);
        
        deepDiveLabel.setFont(new Font("Arial", Font.ITALIC, 28));
        add(deepDiveLabel, BorderLayout.SOUTH);
    }

    private void loadRealData(String title, String slug) {
        try {
            JsonObject market = Http.getJsonObject("https://gamma-api.polymarket.com/markets/slug/" + slug);
            
            Json tokenData = market.get("clobTokenIds");
            JsonArray tokens = null;
            
            if (tokenData instanceof JsonArray ja) {
                tokens = ja;
            } else if (tokenData instanceof JsonString js) {
                // If it's a string, try to parse it as a JSON array
                Json parsed = Json.parse(js.inner());
                if (parsed instanceof JsonArray ja) tokens = ja;
            }

            if (tokens == null || tokens.elements().isEmpty()) {
                SwingUtilities.invokeLater(() -> deepDiveLabel.setText("No tokens found for: " + title));
                return;
            }
            
            String yesTokenId = ((JsonString) tokens.elements().get(0)).inner();
            JsonObject book = Http.getJsonObject("https://clob.polymarket.com/book?token_id=" + yesTokenId);
            JsonArray asks = book.getArray("asks");
            JsonArray bids = book.getArray("bids");

            SwingUtilities.invokeLater(() -> {
                orderBookModel.setRowCount(0);
                
                // Collect and sort asks descending
                List<JsonObject> askList = new ArrayList<>();
                if (asks != null) {
                    for (Json e : asks.elements()) askList.add((JsonObject) e);
                }
                askList.sort((a, b) -> Double.compare(Double.parseDouble(b.getString("price")), Double.parseDouble(a.getString("price"))));
                
                // Collect and sort bids descending
                List<JsonObject> bidList = new ArrayList<>();
                if (bids != null) {
                    for (Json e : bids.elements()) bidList.add((JsonObject) e);
                }
                bidList.sort((a, b) -> Double.compare(Double.parseDouble(b.getString("price")), Double.parseDouble(a.getString("price"))));

                double bestAsk = -1, bestBid = -1;

                // Add top 5 asks (will be the lowest prices at the bottom of the ask section)
                int askStart = Math.max(0, askList.size() - 5);
                for (int i = askStart; i < askList.size(); i++) {
                    JsonObject entry = askList.get(i);
                    String p = entry.getString("price");
                    if (i == askList.size() - 1) bestAsk = Double.parseDouble(p);
                    orderBookModel.addRow(new Object[]{p, entry.getString("size"), "ASK"});
                }

                // Add top 5 bids (highest prices at the top of the bid section)
                for (int i = 0; i < Math.min(5, bidList.size()); i++) {
                    JsonObject entry = bidList.get(i);
                    String p = entry.getString("price");
                    if (i == 0) bestBid = Double.parseDouble(p);
                    orderBookModel.addRow(new Object[]{p, entry.getString("size"), "BID"});
                }

                liquidityLabel.setText("Liquidity: " + formatVal(market.getString("liquidity")));
                deepDiveLabel.setText("Deep Dive: " + title);
                
                Component[] comps = ((JPanel) ((JPanel) getComponent(1)).getComponent(1)).getComponents();
                if (bestAsk != -1 && bestBid != -1) {
                    ((JLabel)comps[1]).setText(String.format("Spread: %.4f", bestAsk - bestBid));
                }
                ((JLabel)comps[2]).setText("24h Vol: " + formatVal(market.getString("volume24h")));
                ((JLabel)comps[3]).setText("Last Price: " + formatVal(market.getString("lastTradePrice")));

                volumePriceModel.setRowCount(0);
                volumePriceModel.addRow(new Object[]{"Total", formatVal(market.getString("volume"))});
            });

        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> deepDiveLabel.setText("Error loading data: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    private String formatVal(String val) {
        if (val == null || val.equals("null")) return "0.00";
        try {
            double d = Double.parseDouble(val);
            return String.format("$%,.2f", d);
        } catch (Exception e) {
            return val;
        }
    }

    public void updateMarketSelector(Map<String, String> markets) {
        String currentSelection = (String) marketSelector.getSelectedItem();
        DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) marketSelector.getModel();
        boolean changed = false;
        for (String title : markets.keySet()) {
            titleToSlug.put(title, markets.get(title));
            if (model.getIndexOf(title) == -1) {
                model.addElement(title);
                changed = true;
            }
        }
        if (changed && currentSelection != null) {
            marketSelector.setSelectedItem(currentSelection);
        }
    }
}
