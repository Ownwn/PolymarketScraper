package ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Set;
import java.util.TreeSet;

public class DeepDivePanel extends JPanel {
    private final JComboBox<String> marketSelector = new JComboBox<>();
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
            if (selected != null) {
                deepDiveLabel.setText("Deep Dive: " + selected);
                loadPlaceholderData(selected);
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

        add(contentPanel, BorderLayout.CENTER);
        
        deepDiveLabel.setFont(new Font("Arial", Font.ITALIC, 28));
        add(deepDiveLabel, BorderLayout.SOUTH);
    }

    public void updateMarketSelector(Set<String> markets) {
        String currentSelection = (String) marketSelector.getSelectedItem();
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
