import javax.swing.*;
import java.awt.*;

public class ResultWindow extends JFrame {
    public ResultWindow(int userId, int categoryId, int score, int total) {
        setTitle("Result");
        setSize(300, 180);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        init(score, total);
    }

    private void init(int score, int total) {
        JPanel p = new JPanel(new GridLayout(3, 1, 6, 6));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        p.add(new JLabel("Your Score: " + score + " / " + total, SwingConstants.CENTER));
        p.add(new JLabel("Percentage: " + (total == 0 ? 0 : (score * 100 / total)) + "%", SwingConstants.CENTER));
        JButton close = new JButton("Close");
        close.addActionListener(e -> System.exit(0));
        p.add(close);
        add(p);
    }
}
