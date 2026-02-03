import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public record MessageListener(String wssUrl) {
    private static final String commentSubscription = "{\"action\":\"subscribe\",\"subscriptions\":[{\"topic\":\"comments\",\"type\":\"comment_created\"}]}";

    public void startListening(Consumer<CharSequence> listener) {
        URI uri = URI.create(wssUrl);

        CompletableFuture<Void> infiniteComplete = new CompletableFuture<>();

        HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(uri, new WebSocket.Listener() {
                    @Override public void onOpen(WebSocket webSocket) {
                        webSocket.request(1);
                        webSocket.sendText(commentSubscription, true);
                    }

                    @Override public CompletionStage<?> onText(WebSocket webSocket,
                                                               CharSequence data,
                                                               boolean last) {
                        listener.accept(data);
                        webSocket.request(1);
                        return null;
                    }

                    @Override public CompletionStage<?> onClose(WebSocket webSocket,
                                                                int statusCode,
                                                                String reason) {
//                        System.out.println("closed: " + statusCode + " " + reason);
                        return CompletableFuture.completedFuture(null);
                    }

                    @Override public void onError(WebSocket webSocket, Throwable error) {
                        error.printStackTrace();
                        infiniteComplete.completeExceptionally(error);
                    }
                }).join();

        infiniteComplete.join();
    }

    static void main() {
        String url = "wss://ws-live-data.polymarket.com";
        new MessageListener(url).startListening(c -> {
            try {
                System.out.println(Json.parse(c.toString().trim()));

            } catch (Exception e) {
                System.err.println(c.toString().trim());
            }
        });
    }

}


