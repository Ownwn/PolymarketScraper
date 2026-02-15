import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Http {

    public static JsonObject getJsonObject(String url) {
        return getJsonObject(url, null);
    }

    public static JsonObject getJsonObject(String url, Map<String, String> headers) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URI(url).toURL().openConnection();
            connection.setRequestMethod("GET");
            if (headers != null) {
                for (var entry : headers.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            return Json.parseObject(new String(connection.getInputStream().readAllBytes()));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonObject post(String url, String jsonBody, Map<String, String> headers) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URI(url).toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            if (headers != null) {
                for (var entry : headers.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int code = connection.getResponseCode();
            if (code >= 200 && code < 300) {
                return Json.parseObject(new String(connection.getInputStream().readAllBytes()));
            } else {
                String error = new String(connection.getErrorStream().readAllBytes());
                throw new RuntimeException("HTTP POST failed with code " + code + ": " + error);
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
