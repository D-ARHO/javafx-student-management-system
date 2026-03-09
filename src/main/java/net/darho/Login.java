package net.darho;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Login {

    private final App app;
    public static int currentUserId = -1;
    public static String currentUserName = "";

    public Login(App app) {
        this.app = app;
    }

    public Parent getView() {
        // --- Header ---
        Label loginLabel = new Label("Login");
        loginLabel.getStyleClass().add("header-label");

        // --- Input Fields ---
        Label emailLabel = new Label("Email");
        emailLabel.getStyleClass().add("form-label");
        TextField emailField = new TextField();
        emailField.setPromptText("username@gmail.com");
        emailField.getStyleClass().add("text-field");
        VBox emailBox = new VBox(5, emailLabel, emailField);

        Label passwordLabel = new Label("Password");
        passwordLabel.getStyleClass().add("form-label");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.getStyleClass().add("password-field");
        VBox passwordBox = new VBox(5, passwordLabel, passwordField);

        // --- Options & Links ---
        CheckBox rememberMe = new CheckBox("Remember Me");
        rememberMe.getStyleClass().add("secondary-text");

        Hyperlink forgotPassword = new Hyperlink("Forgot Password?");
        forgotPassword.getStyleClass().add("link-style");

        HBox optionsBox = new HBox();
        optionsBox.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        optionsBox.getChildren().addAll(rememberMe, spacer, forgotPassword);

        // --- Status/Message Area ---
        Label messageLabel = new Label();
        messageLabel.setWrapText(true);
        messageLabel.setMinWidth(300);
        messageLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-weight: bold;");

        // --- Action Buttons ---
        Button signInBtn = new Button("Sign in");
        signInBtn.setMaxWidth(Double.MAX_VALUE);
        signInBtn.getStyleClass().add("primary-button");

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);

        // --- Logic: Sign In ---
        signInBtn.setOnAction(e -> {
            String email = emailField.getText();
            String password = passwordField.getText();

            if (email.isEmpty() || password.isEmpty()) {
                messageLabel.setText("Please enter credentials!");
                return;
            }

            messageLabel.setText(""); 
            progressBar.setVisible(true);
            progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
            signInBtn.setDisable(true);

            PauseTransition pause = new PauseTransition(Duration.seconds(0.8));
            pause.setOnFinished(event -> handleLogin(email, password, messageLabel, progressBar, signInBtn, rememberMe.isSelected()));
            pause.play();
        });

        // --- Social Buttons ---
        Label continueLabel = new Label("or continue with");
        continueLabel.getStyleClass().add("secondary-text");

        Button googleBtn = new Button("Google");
        Button githubBtn = new Button("GitHub");
        googleBtn.getStyleClass().add("social-button");
        githubBtn.getStyleClass().add("social-button");
        googleBtn.setPrefWidth(150);
        githubBtn.setPrefWidth(150);

        HBox socialBox = new HBox(15, googleBtn, githubBtn);
        socialBox.setAlignment(Pos.CENTER);

        // --- Footer ---
        Label noAccountLabel = new Label("Don't have an account?");
        noAccountLabel.getStyleClass().add("secondary-text");
        Hyperlink registerLink = new Hyperlink("Register Here");
        registerLink.getStyleClass().add("link-style");
        registerLink.setOnAction(e -> app.showRegister());

        HBox footerBox = new HBox(5, noAccountLabel, registerLink);
        footerBox.setAlignment(Pos.CENTER);

        // --- Final Assembly ---
        VBox loginCard = new VBox(20);
        loginCard.getStyleClass().add("glass-card");
        loginCard.setPadding(new Insets(40));
        
        loginCard.getChildren().addAll(
                loginLabel, 
                emailBox, passwordBox, 
                optionsBox, 
                messageLabel, signInBtn, progressBar, 
                continueLabel, socialBox, footerBox
        );

        loginCard.setMaxSize(420, 600);
        loginCard.setAlignment(Pos.TOP_LEFT);

        StackPane root = new StackPane(loginCard);
        root.getStyleClass().add("main-background");
        StackPane.setAlignment(loginCard, Pos.CENTER);

        return root;
    }

    private void handleLogin(String email, String password, Label msg, ProgressBar pb, Button btn, boolean remember) {
        String sql = "SELECT * FROM users WHERE email = ? AND password = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement psmt = conn.prepareStatement(sql)) {
            
            psmt.setString(1, email);
            psmt.setString(2, password);
            ResultSet rs = psmt.executeQuery();

            if (rs.next()) {
                // Store the logged-in user's ID and name
                currentUserId = rs.getInt("id");
                currentUserName = rs.getString("full_name");
                
                msg.setStyle("-fx-text-fill: #51cf66;"); 
                msg.setText("Login Successful! Welcome " + currentUserName);
                pb.setProgress(1.0);
                
                PauseTransition delay = new PauseTransition(Duration.seconds(0.5));
                delay.setOnFinished(ev -> app.showHome());
                delay.play();
            } else {
                msg.setStyle("-fx-text-fill: #ff6b6b;");
                msg.setText("Invalid email or password.");
                pb.setVisible(false);
                btn.setDisable(false);
            }
        } catch (SQLException ex) {
            msg.setText("Database Connection Failed!");
            pb.setVisible(false);
            btn.setDisable(false);
        }
    }
}