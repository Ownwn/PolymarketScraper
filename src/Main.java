static String test = "wss://echo.websocket.org";
static String url = "wss://ws-live-data.polymarket.com";

void main() {
    new MessageListener(url).startListening(c -> {
        if (c.toString().trim().isBlank()) return;
        JsonObject req = Json.parseObject(c.toString()).getObj("payload");
        String title = req.getString("title");
        double price = req.getDouble("price");
        double value = price * req.getDouble("size");

        if (value < 1000) return;

        String side = switch (req.getString("side").intern()) {
            case "BUY" -> "bought";
            case "SELL" -> "sold";
            default -> throw new IllegalStateException("Unexpected value: " + req.getString("side").intern());
        };

        String user = req.getString("name");

        System.out.println(user + " " + side + " $" + value + " of \"" + title + "\"");
    });
}