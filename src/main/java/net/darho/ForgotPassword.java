package net.darho;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.sql.*;

public class ForgotPassword {

    private final App app;

    public ForgotPassword(App app) {
        this.app = app;
    }

    public Parent getView() {
        Label header = new Label("Reset Password");
        header.getStyleClass().add("header-label");

        TextField emailField = new TextField();
        emailField.setPromptText("Enter your registered email");

        Label questionLabel = new Label("Verify your identity");
        questionLabel.setStyle("-fx-text-fill: steel blue; -fx-font-weight: bold;");
        questionLabel.setVisible(false);

        TextField answerField = new TextField();
        answerField.setPromptText("Your security answer");
        answerField.setVisible(false);

        PasswordField newPassField = new PasswordField();
        newPassField.setPromptText("New Password");
        newPassField.setVisible(false);

        Button actionBtn = new Button("Verify Email");
        actionBtn.getStyleClass().add("primary-button");
        actionBtn.setMaxWidth(Double.MAX_VALUE);

        Hyperlink backLink = new Hyperlink("Back to Login");
        backLink.setOnAction(e -> app.showLogin());

        VBox card = new VBox(20, header, new Label("Email"), emailField, questionLabel, answerField, newPassField, actionBtn, backLink);
        card.getStyleClass().add("glass-card");
        card.setPadding(new Insets(40));
        card.setMaxSize(420, 500);

        actionBtn.setOnAction(e -> {
            if (actionBtn.getText().equals("Verify Email")) {
                handleFetchQuestion(emailField.getText(), questionLabel, answerField, newPassField, actionBtn);
            } else {
                handleUpdatePassword(emailField.getText(), answerField.getText(), newPassField.getText());
            }
        });

        StackPane root = new StackPane(card);
        root.getStyleClass().add("main-background");
        return root;
    }

    private void handleFetchQuestion(String email, Label qLab, TextField aFld, PasswordField pFld, Button btn) {
        String sql = "SELECT security_question FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                qLab.setText("Question: " + rs.getString("security_question"));
                qLab.setVisible(true); aFld.setVisible(true); pFld.setVisible(true);
                btn.setText("Reset Password");
            } else {
                showAlert("Error", "No account found with this email.");
            }
        } catch (SQLException ex) { showAlert("Error", ex.getMessage()); }
    }

    private void handleUpdatePassword(String email, String answer, String newPass) {
        String sql = "UPDATE users SET password = ? WHERE email = ? AND security_answer = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPass);
            ps.setString(2, email);
            ps.setString(3, answer);
            if (ps.executeUpdate() > 0) {
                showAlert("Success", "Password updated! Please login.");
                app.showLogin();
            } else {
                showAlert("Error", "Incorrect answer!");
            }
        } catch (SQLException ex) { showAlert("Error", ex.getMessage()); }
    }

    private void showAlert(String t, String c) {
        Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(t); a.setContentText(c); a.show();
    }
}
