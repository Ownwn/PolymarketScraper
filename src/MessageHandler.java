import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

public class MessageHandler {
    private Map<String, Double> orders = new HashMap<>();
    private List<Transaction> orderQueue = Collections.synchronizedList(new LinkedList<>());
    private java.util.Deque<Transaction> hourlyTransactions = new java.util.concurrent.ConcurrentLinkedDeque<>();
    private ScraperUI ui;

    public void setUI(ScraperUI ui) {
        this.ui = ui;
    }

    public void handle(CharSequence charSequence) {
        if (charSequence.toString().trim().isBlank()) return;
        JsonObject req = Json.parseObject(charSequence.toString()).getObj("payload");
        String title = req.getString("title");
        double price = req.getDouble("price");
        double value = price * req.getDouble("size");

        if (value < 5) return;

        long ts = req.getLong("timestamp") * 1000;
        Transaction transaction = new Transaction(title, req.getString("name"), req.getString("slug"), req.getString("side"), ts, value);

        orderQueue.add(transaction);
        hourlyTransactions.add(transaction);

        if (ui != null) {
            try {
                ui.addLog(transaction.pretty());
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public void pruneOldTransactions() {
        long cutoff = System.currentTimeMillis() - 3600 * 1000;
        while (!hourlyTransactions.isEmpty() && hourlyTransactions.peekFirst().timestamp < cutoff) {
            hourlyTransactions.removeFirst();
        }
    }

    public String getMostBoughtInstrumentLastHour() {
        pruneOldTransactions();
        Map<String, Transaction> hourlyOrders = getHourlyMap();
        var biggest = hourlyOrders.entrySet().stream().max(Comparator.comparingDouble(e -> e.getValue().value)).orElse(null);
        if (biggest == null) return "No trades in the last hour";
        return biggest.getValue().title + " for $" + String.format("%.2f", biggest.getValue().value);
    }

    private Map<String, Transaction> getHourlyMap() {
        Map<String, Transaction> hourlyOrders = new HashMap<>();
        for (Transaction t : hourlyTransactions) {
            if (t.buySide()) {
                hourlyOrders.merge(t.slug, t, (t1, t2) -> new Transaction(t1.title, t1.user, t1.slug, t1.side, t1.timestamp, t1.value + t2.value));
            }
        }
        return hourlyOrders;
    }

    public List<Map.Entry<String, Transaction>> getHourlyStats() {
        pruneOldTransactions();
        return getHourlyMap().entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue().value, a.getValue().timestamp))
                .toList();
    }

    public void tallyTransactions() {
        while (!orderQueue.isEmpty()) {
            Transaction t = orderQueue.removeFirst();
            //orders.putIfAbsent(t.slug, 0d);
            orders.merge(t.slug, t.value, Double::sum);
        }
    }

    public void printTransactions() {
        tallyTransactions();
        var biggest = orders.entrySet().stream().max(Comparator.comparingDouble(Map.Entry::getValue)).orElse(null);

        JsonObject biggestMarket = Http.getJsonObject("https://gamma-api.polymarket.com/markets/slug/" + biggest.getKey());
        System.out.println(biggestMarket.getString("question") + " for $" + biggest.getValue());
    }

    record Transaction(String title, String user, String slug, String side, long timestamp, double value) {
        public String getPrettySide() {
            return switch (side.intern()) {
                case "BUY" -> "bought";
                case "SELL" -> "sold";
                default -> throw new IllegalStateException("Unexpected value: " + side);
            };
        }

        public String pretty() {
            return String.format(user + " " + getPrettySide() + " $%.2f" + " of \"" + title + "\"" + slug, value);
        }

        public boolean buySide() {
            return side.equals("BUY");
        }
    }

}
