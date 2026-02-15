import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DataLogger {
    private static final String LOG_DIR = "logs";
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter LOG_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public DataLogger() {
        try {
            Files.createDirectories(Paths.get(LOG_DIR));
        } catch (IOException e) {
            System.err.println("Could not create log directory: " + e.getMessage());
        }
    }

    public synchronized void logRaw(String rawJson) {
        String fileName = LOG_DIR + "/raw_" + LocalDateTime.now().format(FILE_DATE_FORMAT) + ".jsonl";
        appendToFile(fileName, rawJson);
    }

    public synchronized void logTransaction(MessageHandler.Transaction t) {
        String fileName = LOG_DIR + "/trades_" + LocalDateTime.now().format(FILE_DATE_FORMAT) + ".log";
        String entry = String.format("[%s] %s | %s | %s | $%.2f | %s",
                LocalDateTime.now().format(LOG_TIME_FORMAT),
                t.side(),
                t.user(),
                t.title(),
                t.value(),
                t.slug());
        appendToFile(fileName, entry);
    }

    private void appendToFile(String fileName, String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
            writer.write(content);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        }
    }

    public static List<String> readRawLog(String date) throws IOException {
        Path path = Paths.get(LOG_DIR, "raw_" + date + ".jsonl");
        if (Files.exists(path)) {
            return Files.readAllLines(path);
        }
        return List.of();
    }
}
