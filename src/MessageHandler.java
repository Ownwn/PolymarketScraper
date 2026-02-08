import java.util.*;

public class MessageHandler {
    private Map<String, Double> orders = new HashMap<>();
    private List<Transaction> orderQueue = Collections.synchronizedList(new LinkedList<>());

    public void handle(CharSequence charSequence) {
        if (charSequence.toString().trim().isBlank()) return;
        JsonObject req = Json.parseObject(charSequence.toString()).getObj("payload");
        String title = req.getString("title");
        double price = req.getDouble("price");
        double value = price * req.getDouble("size");

        if (value < 5) return;

        Transaction transaction = new Transaction(title, req.getString("name"), req.getString("slug"), req.getString("side"), req.getLong("timestamp"), value);

        orderQueue.add(transaction);

    }

    public void tallyTransactions() {
        while (!orderQueue.isEmpty()) {
            Transaction t = orderQueue.removeFirst();
            //orders.putIfAbsent(t.slug, 0d);
            orders.merge(t.slug, t.value, Double::sum);
        }
    }

    public void printTransactions() {
//        orderQueue.forEach(t -> System.out.println(t.pretty()));
        tallyTransactions();
        var biggest = orders.entrySet().stream().max(Comparator.comparingDouble(Map.Entry::getValue)).orElse(null);
//        orders.forEach((key, value) -> System.out.println(value));
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
            return user + " " + getPrettySide() + " $" + value + " of \"" + title + "\"" + slug;
        }

        public boolean buySide() {
            return side.equals("BUY");
        }
    }

}
