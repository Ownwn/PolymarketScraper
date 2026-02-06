import com.sun.tools.javac.Main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public class MockMessageListener {
    public MockMessageListener(String s) {}

    public static Path testData = Path.of("testData.text");

    public void startListening(Consumer<CharSequence> listener) {
        List<String> lines;
        try {
            lines = Files.readAllLines(testData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (String line : lines) {
            listener.accept(line);
        }

    }
}
