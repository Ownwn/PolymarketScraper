import java.awt.*;
import java.util.*;
import java.util.List;

public class MessageHandler {
    private Map<String, Double> orders = new HashMap<>();
    private List<Transaction> orderQueue = Collections.synchronizedList(new LinkedList<>());
    private java.util.Deque<Transaction> hourlyTransactions = new java.util.concurrent.ConcurrentLinkedDeque<>();
    private Map<String, String> userToFullAddress = new HashMap<>();
    private final DataLogger logger = new DataLogger();
    private List<String> excludedPatterns = new ArrayList<>(List.of("-5m-", "-15m-", "Bitcoin", "Ethereum", "Solana", "XRP"));
    private ScraperUI ui;

    public void setUI(ScraperUI ui) {
        this.ui = ui;
    }

    public void setExcludedPatterns(String patterns) {
        excludedPatterns.clear();
        for (String p : patterns.split(",")) {
            if (!p.trim().isEmpty()) excludedPatterns.add(p.trim());
        }
    }

    public String getExcludedPatternsString() {
        return String.join(", ", excludedPatterns);
    }

    private boolean isExcluded(Transaction t) {
        for (String p : excludedPatterns) {
            if (t.slug.contains(p) || t.title.contains(p)) return true;
        }
        return false;
    }

    public void handle(CharSequence charSequence) {
        handle(charSequence, true);
    }

    public void handle(CharSequence charSequence, boolean shouldLog) {
        String raw = charSequence.toString().trim();
        if (raw.isBlank()) return;
        
        if (shouldLog) logger.logRaw(raw);
        
        try {
            Json parsed = Json.parse(raw);
            if (!(parsed instanceof JsonObject root)) return;
            
            JsonObject payload = root.getObj("payload");
            if (payload == null) return;

            String title = payload.getString("title");
            double price = payload.getDouble("price");
            double size = payload.getDouble("size");
            double value = price * size;

            if (value < 5) return;

            String user = "Unknown";
            String rawAddress = null;
            Json wallet = payload.get("proxyWallet");
            if (wallet instanceof JsonString ws) {
                rawAddress = ws.inner();
            }

            Json name = payload.get("name");
            if (name instanceof JsonString js && !js.inner().isBlank()) {
                user = js.inner();
            } else if (rawAddress != null) {
                user = rawAddress;
                if (user.length() > 10) {
                    user = user.substring(0, 6) + "..." + user.substring(user.length() - 4);
                }
            }

            if (rawAddress != null) {
                userToFullAddress.put(user, rawAddress);
            }

            long ts = payload.getLong("timestamp") * 1000;
            
            // FILTER: If the trade is older than 5 minutes relative to NOW, 
            // it's a late-settled resolved market trade. Ignore it.
            if (System.currentTimeMillis() - ts > 300 * 1000) {
                return;
            }

            Transaction transaction = new Transaction(title, user, payload.getString("slug"), payload.getString("side"), ts, value);

            if (shouldLog) logger.logTransaction(transaction);

            orderQueue.add(transaction);
            hourlyTransactions.add(transaction);

            if (ui != null && !isExcluded(transaction)) {
                ui.addLog(transaction.pretty());
            }
        } catch (Exception e) {
            System.err.println("Error parsing message: " + e.getMessage());
        }
    }

    public void pruneOldTransactions() {
        long cutoff = System.currentTimeMillis() - 3600 * 1000;
        // Use removeIf to ensure ALL old transactions are removed, even if they were appended out of order
        hourlyTransactions.removeIf(t -> t.timestamp < cutoff);
    }

    public void clearStats() {
        hourlyTransactions.clear();
        orderQueue.clear();
        userToFullAddress.clear();
    }

    public List<Map.Entry<String, Transaction>> getTopBoughtStats() {
        pruneOldTransactions();
        return getHourlyMap(true).entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue().value, a.getValue().value))
                .limit(15)
                .toList();
    }

    public List<Map.Entry<String, Transaction>> getTopSoldStats() {
        pruneOldTransactions();
        return getHourlyMap(false).entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue().value, a.getValue().value))
                .limit(15)
                .toList();
    }

    private Map<String, Transaction> getHourlyMap(boolean buy) {
        Map<String, Transaction> hourlyOrders = new HashMap<>();
        for (Transaction t : hourlyTransactions) {
            if (t.buySide() == buy && !isExcluded(t)) {
                hourlyOrders.merge(t.slug, t, (t1, t2) -> new Transaction(t1.title, t1.user, t1.slug, t1.side, t1.timestamp, t1.value + t2.value));
            }
        }
        return hourlyOrders;
    }

    public String getMostBoughtInstrumentLastHour() {
        pruneOldTransactions();
        Map<String, Transaction> hourlyOrders = getHourlyMap(true);
        var biggest = hourlyOrders.entrySet().stream().max(Comparator.comparingDouble(e -> e.getValue().value)).orElse(null);
        if (biggest == null) return "No trades in the last hour";
        return biggest.getValue().title + " for $" + String.format("%.2f", biggest.getValue().value);
    }

    public String getMostBoughtInstrumentSlugLastHour() {
        pruneOldTransactions();
        Map<String, Transaction> hourlyOrders = getHourlyMap(true);
        var biggest = hourlyOrders.entrySet().stream().max(Comparator.comparingDouble(e -> e.getValue().value)).orElse(null);
        if (biggest == null) return null;
        return biggest.getValue().slug;
    }

    public List<Map.Entry<String, Double>> getTopSpenders() {
        pruneOldTransactions();
        Map<String, Double> spenders = new HashMap<>();
        for (Transaction t : hourlyTransactions) {
            if (t.buySide() && !isExcluded(t)) {
                spenders.merge(t.user, t.value, Double::sum);
            }
        }
        return spenders.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(10)
                .toList();
    }

    public List<Transaction> getRecentTradesForUser(String user) {
        return hourlyTransactions.stream()
                .filter(t -> t.user.equals(user) && !isExcluded(t))
                .sorted((a, b) -> Long.compare(b.timestamp, a.timestamp))
                .limit(20)
                .toList();
    }

    public String getFullAddressForUser(String user) {
        return userToFullAddress.get(user);
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
            return String.format("%s %s $%.2f of \"%s\"", user, getPrettySide(), value, title);
        }

        public boolean buySide() {
            return side.equals("BUY");
        }
    }

}
