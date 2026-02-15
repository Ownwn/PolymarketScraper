import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class JLink extends JLabel {
    private String link;

    public JLink(String text, String link) {
        setText(text);
        this.link = link;
        this.setFont(new Font("Arial", Font.BOLD, 24));
        this.setForeground(Color.blue);
        this.setCursor(new Cursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                open();
            }
        });
    }

    public static void openWebpage(String url) {
        if (url == null || url.isBlank()) return;
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                Runtime runtime = Runtime.getRuntime();
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    runtime.exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
                } else if (os.contains("mac")) {
                    runtime.exec(new String[]{"open", url});
                } else { // Linux/Unix
                    String[] browsers = {"xdg-open", "gio", "gnome-open", "kfmclient", "firefox", "chrome", "google-chrome"};
                    boolean started = false;
                    for (String browser : browsers) {
                        try {
                            runtime.exec(new String[]{browser, url});
                            started = true;
                            break;
                        } catch (Exception ignored) {}
                    }
                    if (!started) throw new RuntimeException("No browser found");
                }
            }
        } catch (IOException | URISyntaxException ex) {
            ex.printStackTrace();
        }
    }

    private void open() {
        openWebpage(link);
    }

    @Override
    public void setText(String text) {
        String newText = "<html><u>" + text + "</u></html>";
        super.setText(newText);
    }

    public void setLink(String link) {
        this.link = link;
    }

    // Kept for backward compatibility if used elsewhere, but marked as deprecated/unnecessary
    public java.awt.event.MouseListener getMouseListener() {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                open();
            }
        };
    }
}
