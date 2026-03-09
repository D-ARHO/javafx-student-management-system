package net.darho;

import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.util.Duration;
import java.sql.*;
import java.util.Optional;
import java.util.regex.Pattern;

public class Home {

    private final App app;
    private TableView<Student> table;
    private TextField nameField, emailField, searchField;
    private ComboBox<String> courseCombo;
    private Slider marksSlider;
    private Label marksLabel, statusLabel;
    private CheckBox filterHighAchievers, filterLowAchievers;
    private ToggleGroup sortGroup;
    private RadioButton sortByName, sortByMarks;
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
        
        // Only load students for the current user
        String sql = "SELECT * FROM students WHERE user_id = ? ORDER BY id DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Login.currentUserId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Student student = new Student(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getString("full_name"),
                    rs.getString("email"),
                    rs.getString("course"),
                    rs.getInt("marks")
                );
                studentList.add(student);
            }
            
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load students: " + e.getMessage());
        }
    }

    public Parent getView() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("main-background");

        // --- Top Section with Welcome and Menu ---
        VBox topSection = new VBox();
        
        // Welcome label
        welcomeLabel = new Label("Welcome, " + Login.currentUserName + "!");
        welcomeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 10 20 0 20;");
        
        // Menu Bar
        MenuBar menuBar = createMenuBar();
        
        topSection.getChildren().addAll(welcomeLabel, menuBar);
        root.setTop(topSection);

        // --- Main Content - Only Student Management ---
        VBox mainContent = createManagementContent();
        root.setCenter(mainContent);

        // --- Status Bar ---
        root.setBottom(createStatusBar());

        return root;
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        menuBar.getStyleClass().add("glass-menubar");

        // File Menu with Logout
        Menu fileMenu = new Menu("File");
        
        MenuItem logoutItem = new MenuItem("Logout");
        logoutItem.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Logout");
            alert.setHeaderText("Are you sure you want to logout?");
            alert.setContentText("You will be returned to the login screen.");
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                Login.currentUserId = -1;
                Login.currentUserName = "";
                app.showLogin();
            }
        });
        
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> System.exit(0));
        
        fileMenu.getItems().addAll(logoutItem, new SeparatorMenuItem(), exitItem);

        // Help Menu
        Menu helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("About");
            alert.setHeaderText("Student Management System");
            alert.setContentText("Version 1.0\nDeveloped by Team\nAll Rights Reserved © 2024");
            alert.showAndWait();
        });
        helpMenu.getItems().add(aboutItem);

        menuBar.getMenus().addAll(fileMenu, helpMenu);
        
        return menuBar;
    }

    private VBox createManagementContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: transparent;");

        // Main split layout
        HBox mainLayout = new HBox(20);
        mainLayout.setAlignment(Pos.TOP_CENTER);
        HBox.setHgrow(mainLayout, Priority.ALWAYS);

        // Left Panel - Input Section
        VBox inputSection = createInputSection();
        inputSection.setPrefWidth(350);
        
        // Right Panel - Display Section
        VBox displaySection = createDisplaySection();
        HBox.setHgrow(displaySection, Priority.ALWAYS);
        displaySection.setPrefWidth(700);

        mainLayout.getChildren().addAll(inputSection, displaySection);
        content.getChildren().add(mainLayout);

        return content;
    }

    private VBox createInputSection() {
        VBox inputSection = new VBox(15);
        inputSection.setPadding(new Insets(25));
        inputSection.getStyleClass().add("glass-card");

        Label titleLabel = new Label("Student Details");
        titleLabel.getStyleClass().add("section-title");

        // Name field
        Label nameLabel = new Label("Full Name:");
        nameLabel.getStyleClass().add("form-label");
        nameField = new TextField();
        nameField.setPromptText("Enter student name");
        nameField.getStyleClass().add("text-field");

        // Email field
        Label emailLabel = new Label("Email Address:");
        emailLabel.getStyleClass().add("form-label");
        emailField = new TextField();
        emailField.setPromptText("Enter email address");
        emailField.getStyleClass().add("text-field");

        // Course ComboBox
        Label courseLabel = new Label("Course:");
        courseLabel.getStyleClass().add("form-label");
        courseCombo = new ComboBox<>();
        courseCombo.setItems(FXCollections.observableArrayList(
            "Computer Science (CSE)",
            "Information Technology (IT)",
            "Business IT (BIT)",
            "Software Engineering",
            "Data Science",
            "Cybersecurity"
        ));
        courseCombo.setPromptText("Select Course");
        courseCombo.setMaxWidth(Double.MAX_VALUE);
        courseCombo.getStyleClass().add("combo-box");

        // Marks Slider
        Label marksTitleLabel = new Label("Marks:");
        marksTitleLabel.getStyleClass().add("form-label");
        
        marksSlider = new Slider(0, 100, 50);
        marksSlider.setShowTickLabels(true);
        marksSlider.setShowTickMarks(true);
        marksSlider.setMajorTickUnit(20);
        marksSlider.setMinorTickCount(5);
        marksSlider.setBlockIncrement(5);
        marksSlider.getStyleClass().add("slider");
        
        marksLabel = new Label("50");
        marksLabel.getStyleClass().add("marks-label");
        marksSlider.valueProperty().addListener((obs, oldVal, newVal) -> 
            marksLabel.setText(String.valueOf(newVal.intValue()))
        );

        HBox marksBox = new HBox(10, marksSlider, marksLabel);
        marksBox.setAlignment(Pos.CENTER_LEFT);

        // Buttons - Add, Update, Delete only
        Button addBtn = new Button("➕ ADD STUDENT");
        addBtn.getStyleClass().add("primary-button");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setOnAction(e -> addStudent());

        HBox actionButtons = new HBox(10);
        actionButtons.setAlignment(Pos.CENTER);
        
        Button updateBtn = new Button("✏️ UPDATE");
        updateBtn.getStyleClass().add("update-button");
        updateBtn.setMaxWidth(Double.MAX_VALUE);
        updateBtn.setOnAction(e -> updateStudent());
        
        Button deleteBtn = new Button("🗑️ DELETE");
        deleteBtn.getStyleClass().add("delete-button");
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        deleteBtn.setOnAction(e -> deleteStudent());
        
        HBox.setHgrow(updateBtn, Priority.ALWAYS);
        HBox.setHgrow(deleteBtn, Priority.ALWAYS);
        actionButtons.getChildren().addAll(updateBtn, deleteBtn);

        inputSection.getChildren().addAll(
            titleLabel,
            new Separator(),
            nameLabel, nameField,
            emailLabel, emailField,
            courseLabel, courseCombo,
            marksTitleLabel, marksBox,
            new Separator(),
            addBtn,
            actionButtons
        );

        return inputSection;
    }

    private VBox createDisplaySection() {
        VBox displaySection = new VBox(15);
        displaySection.setPadding(new Insets(25));
        displaySection.getStyleClass().add("glass-card");

        // Search Bar
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        
        searchField = new TextField();
        searchField.setPromptText("🔍 Search by name, email, or course...");
        searchField.getStyleClass().add("text-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        
        Button searchBtn = new Button("SEARCH");
        searchBtn.getStyleClass().add("search-button");
        searchBtn.setOnAction(e -> filterTable());
        
        Button showAllBtn = new Button("SHOW ALL");
        showAllBtn.getStyleClass().add("show-all-button");
        showAllBtn.setOnAction(e -> {
            searchField.clear();
            filterHighAchievers.setSelected(false);
            filterLowAchievers.setSelected(false);
            filterTable();
        });

        searchBox.getChildren().addAll(searchField, searchBtn, showAllBtn);

        // Filter Panel
        VBox filterPanel = new VBox(10);
        filterPanel.getStyleClass().add("filter-panel");

        Label filterLabel = new Label("Filters & Sorting:");
        filterLabel.getStyleClass().add("filter-label");

        HBox filterBox = new HBox(20);
        filterBox.setAlignment(Pos.CENTER_LEFT);

        // CheckBox filters
        filterHighAchievers = new CheckBox("High Achievers (≥75)");
        filterHighAchievers.getStyleClass().add("high-filter");
        filterHighAchievers.setOnAction(e -> filterTable());

        filterLowAchievers = new CheckBox("Low Achievers (<40)");
        filterLowAchievers.getStyleClass().add("low-filter");
        filterLowAchievers.setOnAction(e -> filterTable());

        // Radio buttons for sorting
        sortGroup = new ToggleGroup();
        
        sortByName = new RadioButton("Sort by Name");
        sortByName.setToggleGroup(sortGroup);
        sortByName.setSelected(true);
        sortByName.getStyleClass().add("sort-radio");
        sortByName.setOnAction(e -> sortTable());
        
        sortByMarks = new RadioButton("Sort by Marks");
        sortByMarks.setToggleGroup(sortGroup);
        sortByMarks.getStyleClass().add("sort-radio");
        sortByMarks.setOnAction(e -> sortTable());

        VBox sortBox = new VBox(5);
        sortBox.getChildren().addAll(sortByName, sortByMarks);
        
        filterBox.getChildren().addAll(filterHighAchievers, filterLowAchievers, new Separator(Orientation.VERTICAL), sortBox);
        filterPanel.getChildren().addAll(filterLabel, filterBox);

        // Table
        table = createTable();
        
        displaySection.getChildren().addAll(searchBox, filterPanel, table);

        return displaySection;
    }

    private TableView<Student> createTable() {
        TableView<Student> table = new TableView<>();
        table.getStyleClass().add("table-view");
        table.setPlaceholder(new Label("No students available. Add some students!"));

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Table Columns
        TableColumn<Student, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(50);
        colId.setStyle("-fx-alignment: CENTER;");

        TableColumn<Student, String> colName = new TableColumn<>("Name");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(150);

        TableColumn<Student, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colEmail.setPrefWidth(180);

        TableColumn<Student, String> colCourse = new TableColumn<>("Course");
        colCourse.setCellValueFactory(new PropertyValueFactory<>("course"));
        colCourse.setPrefWidth(150);

        TableColumn<Student, Integer> colMarks = new TableColumn<>("Marks");
        colMarks.setCellValueFactory(new PropertyValueFactory<>("marks"));
        colMarks.setPrefWidth(70);
        colMarks.setStyle("-fx-alignment: CENTER;");
        
        // Add grade column with string manipulation
        TableColumn<Student, String> colGrade = new TableColumn<>("Grade");
        colGrade.setCellValueFactory(cellData -> {
            int marks = cellData.getValue().getMarks();
            String grade = calculateGrade(marks);
            return javafx.beans.binding.Bindings.createObjectBinding(() -> grade);
        });
        colGrade.setPrefWidth(70);
        colGrade.setStyle("-fx-alignment: CENTER;");

        table.getColumns().addAll(colId, colName, colEmail, colCourse, colMarks, colGrade);
        
        // Selection listener
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            selectedStudent = selected;
            if (selected != null) {
                populateFields(selected);
            }
        });

        // Set data
        table.setItems(studentList);
        
        return table;
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox(10);
        statusBar.getStyleClass().add("status-bar");
        statusBar.setAlignment(Pos.CENTER_LEFT);

        statusLabel = new Label("Ready");
        statusLabel.getStyleClass().add("status-label");
        
        countLabel = new Label("Total Students: " + studentList.size());
        countLabel.getStyleClass().add("status-label");
        
        Label timeLabel = new Label(java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        timeLabel.getStyleClass().add("status-label");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusBar.getChildren().addAll(statusLabel, spacer, countLabel, new Separator(Orientation.VERTICAL), timeLabel);
        
        // Update time every second
        PauseTransition pause = new PauseTransition(Duration.seconds(1));
        pause.setOnFinished(e -> {
            timeLabel.setText(java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            pause.play();
        });
        pause.play();
        
        return statusBar;
    }

    // Database Operations
    private void saveStudentToDatabase(Student student) {
        String sql = "INSERT INTO students (user_id, full_name, email, course, marks) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, Login.currentUserId);
            pstmt.setString(2, student.getName());
            pstmt.setString(3, student.getEmail());
            pstmt.setString(4, student.getCourse());
            pstmt.setInt(5, student.getMarks());
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        student.setId(generatedKeys.getInt(1));
                        student.setUserId(Login.currentUserId);
                    }
                }
            }
            
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to save student: " + e.getMessage());
        }
    }

    private void updateStudentInDatabase(Student student) {
        String sql = "UPDATE students SET full_name = ?, email = ?, course = ?, marks = ? WHERE id = ? AND user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, student.getName());
            pstmt.setString(2, student.getEmail());
            pstmt.setString(3, student.getCourse());
            pstmt.setInt(4, student.getMarks());
            pstmt.setInt(5, student.getId());
            pstmt.setInt(6, Login.currentUserId);
            
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to update student: " + e.getMessage());
        }
    }

    private void deleteStudentFromDatabase(int studentId) {
        String sql = "DELETE FROM students WHERE id = ? AND user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, Login.currentUserId);
            
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to delete student: " + e.getMessage());
        }
    }

    // String manipulation methods
    private String calculateGrade(int marks) {
        if (marks >= 90) return "A+";
        else if (marks >= 80) return "A";
        else if (marks >= 70) return "B";
        else if (marks >= 60) return "C";
        else if (marks >= 50) return "D";
        else return "F";
    }

    private boolean validateEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        Pattern pattern = Pattern.compile(emailRegex);
        return pattern.matcher(email).matches();
    }

    private String capitalizeName(String name) {
        if (name == null || name.isEmpty()) return name;
        String[] words = name.trim().split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1).toLowerCase())
                      .append(" ");
            }
        }
        return result.toString().trim();
    }

    // CRUD Operations
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
                updateStatus("Student added successfully!");
                updateStatistics();
            } else {
                showAlert("Error", "Failed to add student to database.");
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
            updateStatus("Student updated successfully!");
            updateStatistics();
        } else {
            showAlert("No Selection", "Please select a student to update.");
        }
    }

    private void deleteStudent() {
        if (selectedStudent != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Delete");
            confirm.setHeaderText("Delete Student");
            confirm.setContentText("Are you sure you want to delete " + selectedStudent.getName() + "?");
            
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                deleteStudentFromDatabase(selectedStudent.getId());
                studentList.remove(selectedStudent);
                clearFields();
                updateStatus("Student deleted successfully!");
                updateStatistics();
            }
        } else {
            showAlert("No Selection", "Please select a student to delete.");
        }
    }

    private void filterTable() {
        FilteredList<Student> filteredData = new FilteredList<>(studentList, p -> true);
        
        filteredData.setPredicate(student -> {
            // Search filter
            if (!searchField.getText().isEmpty()) {
                String searchText = searchField.getText().toLowerCase();
                if (!student.getName().toLowerCase().contains(searchText) &&
                    !student.getEmail().toLowerCase().contains(searchText) &&
                    !student.getCourse().toLowerCase().contains(searchText)) {
                    return false;
                }
            }
            
            // High achievers filter
            if (filterHighAchievers.isSelected() && student.getMarks() < 75) {
                return false;
            }
            
            // Low achievers filter
            if (filterLowAchievers.isSelected() && student.getMarks() >= 40) {
                return false;
            }
            
            return true;
        });
        
        sortTable(filteredData);
    }

    private void sortTable() {
        FilteredList<Student> filteredData = new FilteredList<>(studentList, p -> true);
        filterTable();
    }

    private void sortTable(FilteredList<Student> filteredData) {
        SortedList<Student> sortedData = new SortedList<>(filteredData);
        
        if (sortByName.isSelected()) {
            sortedData.setComparator((a, b) -> a.getName().compareTo(b.getName()));
        } else if (sortByMarks.isSelected()) {
            sortedData.setComparator((a, b) -> Integer.compare(b.getMarks(), a.getMarks()));
        }
        
        table.setItems(sortedData);
    }

    private boolean validateInputs() {
        StringBuilder errors = new StringBuilder();

        if (nameField.getText().trim().isEmpty()) {
            errors.append("• Name is required\n");
        } else if (nameField.getText().length() < 2) {
            errors.append("• Name must be at least 2 characters\n");
        }

        if (emailField.getText().trim().isEmpty()) {
            errors.append("• Email is required\n");
        } else if (!validateEmail(emailField.getText())) {
            errors.append("• Invalid email format\n");
        }

        if (courseCombo.getValue() == null) {
            errors.append("• Please select a course\n");
        }

        if (errors.length() > 0) {
            showAlert("Validation Error", errors.toString());
            return false;
        }

        return true;
    }

    private void populateFields(Student student) {
        nameField.setText(student.getName());
        emailField.setText(student.getEmail());
        courseCombo.setValue(student.getCourse());
        marksSlider.setValue(student.getMarks());
    }

    private void clearFields() {
        nameField.clear();
        emailField.clear();
        courseCombo.setValue(null);
        marksSlider.setValue(50);
        selectedStudent = null;
        table.getSelectionModel().clearSelection();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
        
        // Auto-clear status after 3 seconds
        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> statusLabel.setText("Ready"));
        pause.play();
    }

    private void updateStatistics() {
        countLabel.setText("Total Students: " + studentList.size());
    }

    // Student Model Class with ID and user_id
    public static class Student {
        private int id;
        private int userId;
        private String name, email, course;
        private int marks;

        public Student(int id, int userId, String name, String email, String course, int marks) {
            this.id = id;
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.course = course;
            this.marks = marks;
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        
        public int getUserId() { return userId; }
        public void setUserId(int userId) { this.userId = userId; }
        
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