void main() {
    Json.parse("{\"connection_id\":\"YMbv8fRFrPECIYg=\",\"payload\":{\"body\":\"Easy money\",\"createdAt\":\"2026-02-03T07:40:38.502169Z\",\"id\":\"2364033\",\"parentEntityID\":151953,\"parentEntityType\":\"Event\",\"profile\":{\"baseAddress\":\"0x800b377d3755d024791ef901de7fee332e1907b8\",\"bio\":\"\",\"displayUsernamePublic\":true,\"name\":\"8udjak\",\"positions\":[{\"positionSize\":\"99994735\",\"tokenId\":\"22847776714049256442838704546276442358487931494046283003133543149743720105755\"}],\"proxyWallet\":\"0x0462c61c55da8bf7f78846a309ef18964e81da6d\",\"pseudonym\":\"Creamy-Ketch\"},\"reactionCount\":0,\"reportCount\":0,\"userAddress\":\"0x800b377d3755d024791ef901de7fee332e1907b8\"},\"timestamp\":1770104438509,\"topic\":\"comments\",\"type\":\"comment_created\"}\n");

    new MessageListener(url).startListening(c -> {
        try {
        System.out.println(Json.parse(c.toString().trim()));

        } catch (Exception e) {
            System.err.println(c.toString().trim());
        }
    });


}
static String test = "wss://echo.websocket.org";
static String url = "wss://ws-live-data.polymarket.com";