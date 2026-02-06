public class MessageHandler {


    public static void handle(CharSequence charSequence) {
        if (charSequence.toString().trim().isBlank()) return;
        JsonObject req = Json.parseObject(charSequence.toString()).getObj("payload");
        String title = req.getString("title");
        double price = req.getDouble("price");
        double value = price * req.getDouble("size");

        if (value < 5) return;

        String side = switch (req.getString("side").intern()) {
            case "BUY" -> "bought";
            case "SELL" -> "sold";
            default -> throw new IllegalStateException("Unexpected value: " + req.getString("side").intern());
        };

        String user = req.getString("name");

        System.out.println(user + " " + side + " $" + value + " of \"" + title + "\"");
    }

    record Transaction(String slug, long timestamp) {}

}
