import javax.swing.SwingUtilities;

public class QuizApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginWindow w = new LoginWindow();
            w.setVisible(true);
        });
    }
}

