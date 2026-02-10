import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;

public class JLink extends JLabel {
    private String link;

    public JLink(String text, String link) {
        setText(text);
        this.link = link;
        this.setFont(new Font("Arial", Font.BOLD, 24));
        this.setForeground(Color.blue);
    }

    @Override
    public void setText(String text) {
        String newText = "<html><u>" + text + "</u></html>";
        super.setText(newText);
    }

    public void setLink(String link) {
        this.link = link;
    }

    public MouseListener getMouseListener() {
        return new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Runtime.getRuntime().exec(new String[] {"firefox", link});
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        };
    }





}
