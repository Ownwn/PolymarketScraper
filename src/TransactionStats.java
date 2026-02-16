import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

public class TransactionStats {
    private final java.util.Deque<Transaction> hourlyTransactions = new ConcurrentLinkedDeque<>();
    private final Map<String, String> userToFullAddress = new HashMap<>();

    public void add(Transaction t) {
        hourlyTransactions.add(t);
    }

    public void prune() {
        long cutoff = System.currentTimeMillis() - 3600 * 1000;
        hourlyTransactions.removeIf(t -> t.timestamp() < cutoff);
    }

    public void clear() {
        hourlyTransactions.clear();
        userToFullAddress.clear();
    }

    public void trackAddress(String user, String fullAddress) {
        userToFullAddress.put(user, fullAddress);
    }

    public String getFullAddress(String user) {
        return userToFullAddress.get(user);
    }

    public List<Map.Entry<String, Transaction>> getTopBoughtStats(java.util.function.Predicate<Transaction> filter) {
        prune();
        return getHourlyMap(true, filter).entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue().value(), a.getValue().value()))
                .limit(15)
                .toList();
    }

    public List<Map.Entry<String, Transaction>> getTopSoldStats(java.util.function.Predicate<Transaction> filter) {
        prune();
        return getHourlyMap(false, filter).entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue().value(), a.getValue().value()))
                .limit(15)
                .toList();
    }

    private Map<String, Transaction> getHourlyMap(boolean buy, java.util.function.Predicate<Transaction> filter) {
        Map<String, Transaction> hourlyOrders = new HashMap<>();
        for (Transaction t : hourlyTransactions) {
            if (t.buySide() == buy && filter.test(t)) {
                hourlyOrders.merge(t.slug(), t, (t1, t2) -> new Transaction(t1.title(), t1.user(), t1.slug(), t1.side(), t1.timestamp(), t1.value() + t2.value()));
            }
        }
        return hourlyOrders;
    }

    public List<Map.Entry<String, Double>> getTopSpenders(java.util.function.Predicate<Transaction> filter) {
        prune();
        Map<String, Double> spenders = new HashMap<>();
        for (Transaction t : hourlyTransactions) {
            if (t.buySide() && filter.test(t)) {
                spenders.merge(t.user(), t.value(), Double::sum);
            }
        }
        return spenders.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(10)
                .toList();
    }

    public List<Transaction> getRecentTradesForUser(String user, java.util.function.Predicate<Transaction> filter) {
        return hourlyTransactions.stream()
                .filter(t -> t.user().equals(user) && filter.test(t))
                .sorted((a, b) -> Long.compare(b.timestamp(), a.timestamp()))
                .limit(20)
                .toList();
    }

    public Transaction getMostBought(java.util.function.Predicate<Transaction> filter) {
        prune();
        return getHourlyMap(true, filter).values().stream()
                .max(Comparator.comparingDouble(Transaction::value))
                .orElse(null);
    }
}
