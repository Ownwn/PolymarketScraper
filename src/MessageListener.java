import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public record MessageListener(String wssUrl) {
    private static final String tradeSubscription = "{\"action\":\"subscribe\",\"subscriptions\":[{\"topic\":\"activity\",\"type\":\"trades\"}]}";

    public void startListening(Consumer<CharSequence> listener) {
        URI uri = URI.create(wssUrl);

        CompletableFuture<Void> infiniteComplete = new CompletableFuture<>();

        HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(uri, new WebSocket.Listener() {
                    StringBuilder buffer = new StringBuilder();

                    @Override
                    public void onOpen(WebSocket webSocket) {
                        webSocket.request(1);
                        webSocket.sendText(tradeSubscription, true);
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket,
                                                     CharSequence data,
                                                     boolean last) {
                        buffer.append(data);
                        if (last) {
                            listener.accept(buffer.toString());
                            buffer.setLength(0);
                        }
                        webSocket.request(1);
                        return null;
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket,
                                                      int statusCode,
                                                      String reason) {
                        System.out.println("closed: " + statusCode + " " + reason);
                        return CompletableFuture.completedFuture(null);
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        error.printStackTrace();
                        infiniteComplete.completeExceptionally(error);
                    }
                }).join();

        infiniteComplete.join();
    }
}