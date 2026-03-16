package net.darho;

import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.util.Duration;
import java.sql.*;

public class Home {

    private final App app;
    private TableView<Student> table;
    private TextField nameField, emailField, searchField;
    private ComboBox<String> courseCombo;
    private Slider marksSlider;
    private Label marksLabel, statusLabel;
    private ObservableList<Student> studentList = FXCollections.observableArrayList();
    private Student selectedStudent = null;
    private Label countLabel;
    private Label welcomeLabel;

    public Home(App app) {
        this.app = app;
        loadStudentsFromDatabase();
    }

    private void loadStudentsFromDatabase() {
        studentList.clear();
        String sql = "SELECT * FROM students WHERE user_id = ? ORDER BY id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, Login.currentUserId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                studentList.add(new Student(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getString("full_name"),
                    rs.getString("email"),
                    rs.getString("course"),
                    rs.getInt("marks")
                ));
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load students: " + e.getMessage());
        }
    }

    public Parent getView() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("main-background");

        VBox topSection = new VBox();
        welcomeLabel = new Label("Welcome, " + Login.currentUserName + "!");
        welcomeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 10 20 0 20;");
        topSection.getChildren().addAll(welcomeLabel, createMenuBar());
        root.setTop(topSection);

        VBox mainContent = createManagementContent();
        ScrollPane scrollPane = new ScrollPane(mainContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        
        root.setCenter(scrollPane);
        root.setBottom(createStatusBar());

        return root;
    }

    private VBox createManagementContent() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(20));
        container.setAlignment(Pos.TOP_CENTER);

        HBox mainLayout = new HBox(20);
        mainLayout.setAlignment(Pos.TOP_CENTER);

        VBox inputSection = createInputSection();
        inputSection.setMinWidth(350);
        inputSection.setPrefWidth(350);
        
        VBox displaySection = createDisplaySection();
        HBox.setHgrow(displaySection, Priority.ALWAYS);

        mainLayout.getChildren().addAll(inputSection, displaySection);
        container.getChildren().add(mainLayout);
        VBox.setVgrow(mainLayout, Priority.ALWAYS);

        return container;
    }

    private VBox createInputSection() {
        VBox inputSection = new VBox(15);
        inputSection.setPadding(new Insets(25));
        inputSection.getStyleClass().add("glass-card");

        Label titleLabel = new Label("Student Details");
        titleLabel.getStyleClass().add("section-title");

        nameField = new TextField(); nameField.setPromptText("Enter full name");
        emailField = new TextField(); emailField.setPromptText("Enter email");
        
        courseCombo = new ComboBox<>(FXCollections.observableArrayList(
            "Computer Science (CSE)", "Information Technology (IT)", "Software Engineering", "Data Science", "Cybersecurity"
        ));
        courseCombo.setMaxWidth(Double.MAX_VALUE);

        marksSlider = new Slider(0, 100, 50);
        marksLabel = new Label("50");
        marksSlider.valueProperty().addListener((obs, old, newVal) -> marksLabel.setText(String.valueOf(newVal.intValue())));
        HBox marksBox = new HBox(10, marksSlider, marksLabel);

        Button addBtn = new Button("➕ ADD STUDENT");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.getStyleClass().add("primary-button");
        addBtn.setOnAction(e -> addStudent());

        Button updateBtn = new Button("✏️ UPDATE");
        updateBtn.getStyleClass().add("update-button");
        Button deleteBtn = new Button("🗑️ DELETE");
        deleteBtn.getStyleClass().add("delete-button");
        
        updateBtn.setMaxWidth(Double.MAX_VALUE);
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(updateBtn, Priority.ALWAYS);
        HBox.setHgrow(deleteBtn, Priority.ALWAYS);
        HBox actionButtons = new HBox(10, updateBtn, deleteBtn);
        
        updateBtn.setOnAction(e -> updateStudent());
        deleteBtn.setOnAction(e -> deleteStudent());

        inputSection.getChildren().addAll(
            titleLabel, new Separator(),
            new Label("Full Name:"), nameField,
            new Label("Email:"), emailField,
            new Label("Course:"), courseCombo,
            new Label("Marks:"), marksBox,
            new Separator(), addBtn, actionButtons
        );

        return inputSection;
    }

    private VBox createDisplaySection() {
        VBox displaySection = new VBox(15);
        displaySection.setPadding(new Insets(25));
        displaySection.getStyleClass().add("glass-card");

        searchField = new TextField();
        searchField.setPromptText("🔍 Search...");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        
        Button searchBtn = new Button("SEARCH");
        searchBtn.setOnAction(e -> filterTable());
        Button allBtn = new Button("SHOW ALL");
        allBtn.setOnAction(e -> { searchField.clear(); filterTable(); });

        HBox searchBox = new HBox(10, searchField, searchBtn, allBtn);

        table = createTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        displaySection.getChildren().addAll(searchBox, table);
        return displaySection;
    }

    @SuppressWarnings("unchecked")
    private TableView<Student> createTable() {
        TableView<Student> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Student, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setMaxWidth(50);

        TableColumn<Student, String> colName = new TableColumn<>("Name");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Student, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        TableColumn<Student, Integer> colMarks = new TableColumn<>("Marks");
        colMarks.setCellValueFactory(new PropertyValueFactory<>("marks"));

        table.getColumns().addAll(colId, colName, colEmail, colMarks);
        table.setItems(studentList);
        
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            selectedStudent = selected;
            if (selected != null) populateFields(selected);
        });

        return table;
    }

    private void addStudent() {
        if (validateInputs()) {
            String name = capitalizeName(nameField.getText());
            String email = emailField.getText().toLowerCase();
            String course = courseCombo.getValue();
            int marks = (int) marksSlider.getValue();

            Student newStudent = new Student(0, Login.currentUserId, name, email, course, marks);
            saveStudentToDatabase(newStudent);
            
            if (newStudent.getId() > 0) {
                studentList.add(newStudent);
                clearFields();
                updateStatus("Student added!");
                updateStatistics();
            }
        }
    }

    private void updateStudent() {
        if (selectedStudent != null && validateInputs()) {
            selectedStudent.setName(capitalizeName(nameField.getText()));
            selectedStudent.setEmail(emailField.getText().toLowerCase());
            selectedStudent.setCourse(courseCombo.getValue());
            selectedStudent.setMarks((int) marksSlider.getValue());
            
            updateStudentInDatabase(selectedStudent);
            table.refresh();
            clearFields();
            updateStatus("Student updated!");
        } else {
            showAlert("Selection Required", "Please select a student first.");
        }
    }

    private void deleteStudent() {
        if (selectedStudent != null) {
            deleteStudentFromDatabase(selectedStudent.getId());
            studentList.remove(selectedStudent);
            clearFields();
            updateStatus("Student deleted!");
            updateStatistics();
        }
    }

    private void saveStudentToDatabase(Student student) {
        String sql = "INSERT INTO students (user_id, full_name, email, course, marks) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, Login.currentUserId);
            pstmt.setString(2, student.getName());
            pstmt.setString(3, student.getEmail());
            pstmt.setString(4, student.getCourse());
            pstmt.setInt(5, student.getMarks());
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) student.setId(rs.getInt(1));
        } catch (SQLException e) { showAlert("Error", e.getMessage()); }
    }

    private void updateStudentInDatabase(Student student) {
        String sql = "UPDATE students SET full_name=?, email=?, course=?, marks=? WHERE id=? AND user_id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, student.getName());
            pstmt.setString(2, student.getEmail());
            pstmt.setString(3, student.getCourse());
            pstmt.setInt(4, student.getMarks());
            pstmt.setInt(5, student.getId());
            pstmt.setInt(6, Login.currentUserId);
            pstmt.executeUpdate();
        } catch (SQLException e) { showAlert("Error", e.getMessage()); }
    }

    private void deleteStudentFromDatabase(int id) {
        String sql = "DELETE FROM students WHERE id=? AND user_id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.setInt(2, Login.currentUserId);
            pstmt.executeUpdate();
        } catch (SQLException e) { showAlert("Error", e.getMessage()); }
    }

    private void filterTable() {
        FilteredList<Student> filteredData = new FilteredList<>(studentList, p -> true);
        filteredData.setPredicate(s -> {
            String search = searchField.getText().toLowerCase();
            return search.isEmpty() || s.getName().toLowerCase().contains(search) || s.getEmail().toLowerCase().contains(search);
        });
        table.setItems(filteredData);
    }

    private boolean validateInputs() {
        if (nameField.getText().isEmpty() || emailField.getText().isEmpty() || courseCombo.getValue() == null) {
            showAlert("Validation Error", "All fields are required!");
            return false;
        }
        return true;
    }

    private void populateFields(Student s) {
        nameField.setText(s.getName());
        emailField.setText(s.getEmail());
        courseCombo.setValue(s.getCourse());
        marksSlider.setValue(s.getMarks());
    }

    private void clearFields() {
        nameField.clear(); emailField.clear(); courseCombo.setValue(null); marksSlider.setValue(50); selectedStudent = null;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content); alert.showAndWait();
    }

    private void updateStatus(String msg) {
        statusLabel.setText(msg);
        PauseTransition p = new PauseTransition(Duration.seconds(3));
        p.setOnFinished(e -> statusLabel.setText("Ready"));
        p.play();
    }

    private void updateStatistics() { countLabel.setText("Total: " + studentList.size()); }

    private MenuBar createMenuBar() { 
        MenuBar mb = new MenuBar(); 
        Menu file = new Menu("File");
        MenuItem logout = new MenuItem("Logout");
        logout.setOnAction(e -> app.showLogin());
        file.getItems().add(logout);
        mb.getMenus().add(file);
        return mb; 
    }

    private HBox createStatusBar() {
        HBox sb = new HBox(10); sb.setPadding(new Insets(5));
        statusLabel = new Label("Ready");
        countLabel = new Label("Total: " + studentList.size());
        sb.getChildren().addAll(statusLabel, new Pane(), countLabel);
        HBox.setHgrow(sb.getChildren().get(1), Priority.ALWAYS);
        return sb;
    }

    private String capitalizeName(String name) {
        if (name == null || name.isEmpty()) return name;
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public static class Student {
        private int id, marks;
        private String name, email, course;
        public Student(int id, int userId, String name, String email, String course, int marks) {
            this.id = id; this.name = name; this.email = email; this.course = course; this.marks = marks;
        }
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getCourse() { return course; }
        public void setCourse(String course) { this.course = course; }
        public int getMarks() { return marks; }
        public void setMarks(int marks) { this.marks = marks; }
    }
}