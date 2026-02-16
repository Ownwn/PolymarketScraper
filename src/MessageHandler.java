import java.util.*;

public class MessageHandler {
    private final List<String> excludedPatterns = new ArrayList<>(List.of("-5m-", "-15m-", "Bitcoin", "Ethereum", "Solana", "XRP"));
    private final DataLogger logger = new DataLogger();
    private final TransactionStats stats = new TransactionStats();
    private ScraperUI ui;

    public void setUI(ScraperUI ui) {
        this.ui = ui;
    }

    public TransactionStats getStats() {
        return stats;
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

    public boolean isExcluded(Transaction t) {
        for (String p : excludedPatterns) {
            if (t.slug().contains(p) || t.title().contains(p)) return true;
        }
        // Past Date Filter (for sports/events)
        if (t.slug().matches(".*20[2-9][0-9]-[0-1][0-9]-[0-3][0-9].*")) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("20[2-9][0-9]-[0-1][0-9]-[0-3][0-9]").matcher(t.slug());
            if (m.find()) {
                String dateStr = m.group();
                String todayStr = java.time.LocalDate.now().toString();
                if (dateStr.compareTo(todayStr) < 0) return true;
            }
        }
        // Keywords for finished markets
        String lowerTitle = t.title().toLowerCase();
        String lowerSlug = t.slug().toLowerCase();
        if (lowerTitle.contains("completed") || lowerTitle.contains("resolved") || 
            lowerSlug.contains("completed") || lowerSlug.contains("resolved")) {
            return true;
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

            double price = payload.getDouble("price");
            if (price >= 0.99 || price <= 0.01) return;

            String title = payload.getString("title");
            double size = payload.getDouble("size");
            double value = price * size;
            if (value < 5) return;

            String user = "Unknown";
            String rawAddress = null;
            Json wallet = payload.get("proxyWallet");
            if (wallet instanceof JsonString ws) rawAddress = ws.inner();

            Json name = payload.get("name");
            if (name instanceof JsonString js && !js.inner().isBlank()) {
                user = js.inner();
            } else if (rawAddress != null) {
                user = rawAddress;
                if (user.length() > 10) user = user.substring(0, 6) + "..." + user.substring(user.length() - 4);
            }

            if (rawAddress != null) stats.trackAddress(user, rawAddress);

            long ts = payload.getLong("timestamp") * 1000;
            if (System.currentTimeMillis() - ts > 300 * 1000) return;

            Transaction transaction = new Transaction(title, user, payload.getString("slug"), payload.getString("side"), ts, value);
            if (shouldLog) logger.logTransaction(transaction);

            stats.add(transaction);

            if (ui != null && !isExcluded(transaction)) {
                ui.addLog(transaction.pretty());
            }
        } catch (Exception e) {
            System.err.println("Error parsing message: " + e.getMessage());
        }
    }
}