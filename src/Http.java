import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;

public class Http {

    public static JsonObject getJsonObject(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URI(url).toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);
            return Json.parseObject(new String(connection.getInputStream().readAllBytes()));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }

    }
}
