import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class CategoryWindow extends JFrame {
    private int userId;
    private String username;
    public CategoryWindow(int userId, String username) {
        this.userId = userId;
        this.username = username;
        setTitle("Select Category - " + username);
        setSize(400,300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        init();
    }

    private void init() {
        JPanel p = new JPanel(new BorderLayout(8,8));
        DefaultListModel<Category> model = new DefaultListModel<>();
        JList<Category> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        p.add(new JScrollPane(list), BorderLayout.CENTER);

        JButton start = new JButton("Start Quiz");
        p.add(start, BorderLayout.SOUTH);
        add(p);

        // load categories
        try (Connection c = DBConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT id, name FROM categories")) {
            while (rs.next()) model.addElement(new Category(rs.getInt("id"), rs.getString("name")));
        } catch (SQLException ex) { ex.printStackTrace(); }

        start.addActionListener(e -> {
            Category sel = list.getSelectedValue();
            if (sel == null) { JOptionPane.showMessageDialog(this,"Choose category"); return; }
            dispose();
            // open QuizWindow with userId and selected category
            new QuizWindow(userId, sel.id, sel.name).setVisible(true);
        });
    }

    private static class Category {
        int id; String name;
        Category(int id, String name) { this.id=id; this.name=name; }
        public String toString(){ return name; }
    }
}
