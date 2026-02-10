import javax.swing.SwingUtilities;

static String test = "wss://echo.websocket.org";
static String url = "wss://ws-live-data.polymarket.com";


void main() {
//    gatherTestData();
//    testTime();
    trackLiveTrades();
}

void trackLiveTrades() {
    System.out.println("Starting live trade tracking with UI...");
    MessageHandler messageHandler = new MessageHandler();
    
    ScraperUI ui = new ScraperUI(messageHandler);
    messageHandler.setUI(ui);
    
    SwingUtilities.invokeLater(() -> ui.setVisible(true));

    // Start listening in a separate thread
    new Thread(() -> {
        new MessageListener(url).startListening(messageHandler::handle);
    }).start();
}

void gatherTestData() {
    new MessageListener(url).startListening(c -> {
        if (c.isEmpty()) return;
        try {
            Files.writeString(MockMessageListener.testData, c, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            Files.writeString(MockMessageListener.testData, "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    });
}

void testTime() {

    List<String> lines;
    try {
        lines = Files.readAllLines(MockMessageListener.testData);
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
    MessageHandler messageHandler = new MessageHandler();
    new MockMessageListener(url).startListening(messageHandler::handle);

    messageHandler.printTransactions();
}

void test() {
    Json.parse("{\"foo\": true}");
}