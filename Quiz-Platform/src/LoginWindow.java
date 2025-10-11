import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class LoginWindow extends JFrame {
    private static final String ADMIN_USERNAME = "admin";
    // Change this to whatever admin password you want.
    // For better security, store this in a secure config or database in real apps.
    private static final String ADMIN_PASSWORD = "admin@123";

    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginWindow() {
        setTitle("Quiz - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(350, 180);
        setLocationRelativeTo(null);
        init();
    }

    private void init() {
        JPanel p = new JPanel(new GridLayout(3, 2, 6, 6));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        p.add(new JLabel("Username:"));
        usernameField = new JTextField();
        p.add(usernameField);
        p.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        p.add(passwordField);

        JButton loginBtn = new JButton("Login");
        JButton registerBtn = new JButton("Register");
        p.add(loginBtn);
        p.add(registerBtn);
        add(p);

        loginBtn.addActionListener(e -> attemptLogin());
        registerBtn.addActionListener(e -> attemptRegister());
    }

    private void attemptLogin() {
        String user = usernameField.getText().trim();
        String pass = new String(passwordField.getPassword());
        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter both fields");
            return;
        }

        // 1) Admin shortcut: if user is "admin" and password matches ADMIN_PASSWORD,
        // open AdminWindow
        /*
         * if (ADMIN_USERNAME.equals(user) && ADMIN_PASSWORD.equals(pass)) {
         * dispose();
         * // Open admin interface
         * new AdminWindow().setVisible(true);
         * return;
         * }
         */

        // 2) Regular user login flow
        String sql = "SELECT id FROM users WHERE username = ? AND password = ?";
        try (
                Connection c = DBConnection.getConnection();
                java.sql.PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, user);
            ps.setString(2, pass);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int userId = rs.getInt("id");
                    dispose();
                    new CategoryWindow(userId, user).setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid credentials");
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "DB error: " + ex.getMessage());
        }
    }

    private void attemptRegister() {
        String user = usernameField.getText().trim();
        String pass = new String(passwordField.getPassword());
        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter both fields");
            return;
        }

        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (Connection c = DBConnection.getConnection();
                java.sql.PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, user);
            ps.setString(2, pass);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Registered. Now login.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Register failed: " + ex.getMessage());
        }
    }
}
