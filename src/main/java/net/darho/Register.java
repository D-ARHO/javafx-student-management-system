package net.darho;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Register {

    private final App app;

    public Register(App app) {
        this.app = app;
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email.matches(emailRegex);
    }

    public Parent getView() {
        // --- Header ---
        Label registerLabel = new Label("Create Account");
        registerLabel.getStyleClass().add("header-label");

        // --- Input Fields ---
        Label nameLabel = new Label("Full Name");
        nameLabel.getStyleClass().add("form-label");
        TextField nameField = new TextField();
        nameField.setPromptText("Enter your full name");
        nameField.getStyleClass().add("text-field");

        Label emailLabel = new Label("Email");
        emailLabel.getStyleClass().add("form-label");
        TextField emailField = new TextField();
        emailField.setPromptText("username@gmail.com");
        emailField.getStyleClass().add("text-field");

        // --- Password Section with Eye Logic ---
        Label passwordLabel = new Label("Password");
        passwordLabel.getStyleClass().add("form-label");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Create Password");
        passwordField.getStyleClass().add("password-field");
        HBox.setHgrow(passwordField, Priority.ALWAYS);

        TextField passwordTextVisible = new TextField();
        passwordTextVisible.setPromptText("Create Password");
        passwordTextVisible.getStyleClass().add("text-field");
        passwordTextVisible.setManaged(false);
        passwordTextVisible.setVisible(false);
        HBox.setHgrow(passwordTextVisible, Priority.ALWAYS);
        passwordTextVisible.textProperty().bindBidirectional(passwordField.textProperty());

        ToggleButton eyeBtn1 = new ToggleButton("👁");
        eyeBtn1.getStyleClass().add("social-button");
        eyeBtn1.setOnAction(e -> toggleVisibility(eyeBtn1, passwordField, passwordTextVisible));

        HBox passwordRow = new HBox(5, passwordField, passwordTextVisible, eyeBtn1);
        passwordRow.setAlignment(Pos.CENTER_LEFT);

        // --- Confirm Password Section with Eye Logic ---
        Label confirmLabel = new Label("Confirm Password");
        confirmLabel.getStyleClass().add("form-label");
        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Repeat Password");
        confirmField.getStyleClass().add("password-field");
        HBox.setHgrow(confirmField, Priority.ALWAYS);

        TextField confirmTextVisible = new TextField();
        confirmTextVisible.setPromptText("Repeat Password");
        confirmTextVisible.getStyleClass().add("text-field");
        confirmTextVisible.setManaged(false);
        confirmTextVisible.setVisible(false);
        HBox.setHgrow(confirmTextVisible, Priority.ALWAYS);
        confirmTextVisible.textProperty().bindBidirectional(confirmField.textProperty());

        ToggleButton eyeBtn2 = new ToggleButton("👁");
        eyeBtn2.getStyleClass().add("social-button");
        eyeBtn2.setOnAction(e -> toggleVisibility(eyeBtn2, confirmField, confirmTextVisible));

        HBox confirmRow = new HBox(5, confirmField, confirmTextVisible, eyeBtn2);
        confirmRow.setAlignment(Pos.CENTER_LEFT);

        // --- Security Question ---
        Label questionLabel = new Label("Security Question (for password recovery)");
        questionLabel.getStyleClass().add("form-label");
        ComboBox<String> questionBox = new ComboBox<>(FXCollections.observableArrayList(
            "What is your pet's name?", 
            "What is your mother's maiden name?", 
            "What was your first car?",
            "What city were you born in?"
        ));
        questionBox.setPromptText("Select a question");
        questionBox.setMaxWidth(Double.MAX_VALUE);

        Label answerLabel = new Label("Security Answer");
        answerLabel.getStyleClass().add("form-label");
        TextField answerField = new TextField();
        answerField.setPromptText("Your secret answer");
        answerField.getStyleClass().add("text-field");

        // --- Action Button ---
        Button signUpBtn = new Button("Register");
        signUpBtn.setMaxWidth(Double.MAX_VALUE);
        signUpBtn.getStyleClass().add("primary-button");

        // --- ENTER KEY FLOW LOGIC ---
        nameField.setOnAction(e -> emailField.requestFocus());
        emailField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> confirmField.requestFocus());
        passwordTextVisible.setOnAction(e -> confirmField.requestFocus());
        confirmField.setOnAction(e -> questionBox.requestFocus());
        confirmTextVisible.setOnAction(e -> questionBox.requestFocus());
        answerField.setOnAction(e -> signUpBtn.fire());

        // --- Footer ---
        Label hasAccountLabel = new Label("Already have an account?");
        hasAccountLabel.getStyleClass().add("secondary-text");
        Hyperlink loginLink = new Hyperlink("Login Here");
        loginLink.getStyleClass().add("link-style");
        loginLink.setOnAction(e -> app.showLogin());

        HBox footerBox = new HBox(5, hasAccountLabel, loginLink);
        footerBox.setAlignment(Pos.CENTER);

        // --- Layout Assembly (Exactly your VBox structure) ---
        VBox registerCard = new VBox(12);
        registerCard.getStyleClass().add("glass-card");
        registerCard.setPadding(new Insets(30, 40, 30, 40));
        registerCard.getChildren().addAll(
                registerLabel, 
                nameLabel, nameField, 
                emailLabel, emailField, 
                passwordLabel, passwordRow, 
                confirmLabel, confirmRow,
                questionLabel, questionBox,
                answerLabel, answerField,
                signUpBtn, 
                footerBox
        );

        registerCard.setMaxSize(450, 850);
        registerCard.setAlignment(Pos.TOP_LEFT);

        StackPane root = new StackPane(registerCard);
        root.getStyleClass().add("main-background");

        // --- Logic: Sign Up ---
        signUpBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            String password = passwordField.getText();
            String confirm = confirmField.getText();
            String question = questionBox.getValue();
            String answer = answerField.getText().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || question == null || answer.isEmpty()) {
                showAlert("Registration Error", "All fields are required!");
                return;
            }

            if (!isValidEmail(email)) {
                showAlert("Invalid Email", "Please enter a valid email address.");
                return;
            }

            if (!password.equals(confirm)) {
                showAlert("Password Mismatch", "Passwords do not match!");
                return;
            }

            String sql = "INSERT INTO users (full_name, email, password, security_question, security_answer) VALUES(?,?,?,?,?)";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement psmt = conn.prepareStatement(sql)) {

                psmt.setString(1, name);
                psmt.setString(2, email);
                psmt.setString(3, password);
                psmt.setString(4, question);
                psmt.setString(5, answer);

                int rowsAffected = psmt.executeUpdate();
                if (rowsAffected > 0) {
                    showAlert("Success", "Account created successfully! Please login.");
                    app.showLogin();
                }

            } catch (SQLException ex) {
                showAlert("Database Error", "Registration failed: " + ex.getMessage());
            }
        });

        return root;
    }

    // Toggle visibility helper
    private void toggleVisibility(ToggleButton btn, PasswordField pf, TextField tf) {
        boolean show = btn.isSelected();
        btn.setText(show ? "🙈" : "👁");
        pf.setManaged(!show);
        pf.setVisible(!show);
        tf.setManaged(show);
        tf.setVisible(show);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}