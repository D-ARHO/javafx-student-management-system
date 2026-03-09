package net.darho;

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

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email.matches(emailRegex);
    }

    public Register(App app) {
        this.app = app;
    }

    public Parent getView() {
        // Header
        Label registerLabel = new Label("Create Account");
        registerLabel.getStyleClass().add("header-label");

        // Full Name Section
        Label nameLabel = new Label("Full Name");
        nameLabel.getStyleClass().add("form-label");
        TextField nameField = new TextField();
        nameField.setPromptText("PRINCE Darho");
        nameField.getStyleClass().add("text-field");
        VBox nameBox = new VBox(5, nameLabel, nameField);

        // Email Section
        Label emailLabel = new Label("Email");
        emailLabel.getStyleClass().add("form-label");
        TextField emailField = new TextField();
        emailField.setPromptText("username@gmail.com");
        emailField.getStyleClass().add("text-field");
        VBox emailBox = new VBox(5, emailLabel, emailField);

        // Password Section
        Label passwordLabel = new Label("Password");
        passwordLabel.getStyleClass().add("form-label");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Create Password");
        passwordField.getStyleClass().add("password-field");
        VBox passwordBox = new VBox(5, passwordLabel, passwordField);

        // Confirm Password Section
        Label confirmLabel = new Label("Confirm Password");
        confirmLabel.getStyleClass().add("form-label");
        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Repeat Password");
        confirmField.getStyleClass().add("password-field");
        VBox confirmBox = new VBox(5, confirmLabel, confirmField);

        // Action Button
        Button signUpBtn = new Button("Register");
        signUpBtn.setMaxWidth(Double.MAX_VALUE);
        signUpBtn.getStyleClass().add("primary-button");

        // Footer
        Label hasAccountLabel = new Label("Already have an account?");
        hasAccountLabel.getStyleClass().add("secondary-text");
        Hyperlink loginLink = new Hyperlink("Login Here");
        loginLink.getStyleClass().add("link-style");
        
        loginLink.setOnAction(e -> app.showLogin());

        HBox footerBox = new HBox(5, hasAccountLabel, loginLink);
        footerBox.setAlignment(Pos.CENTER);

        // Glass Card Container
        VBox registerCard = new VBox(15);
        registerCard.getStyleClass().add("glass-card");
        registerCard.setPadding(new Insets(40));
        registerCard.getChildren().addAll(
                registerLabel, 
                nameBox, 
                emailBox, 
                passwordBox, 
                confirmBox, 
                signUpBtn, 
                footerBox
        );

        registerCard.setMaxSize(420, 600);
        registerCard.setAlignment(Pos.TOP_LEFT);

        // Root Layout
        StackPane root = new StackPane(registerCard);
        root.getStyleClass().add("main-background");
        StackPane.setAlignment(registerCard, Pos.CENTER);

        signUpBtn.setOnAction(e -> {
            String name = nameField.getText();
            String email = emailField.getText();
            String password = passwordField.getText();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                System.out.println("Please fill all fields!");
                return;
            }

            if (!isValidEmail(email)) {
                System.out.println("Invalid email format!");
                return;
            }   

            String sql = "INSERT INTO users (full_name, email, password) VALUES(?,?,?)";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement psmt = conn.prepareStatement(sql)) {

                psmt.setString(1, name);
                psmt.setString(2, email);
                psmt.setString(3, password);

                int rowsAffected = psmt.executeUpdate();

                if (rowsAffected > 0) {
                    System.out.println("Register Successfully!!!");
                    app.showLogin();
                } else {
                    System.out.println("Register Failed");
                }

            } catch (SQLException ex) {
                System.out.println("Connection failed! " + ex);
            }
        });

        return root;
    }
}