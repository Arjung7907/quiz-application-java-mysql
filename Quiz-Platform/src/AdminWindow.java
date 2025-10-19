import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * AdminWindow - full CRUD panel for categories and questions (4 options each).
 * Requires DB tables: categories, questions, options, and a valid DBConnection.
 */
public class AdminWindow extends JFrame {
    private JTable categoryTable, questionTable;
    private DefaultTableModel categoryModel, questionModel;
    private JButton addCatBtn, renameCatBtn, deleteCatBtn;
    private JButton addQBtn, editQBtn, deleteQBtn;
    private JTextArea statusArea;

    // --- Data Models ---

    // Class to hold the details of a single option
    public static class OptionDetails {
        private String text;
        private boolean isCorrect;

        public OptionDetails(String text, boolean isCorrect) {
            this.text = text;
            this.isCorrect = isCorrect;
        }

        public String getText() {
            return text;
        }

        public boolean isCorrect() {
            return isCorrect;
        }
    }

    // Class to hold the details of a single question and its options
    public static class QuestionDetails {
        private Integer id; // Null if adding a new question
        private String questionText;
        private List<OptionDetails> options = new ArrayList<>();

        public QuestionDetails(Integer id, String questionText) {
            this.id = id;
            this.questionText = questionText;
        }

        public Integer getId() {
            return id;
        }

        public String getQuestionText() {
            return questionText;
        }

        public List<OptionDetails> getOptions() {
            return options;
        }

        public void addOption(String text, boolean isCorrect) {
            options.add(new OptionDetails(text, isCorrect));
        }
    }

    // --- Constructor and UI Initialization (No changes here) ---

    public AdminWindow() {
        setTitle("Admin - Manage Quiz");
        setSize(1000, 640);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        initUI();
        loadCategories();
    }

    private void initUI() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(350);
        getContentPane().add(splitPane, BorderLayout.CENTER);

        // Left panel - categories
        JPanel leftPanel = new JPanel(new BorderLayout(6, 6));
        categoryModel = new DefaultTableModel(new String[] { "ID", "Category Name" }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        categoryTable = new JTable(categoryModel);
        categoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        leftPanel.add(new JScrollPane(categoryTable), BorderLayout.CENTER);

        JPanel catBtnPanel = new JPanel(new GridLayout(3, 1, 6, 6));
        addCatBtn = new JButton("Add Category");
        renameCatBtn = new JButton("Rename Category");
        deleteCatBtn = new JButton("Delete Category");
        catBtnPanel.add(addCatBtn);
        catBtnPanel.add(renameCatBtn);
        catBtnPanel.add(deleteCatBtn);
        leftPanel.add(catBtnPanel, BorderLayout.SOUTH);

        splitPane.setLeftComponent(leftPanel);

        // Right panel - questions
        JPanel rightPanel = new JPanel(new BorderLayout(6, 6));
        questionModel = new DefaultTableModel(new String[] { "ID", "Question" }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        questionTable = new JTable(questionModel);
        questionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rightPanel.add(new JScrollPane(questionTable), BorderLayout.CENTER);

        JPanel qBtnPanel = new JPanel(new GridLayout(3, 1, 6, 6));
        addQBtn = new JButton("Add Question");
        editQBtn = new JButton("Edit Question");
        deleteQBtn = new JButton("Delete Question");
        qBtnPanel.add(addQBtn);
        qBtnPanel.add(editQBtn);
        qBtnPanel.add(deleteQBtn);
        rightPanel.add(qBtnPanel, BorderLayout.SOUTH);

        splitPane.setRightComponent(rightPanel);

        // Status log at bottom
        statusArea = new JTextArea(5, 80);
        statusArea.setEditable(false);
        getContentPane().add(new JScrollPane(statusArea), BorderLayout.SOUTH);

        // Listeners
        categoryTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                loadQuestionsForSelectedCategory();
        });

        addCatBtn.addActionListener(e -> addCategory());
        renameCatBtn.addActionListener(e -> renameCategory());
        deleteCatBtn.addActionListener(e -> deleteCategory());

        addQBtn.addActionListener(e -> openQuestionDialog(null));
        editQBtn.addActionListener(e -> {
            int row = questionTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Select a question to edit");
                return;
            }
            Integer qid = (Integer) questionModel.getValueAt(row, 0);
            openQuestionDialog(qid);
        });
        deleteQBtn.addActionListener(e -> deleteQuestion());
    }

    // --- Database Loading Methods ---

    // Load categories into table
    private void loadCategories() {
        categoryModel.setRowCount(0);
        try (Connection c = DBConnection.getConnection();
                Statement s = c.createStatement();
                ResultSet rs = s.executeQuery("SELECT id, name FROM categories ORDER BY id")) {
            while (rs.next()) {
                categoryModel.addRow(new Object[] { rs.getInt("id"), rs.getString("name") });
            }
        } catch (SQLException ex) {
            status("Error loading categories: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // Load questions for selected category (Now includes ORDER BY id)
    private void loadQuestionsForSelectedCategory() {
        questionModel.setRowCount(0);
        int row = categoryTable.getSelectedRow();
        if (row < 0)
            return;
        int catId = (Integer) categoryModel.getValueAt(row, 0);

        // FIX: Added ORDER BY id to ensure sequential question listing
        String sql = "SELECT id, text FROM questions WHERE category_id = ? ORDER BY id ASC";

        try (Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, catId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    questionModel.addRow(new Object[] { rs.getInt("id"), rs.getString("text") });
                }
            }
        } catch (SQLException ex) {
            status("Error loading questions: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // --- New Data Loading Helper Method ---

    private QuestionDetails getQuestionDetails(int qid) throws SQLException {
        QuestionDetails details = null;
        try (Connection c = DBConnection.getConnection()) {

            // 1. Get question text
            PreparedStatement psQ = c.prepareStatement("SELECT text FROM questions WHERE id = ?");
            psQ.setInt(1, qid);
            try (ResultSet rsQ = psQ.executeQuery()) {
                if (rsQ.next()) {
                    details = new QuestionDetails(qid, rsQ.getString("text"));
                } else {
                    throw new SQLException("Question not found for ID: " + qid);
                }
            }

            // 2. Get options
            PreparedStatement psO = c.prepareStatement(
                    "SELECT text, is_correct FROM options WHERE question_id = ? ORDER BY id LIMIT 4");
            psO.setInt(1, qid);
            try (ResultSet rsO = psO.executeQuery()) {
                while (rsO.next()) {
                    details.addOption(rsO.getString("text"), rsO.getInt("is_correct") == 1);
                }
            }
        }
        return details;
    }

    // --- Category CRUD (No changes) ---
    // ... (addCategory, renameCategory, deleteCategory methods remain here) ...
    private void addCategory() {
        String name = JOptionPane.showInputDialog(this, "Enter new category name:");
        if (name == null || name.trim().isEmpty())
            return;
        try (Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement("INSERT INTO categories(name) VALUES(?)")) {
            ps.setString(1, name.trim());
            ps.executeUpdate();
            status("Category added: " + name);
            loadCategories();
        } catch (SQLException ex) {
            status("Error adding category: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void renameCategory() {
        int row = categoryTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a category to rename");
            return;
        }
        int id = (Integer) categoryModel.getValueAt(row, 0);
        String oldName = (String) categoryModel.getValueAt(row, 1);
        String newName = JOptionPane.showInputDialog(this, "Enter new name:", oldName);
        if (newName == null || newName.trim().isEmpty())
            return;
        try (Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement("UPDATE categories SET name = ? WHERE id = ?")) {
            ps.setString(1, newName.trim());
            ps.setInt(2, id);
            ps.executeUpdate();
            status("Category renamed: " + oldName + " -> " + newName);
            loadCategories();
        } catch (SQLException ex) {
            status("Error renaming category: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void deleteCategory() {
        int row = categoryTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a category to delete");
            return;
        }
        int id = (Integer) categoryModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Delete category and all its questions?", "Confirm",
                JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION)
            return;
        try (Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement("DELETE FROM categories WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
            status("Category deleted");
            loadCategories();
            questionModel.setRowCount(0);
        } catch (SQLException ex) {
            status("Error deleting category: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // --- Question CRUD ---

    private void deleteQuestion() {
        int row = questionTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a question to delete");
            return;
        }
        int qid = (Integer) questionModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Delete question and its options?", "Confirm",
                JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION)
            return;
        try (Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement("DELETE FROM questions WHERE id = ?")) {
            ps.setInt(1, qid);
            ps.executeUpdate();
            status("Question deleted");
            loadQuestionsForSelectedCategory();
        } catch (SQLException ex) {
            status("Error deleting question: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Add / Edit question dialog. If qid == null -> add, else edit
     * FIX: Uses QuestionDetails to load/store data.
     */
    private void openQuestionDialog(Integer qid) {
        int catRow = categoryTable.getSelectedRow();
        if (catRow < 0) {
            JOptionPane.showMessageDialog(this, "Select a category first");
            return;
        }
        int catId = (Integer) categoryModel.getValueAt(catRow, 0);

        JDialog dlg = new JDialog(this, (qid == null ? "Add Question" : "Edit Question"), true);
        dlg.setSize(640, 420);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(8, 8));

        JTextArea qText = new JTextArea(4, 40);
        qText.setLineWrap(true);
        qText.setWrapStyleWord(true);

        JPanel optPanel = new JPanel(new GridLayout(4, 1, 6, 6));
        JTextField[] opts = new JTextField[4];
        JRadioButton[] correctBtns = new JRadioButton[4];
        ButtonGroup bg = new ButtonGroup();
        for (int i = 0; i < 4; i++) {
            JPanel row = new JPanel(new BorderLayout(6, 6));
            opts[i] = new JTextField();
            correctBtns[i] = new JRadioButton("Correct");
            bg.add(correctBtns[i]);
            row.add(opts[i], BorderLayout.CENTER);
            row.add(correctBtns[i], BorderLayout.EAST);
            optPanel.add(row);
        }

        // --- LOAD DATA FOR EDIT MODE (Updated Logic) ---
        if (qid != null) {
            try {
                QuestionDetails details = getQuestionDetails(qid);
                qText.setText(details.getQuestionText());

                int i = 0;
                for (OptionDetails opt : details.getOptions()) {
                    if (i < 4) {
                        opts[i].setText(opt.getText());
                        correctBtns[i].setSelected(opt.isCorrect());
                    }
                    i++;
                }
            } catch (SQLException ex) {
                status("Error loading question for edit: " + ex.getMessage());
                ex.printStackTrace();
                dlg.dispose(); // Close dialog on failure
                return;
            }
        }

        JPanel center = new JPanel(new BorderLayout(6, 6));
        center.add(new JLabel("Question:"), BorderLayout.NORTH);
        center.add(new JScrollPane(qText), BorderLayout.CENTER);
        center.add(optPanel, BorderLayout.SOUTH);

        JButton saveBtn = new JButton("Save");
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(saveBtn);

        dlg.add(center, BorderLayout.CENTER);
        dlg.add(south, BorderLayout.SOUTH);

        saveBtn.addActionListener(e -> {
            String qStr = qText.getText().trim();
            if (qStr.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Enter question text");
                return;
            }
            String[] optStr = new String[4];
            int correctIdx = -1;
            for (int i = 0; i < 4; i++) {
                optStr[i] = opts[i].getText().trim();
                if (optStr[i].isEmpty()) {
                    JOptionPane.showMessageDialog(dlg, "Fill all option fields");
                    return;
                }
                if (correctBtns[i].isSelected())
                    correctIdx = i;
            }
            if (correctIdx == -1) {
                JOptionPane.showMessageDialog(dlg, "Select correct option");
                return;
            }

            Connection conn = null;
            try {
                conn = DBConnection.getConnection();
                conn.setAutoCommit(false);
                int qIdToUse;
                if (qid == null) {
                    // --- INSERT (ADD QUESTION) ---
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO questions(category_id, text) VALUES(?, ?)", Statement.RETURN_GENERATED_KEYS);
                    ps.setInt(1, catId);
                    ps.setString(2, qStr);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next())
                            qIdToUse = keys.getInt(1);
                        else
                            throw new SQLException("Failed to retrieve question id");
                    }
                } else {
                    // --- UPDATE (EDIT QUESTION) ---
                    qIdToUse = qid;
                    // 1. Update question text
                    PreparedStatement ps = conn.prepareStatement("UPDATE questions SET text = ? WHERE id = ?");
                    ps.setString(1, qStr);
                    ps.setInt(2, qid);
                    ps.executeUpdate();

                    // 2. Delete old options (since we don't know which options map to which option
                    // ID)
                    PreparedStatement psDel = conn.prepareStatement("DELETE FROM options WHERE question_id = ?");
                    psDel.setInt(1, qid);
                    psDel.executeUpdate();
                }

                // 3. Insert 4 new options (for both ADD and EDIT)
                PreparedStatement psOpt = conn
                        .prepareStatement("INSERT INTO options(question_id, text, is_correct) VALUES(?, ?, ?)");
                for (int i = 0; i < 4; i++) {
                    psOpt.setInt(1, qIdToUse);
                    psOpt.setString(2, optStr[i]);
                    psOpt.setInt(3, (i == correctIdx ? 1 : 0));
                    psOpt.addBatch();
                }
                psOpt.executeBatch();
                conn.commit();
                status(qid == null ? "Question added" : "Question updated");
                loadQuestionsForSelectedCategory();
                dlg.dispose();
            } catch (SQLException ex) {
                try {
                    if (conn != null)
                        conn.rollback();
                } catch (SQLException ignore) {
                }
                status("Error saving question: " + ex.getMessage());
                ex.printStackTrace();
            } finally {
                try {
                    if (conn != null) {
                        conn.setAutoCommit(true);
                        conn.close();
                    }
                } catch (SQLException ignore) {
                }
            }
        });

        dlg.setVisible(true);
    }

    private void status(String msg) {
        statusArea.append(msg + "\n");
        statusArea.setCaretPosition(statusArea.getDocument().getLength());
    }

    // optional standalone test
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminWindow().setVisible(true));
    }
}