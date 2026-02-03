static String test = "wss://echo.websocket.org";
static String url = "wss://ws-live-data.polymarket.com";
static Path testData = Path.of("testData.text");

void main() {
//    gatherTestData();
    testTime();
}

void gatherTestData() {
    new MessageListener(url).startListening(c -> {
        if (c.isEmpty()) return;
        try {
            Files.writeString(testData, c, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            Files.writeString(testData, "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    });
}

void testTime() {

    List<String> lines;
    try {
        lines = Files.readAllLines(testData);
    } catch (IOException e) {
        throw new RuntimeException(e);
    }
    long start = System.nanoTime();
        lines.forEach(Json::parse);
    long end = System.nanoTime();
    long time = (end - start) / 1_000_000;
    System.out.println(lines.size() + " lines took " + time + "ms");
}

void printSummaries() {
    new MessageListener(url).startListening(c -> {
        if (c.toString().trim().isBlank()) return;
        JsonObject req = Json.parseObject(c.toString()).getObj("payload");
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
    });
}

void test() {
    Json.parse("{\"foo\": true}");
}