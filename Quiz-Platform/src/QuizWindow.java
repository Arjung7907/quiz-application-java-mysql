import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.*;

public class QuizWindow extends JFrame {
    private int userId, categoryId;
    private String categoryName;
    private java.util.List<Question> questions = new ArrayList<>();
    private int cursor = 0;
    private int score = 0;

    private JLabel qLabel;
    private JRadioButton[] optionButtons = new JRadioButton[4];
    private ButtonGroup group;

    // Make constructor accept userId, categoryId, categoryName
    public QuizWindow(int userId, int categoryId, String categoryName) {
        this.userId = userId; this.categoryId = categoryId; this.categoryName = categoryName;
        setTitle("Quiz - " + categoryName);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600,250);
        setLocationRelativeTo(null);
        loadQuestions();
        initUI();
        showQuestion(0);
    }

    private void loadQuestions() {
        String qSql = "SELECT id, text FROM questions WHERE category_id = ?";
        String oSql = "SELECT id, text, is_correct FROM options WHERE question_id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement qp = c.prepareStatement(qSql);
             PreparedStatement op = c.prepareStatement(oSql)) {
            qp.setInt(1, categoryId);
            try (ResultSet qrs = qp.executeQuery()) {
                while (qrs.next()) {
                    int qid = qrs.getInt("id");
                    String text = qrs.getString("text");
                    Question q = new Question(qid, text);
                    op.setInt(1, qid);
                    try (ResultSet ors = op.executeQuery()) {
                        while (ors.next()) {
                            q.options.add(new Option(ors.getInt("id"), ors.getString("text"), ors.getInt("is_correct")==1));
                        }
                    }
                    questions.add(q);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void initUI() {
        JPanel p = new JPanel(new BorderLayout(8,8));
        qLabel = new JLabel("", SwingConstants.LEFT);
        qLabel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        p.add(qLabel, BorderLayout.NORTH);

        JPanel opts = new JPanel(new GridLayout(4,1));
        group = new ButtonGroup();
        for (int i=0;i<4;i++){
            optionButtons[i] = new JRadioButton();
            group.add(optionButtons[i]);
            opts.add(optionButtons[i]);
        }
        p.add(opts, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        JButton prev = new JButton("Prev");
        JButton next = new JButton("Next");
        JButton submit = new JButton("Submit");
        bottom.add(prev); bottom.add(next); bottom.add(submit);
        p.add(bottom, BorderLayout.SOUTH);
        add(p);

        prev.addActionListener(e -> {
            if (cursor>0) { saveAnswer(); cursor--; showQuestion(cursor); }
        });
        next.addActionListener(e -> {
            if (cursor < questions.size()-1) { saveAnswer(); cursor++; showQuestion(cursor); }
        });
        submit.addActionListener(e -> {
            saveAnswer();
            computeScoreAndSave();
            dispose();
            new ResultWindow(userId, categoryId, score, questions.size()).setVisible(true);
        });
    }

   private void showQuestion(int idx) {
        if (questions.isEmpty()) { JOptionPane.showMessageDialog(this,"No questions"); return; }
        
        Question q = questions.get(idx);
        
        qLabel.setText("<html><b>Q" + (idx+1) + ":</b> " + q.text + "</html>");
        
        // --- FIX START: Explicitly clear the selection in the ButtonGroup ---
        group.clearSelection();
        // --- FIX END ---
        
        for (int i=0;i<4;i++){
            if (i < q.options.size()) optionButtons[i].setText(q.options.get(i).text);
            else optionButtons[i].setText("");
            
            // This line is technically correct but less effective than group.clearSelection()
            // optionButtons[i].setSelected(false);
        }
        
        // Check if the current question has a previously saved answer and restore it
        if (q.selectedIndex >= 0 && q.selectedIndex < 4) {
            optionButtons[q.selectedIndex].setSelected(true);
        }
    }

    private void saveAnswer() {
        Question q = questions.get(cursor);
        for (int i=0;i<4;i++){
            if (optionButtons[i].isSelected()) { q.selectedIndex = i; return; }
        }
        q.selectedIndex = -1;
    }

    private void computeScoreAndSave() {
        score = 0;
        for (Question q : questions) {
            if (q.selectedIndex >=0 && q.selectedIndex < q.options.size()) {
                if (q.options.get(q.selectedIndex).isCorrect) score++;
            }
        }
        String sql = "INSERT INTO results (user_id, category_id, score) VALUES (?, ?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, categoryId);
            ps.setInt(3, score);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private static class Question {
        int id; String text; java.util.List<Option> options = new ArrayList<>();
        int selectedIndex = -1;
        Question(int id, String text){ this.id=id; this.text=text; }
    }

    private static class Option {
        int id; String text; boolean isCorrect;
        Option(int id, String text, boolean isCorrect){ this.id=id; this.text=text; this.isCorrect=isCorrect;}
    }
}
