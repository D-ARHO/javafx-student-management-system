package net.darho;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import java.sql.*;
import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Pattern;

public class Home {

    private final App app;
    private TableView<Student> table;
    private TextField nameField, emailField, searchField, marksField;
    private ComboBox<String> courseCombo;
    private Label statusLabel, countLabel, welcomeLabel;
    private ObservableList<Student> studentList = FXCollections.observableArrayList();
    private Student selectedStudent = null;
    private Button addBtn;

    // Button Style Constant (Steel Blue)
    private final String BLUE_BUTTON_STYLE = "-fx-background-color: #4682B4; -fx-text-fill: steel blue; -fx-font-weight: bold; -fx-cursor: hand;";

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
                    rs.getString("full_name"), rs.getString("email"),
                    rs.getString("course"), rs.getInt("marks")
                ));
            }
            if (countLabel != null) countLabel.setText("Total Students: " + studentList.size());
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load students: " + e.getMessage());
        }
    }

    public Parent getView() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("main-background");

        VBox topSection = new VBox();
        welcomeLabel = new Label("Welcome, " + Login.currentUserName + "!");
        welcomeLabel.setStyle("-fx-text-fill: #4682B4; -fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 10 20 0 20;");
        topSection.getChildren().addAll(createMenuBar(), welcomeLabel);
        root.setTop(topSection);

        HBox mainLayout = new HBox(20);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setAlignment(Pos.TOP_CENTER);

        VBox inputSection = createInputSection();
        VBox displaySection = createDisplaySection();

        inputSection.setMinWidth(350);
        HBox.setHgrow(displaySection, Priority.ALWAYS);

        mainLayout.getChildren().addAll(inputSection, displaySection);
        root.setCenter(mainLayout);
        root.setBottom(createStatusBar());

        return root;
    }

    private VBox createInputSection() {
        VBox inputSection = new VBox(15);
        inputSection.setPadding(new Insets(25));
        inputSection.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 15;");

        Label titleLabel = new Label("Student Details");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #4682B4;");

        nameField = new TextField(); nameField.setPromptText("Enter full name");
        emailField = new TextField(); emailField.setPromptText("Enter email");
        
        courseCombo = new ComboBox<>(FXCollections.observableArrayList(
            "Computer Science (CSE)", "Information Technology (IT)", "Business IT (BIT)",
            "Software Engineering", "Data Science", "Cybersecurity"
        ));
        courseCombo.setPromptText("Select Course");
        courseCombo.setMaxWidth(Double.MAX_VALUE);

        marksField = new TextField();
        marksField.setPromptText("Marks (0 - 100)");
        marksField.textProperty().addListener((obs, old, newValue) -> {
            if (!newValue.matches("\\d*")) marksField.setText(newValue.replaceAll("[^\\d]", ""));
            if (!marksField.getText().isEmpty() && Integer.parseInt(marksField.getText()) > 100) marksField.setText("100");
        });

        addBtn = new Button("➕ ADD STUDENT");
        addBtn.setStyle(BLUE_BUTTON_STYLE);
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setDisable(true); 
        addBtn.setOnAction(e -> addStudent());

        Runnable validate = () -> {
            boolean nameValid = nameField.getText().trim().length() >= 2;
            boolean emailValid = validateEmail(emailField.getText().trim());
            boolean courseValid = courseCombo.getValue() != null;
            boolean marksValid = !marksField.getText().isEmpty();
            addBtn.setDisable(!(nameValid && emailValid && courseValid && marksValid));
        };

        nameField.textProperty().addListener((o, old, n) -> validate.run());
        emailField.textProperty().addListener((o, old, n) -> validate.run());
        marksField.textProperty().addListener((o, old, n) -> validate.run());
        courseCombo.valueProperty().addListener((o, old, n) -> validate.run());

        Button updateBtn = new Button("✏️ UPDATE");
        updateBtn.setStyle(BLUE_BUTTON_STYLE);
        Button deleteBtn = new Button("🗑️ DELETE");
        deleteBtn.setStyle(BLUE_BUTTON_STYLE);
        
        HBox actionButtons = new HBox(10, updateBtn, deleteBtn);
        updateBtn.setMaxWidth(Double.MAX_VALUE); deleteBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(updateBtn, Priority.ALWAYS); HBox.setHgrow(deleteBtn, Priority.ALWAYS);

        updateBtn.setOnAction(e -> updateStudent());
        deleteBtn.setOnAction(e -> deleteStudent());

        inputSection.getChildren().addAll(
            titleLabel, new Separator(),
            new Label("Full Name:"), nameField,
            new Label("Email:"), emailField,
            new Label("Course:"), courseCombo,
            new Label("Marks:"), marksField,
            new Separator(), addBtn, actionButtons
        );

        return inputSection;
    }

    private VBox createDisplaySection() {
        VBox displaySection = new VBox(15);
        displaySection.setPadding(new Insets(25));
        displaySection.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 15;");

        searchField = new TextField();
        searchField.setPromptText("🔍 Search students...");
        Button showAllBtn = new Button("Show All");
        showAllBtn.setOnAction(e -> { searchField.clear(); loadStudentsFromDatabase(); });
        
        HBox searchBar = new HBox(10, searchField, showAllBtn);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.textProperty().addListener((obs, old, newVal) -> filterTable());

        CheckBox highAchievers = new CheckBox("High Achievers (>80)");
        highAchievers.setStyle("-fx-text-fill: steel blue;");
        highAchievers.setOnAction(e -> {
            if(highAchievers.isSelected()) table.setItems(studentList.filtered(s -> s.getMarks() > 80));
            else table.setItems(studentList);
        });

        ToggleGroup sortGroup = new ToggleGroup();
        RadioButton sortName = new RadioButton("Sort Name");
        RadioButton sortMarks = new RadioButton("Sort Marks");
        sortName.setToggleGroup(sortGroup); sortMarks.setToggleGroup(sortGroup);
        sortName.setStyle("-fx-text-fill: steel blue;"); sortMarks.setStyle("-fx-text-fill: steel blue;");

        sortName.setOnAction(e -> studentList.sort(Comparator.comparing(Student::getName)));
        sortMarks.setOnAction(e -> studentList.sort(Comparator.comparingInt(Student::getMarks).reversed()));

        HBox filterBox = new HBox(15, highAchievers, sortName, sortMarks);
        
        table = createTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        displaySection.getChildren().addAll(new Label("Student Records"), searchBar, filterBox, table);
        return displaySection;
    }

    @SuppressWarnings("unchecked")
    private TableView<Student> createTable() {
        TableView<Student> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Student, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<Student, String> colName = new TableColumn<>("Name");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<Student, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        TableColumn<Student, String> colCourse = new TableColumn<>("Course");
        colCourse.setCellValueFactory(new PropertyValueFactory<>("course"));
        TableColumn<Student, Integer> colMarks = new TableColumn<>("Marks");
        colMarks.setCellValueFactory(new PropertyValueFactory<>("marks"));
        TableColumn<Student, String> colGrade = new TableColumn<>("Grade");
        colGrade.setCellValueFactory(cell -> javafx.beans.binding.Bindings.createStringBinding(() -> calculateGrade(cell.getValue().getMarks())));

        table.getColumns().addAll(colId, colName, colEmail, colCourse, colMarks, colGrade);
        table.setItems(studentList);

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                selectedStudent = selected;
                nameField.setText(selected.getName());
                emailField.setText(selected.getEmail());
                courseCombo.setValue(selected.getCourse());
                marksField.setText(String.valueOf(selected.getMarks()));
            }
        });
        return table;
    }

    private String calculateGrade(int marks) {
        if (marks >= 90) return "A+";
        else if (marks >= 70) return "B";
        else if (marks >= 50) return "C";
        else return "F";
    }

    private String capitalizeName(String name) {
        if (name == null || name.isEmpty()) return "";
        String[] words = name.trim().split("\\s+");
        StringBuilder res = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) res.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase()).append(" ");
        }
        return res.toString().trim();
    }

    private boolean validateEmail(String email) {
        return Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$").matcher(email).matches();
    }

    private void addStudent() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO students (user_id, full_name, email, course, marks) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, Login.currentUserId);
            pstmt.setString(2, capitalizeName(nameField.getText()));
            pstmt.setString(3, emailField.getText().toLowerCase());
            pstmt.setString(4, courseCombo.getValue());
            pstmt.setInt(5, Integer.parseInt(marksField.getText()));
            pstmt.executeUpdate();
            
            loadStudentsFromDatabase();
            clearFields();
            showAlert("Success", "🎉 Student added successfully!");
        } catch (SQLException e) { showAlert("Database Error", e.getMessage()); }
    }

    private void updateStudent() {
        if (selectedStudent == null) {
            showAlert("No Selection", "Please select a student from the table first.");
            return;
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE students SET full_name=?, email=?, course=?, marks=? WHERE id=?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, capitalizeName(nameField.getText()));
            pstmt.setString(2, emailField.getText());
            pstmt.setString(3, courseCombo.getValue());
            pstmt.setInt(4, Integer.parseInt(marksField.getText()));
            pstmt.setInt(5, selectedStudent.getId());
            pstmt.executeUpdate();
            
            loadStudentsFromDatabase();
            clearFields();
            showAlert("Updated", "✏️ Student information updated!");
        } catch (SQLException e) { showAlert("Error", e.getMessage()); }
    }

    private void deleteStudent() {
        if (selectedStudent == null) {
            showAlert("No Selection", "Please select a student from the table first.");
            return;
        }

        // --- NEW CONFIRMATION DIALOG ---
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Student: " + selectedStudent.getName());
        confirm.setContentText("Are you sure you want to delete this record? This action cannot be undone.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "DELETE FROM students WHERE id=?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, selectedStudent.getId());
                pstmt.executeUpdate();
                
                loadStudentsFromDatabase();
                clearFields();
                showAlert("Deleted", "🗑️ Student has been removed.");
            } catch (SQLException e) { showAlert("Error", e.getMessage()); }
        }
    }

    private void filterTable() {
        String filter = searchField.getText().toLowerCase();
        FilteredList<Student> filteredData = new FilteredList<>(studentList, s -> 
            s.getName().toLowerCase().contains(filter) || s.getEmail().toLowerCase().contains(filter)
        );
        table.setItems(filteredData);
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");
        MenuItem logoutItem = new MenuItem("Logout");
        logoutItem.setOnAction(e -> app.showLogin());
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> System.exit(0));
        fileMenu.getItems().addAll(logoutItem, exitItem);

        Menu studentMenu = new Menu("Students");
        MenuItem addItem = new MenuItem("Add"); addItem.setOnAction(e -> addStudent());
        MenuItem updateItem = new MenuItem("Update"); updateItem.setOnAction(e -> updateStudent());
        MenuItem deleteItem = new MenuItem("Delete"); deleteItem.setOnAction(e -> deleteStudent());
        studentMenu.getItems().addAll(addItem, updateItem, deleteItem);

        Menu helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAlert("About", "Student Management System v1.0\nDeveloped by net.darho"));
        helpMenu.getItems().addAll(aboutItem);

        menuBar.getMenus().addAll(fileMenu, studentMenu, helpMenu);
        return menuBar;
    }

    private HBox createStatusBar() {
        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-text-fill: steel blue;");
        countLabel = new Label("Total Students: 0");
        countLabel.setStyle("-fx-text-fill: steel blue;");
        HBox statusBar = new HBox(20, statusLabel, new Pane(), countLabel);
        HBox.setHgrow(statusBar.getChildren().get(1), Priority.ALWAYS);
        statusBar.setPadding(new Insets(5, 20, 5, 20));
        statusBar.setStyle("-fx-background-color: #2c3e50;");
        return statusBar;
    }

    private void clearFields() {
        nameField.clear(); emailField.clear(); marksField.clear();
        courseCombo.setValue(null);
        selectedStudent = null;
        addBtn.setDisable(true);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait(); // showAndWait ensures the user sees it before proceeding
    }

    public static class Student {
        private int id, marks;
        private String name, email, course;
        public Student(int id, String name, String email, String course, int marks) {
            this.id = id; this.name = name; this.email = email; this.course = course; this.marks = marks;
        }
        public int getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getCourse() { return course; }
        public int getMarks() { return marks; }
    }
}