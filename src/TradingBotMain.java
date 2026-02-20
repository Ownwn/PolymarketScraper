public class TradingBotMain {
    public static void main(String[] args) {
        TradingBot bot = new TradingBot();

        String exampleTokenId = "0x foo bar";
        

        JsonObject response = bot.placeOrder(exampleTokenId, "BUY", 0.10, 10);

        if (response != null) {
            System.out.println("Successfully placed order. Server response:");
            System.out.println(response);
        } else {
            System.err.println("Failed to place order.");
        }
    }
}
