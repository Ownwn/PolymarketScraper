// Note: This class depends on Http.java and Json.java from the 'src' directory.
// Ensure your build path includes the 'src' directory.

import java.util.HashMap;
import java.util.Map;

public class TradingBot {
    private static final String TS_SERVER_URL = "http://localhost:3000";

    public JsonObject placeOrder(String tokenId, String side, double price, double size) {
        String path = "/order";

        String body = "{" +
            "\"tokenID\":\"" + tokenId + "\"," +
            "\"side\":\"" + side.toUpperCase() + "\"," +
            "\"price\":" + price + "," +
            "\"size\":" + size +
            "}";

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        try {
            return Http.post(TS_SERVER_URL + path, body, headers);
        } catch (Exception e) {
            System.err.println("Error placing order: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
