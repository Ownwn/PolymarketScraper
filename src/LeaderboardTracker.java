import java.util.*;
import java.util.stream.Collectors;

public class LeaderboardTracker {
    private static final String LEADERBOARD_URL = "https://data-api.polymarket.com/v1/leaderboard?timePeriod=%s&orderBy=VOL&limit=50";
    
    public record Entry(String userName, String proxyWallet, double vol, double pnl) {}

    private List<Entry> smartTraders = new ArrayList<>();

    public void refresh(String period) {
        try {
            String url = String.format(LEADERBOARD_URL, period);
            Json array = Json.parse(Http.getRaw(url));
            if (!(array instanceof JsonArray ja)) return;

            List<Entry> results = new ArrayList<>();
            for (Json item : ja.elements()) {
                if (item instanceof JsonObject obj) {
                    double pnl = obj.getDouble("pnl");
                    if (pnl > 0) { // Only profitable
                        results.add(new Entry(
                            obj.getString("userName"),
                            obj.getString("proxyWallet"),
                            obj.getDouble("vol"),
                            pnl
                        ));
                    }
                }
            }
            this.smartTraders = results;
        } catch (Exception e) {
            System.err.println("Error refreshing leaderboard: " + e.getMessage());
        }
    }

    public List<Entry> getSmartTraders() {
        return smartTraders;
    }

    public static List<Transaction> fetchRecentTrades(String address) {
        try {
            String url = "https://data-api.polymarket.com/activity?user=" + address + "&limit=20";
            Json array = Json.parse(Http.getRaw(url));
            if (!(array instanceof JsonArray ja)) return List.of();

            List<Transaction> trades = new ArrayList<>();
            for (Json item : ja.elements()) {
                if (item instanceof JsonObject obj) {
                    if (!obj.getString("type").equals("TRADE")) continue;
                    
                    trades.add(new Transaction(
                        obj.getString("title"),
                        obj.getString("name"),
                        obj.getString("slug"),
                        obj.getString("side"),
                        obj.getLong("timestamp") * 1000,
                        obj.getDouble("usdcSize")
                    ));
                }
            }
            return trades;
        } catch (Exception e) {
            System.err.println("Error fetching trades for " + address + ": " + e.getMessage());
            return List.of();
        }
    }
}
