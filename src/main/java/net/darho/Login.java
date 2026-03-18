package net.darho;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import java.io.*;
import java.util.*;
import java.sql.*;

public class Login {

    private final App app;
    private final String REMEMBER_FILE = "remember_me.txt"; 
    public static int currentUserId = -1;
    public static String currentUserName = "";

    public Login(App app) {
        this.app = app;
    }

    public Parent getView() {
        Label loginLabel = new Label("Login");
        loginLabel.getStyleClass().add("header-label");

        Label emailLabel = new Label("Email");
        emailLabel.getStyleClass().add("form-label");
        
        ComboBox<String> emailField = new ComboBox<>();
        emailField.setEditable(true); 
        emailField.setPromptText("username@gmail.com");
        emailField.getStyleClass().add("combo-box");
        emailField.setMaxWidth(Double.MAX_VALUE);
        
        VBox emailBox = new VBox(5, emailLabel, emailField);

        Label passwordLabel = new Label("Password");
        passwordLabel.getStyleClass().add("form-label");

        // --- PASSWORD VISIBILITY LOGIC ---
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.getStyleClass().add("password-field");
        HBox.setHgrow(passwordField, Priority.ALWAYS); // Let it take available space

        TextField passwordTextVisible = new TextField();
        passwordTextVisible.setPromptText("Password");
        passwordTextVisible.getStyleClass().add("text-field");
        passwordTextVisible.setManaged(false);
        passwordTextVisible.setVisible(false);
        HBox.setHgrow(passwordTextVisible, Priority.ALWAYS);

        // Sync both fields
        passwordTextVisible.textProperty().bindBidirectional(passwordField.textProperty());

        ToggleButton eyeBtn = new ToggleButton("👁");
        eyeBtn.getStyleClass().add("social-button"); // Uses your button style
        eyeBtn.setMinWidth(45);

        eyeBtn.setOnAction(e -> {
            boolean show = eyeBtn.isSelected();
            eyeBtn.setText(show ? "🙈" : "👁");
            passwordField.setManaged(!show);
            passwordField.setVisible(!show);
            passwordTextVisible.setManaged(show);
            passwordTextVisible.setVisible(show);
        });

        // HBox to keep password fields and eye on the same line
        HBox passwordInputRow = new HBox(5, passwordField, passwordTextVisible, eyeBtn);
        passwordInputRow.setAlignment(Pos.CENTER_LEFT);
        
        VBox passwordBox = new VBox(5, passwordLabel, passwordInputRow);

        // --- BUTTONS & PROGRESS ---
        Button signInBtn = new Button("Sign in");
        signInBtn.setMaxWidth(Double.MAX_VALUE);
        signInBtn.getStyleClass().add("primary-button");

        // Logic for Enter Key
        emailField.getEditor().setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> signInBtn.fire());
        passwordTextVisible.setOnAction(e -> signInBtn.fire());

        CheckBox rememberMe = new CheckBox("Remember Me");
        rememberMe.getStyleClass().add("secondary-text");

        loadSavedEmails(emailField, rememberMe);

        Hyperlink forgotPassword = new Hyperlink("Forgot Password?");
        forgotPassword.getStyleClass().add("link-style");
        forgotPassword.setOnAction(e -> app.showForgotPassword());

        HBox optionsBox = new HBox();
        optionsBox.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        optionsBox.getChildren().addAll(rememberMe, spacer, forgotPassword);

        Label messageLabel = new Label();
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-weight: bold;");

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);

        signInBtn.setOnAction(e -> {
            String email = emailField.getEditor().getText().trim();
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

       

        Label noAccountLabel = new Label("Don't have an account?");
        noAccountLabel.getStyleClass().add("secondary-text");
        Hyperlink registerLink = new Hyperlink("Register Here");
        registerLink.getStyleClass().add("link-style");
        registerLink.setOnAction(e -> app.showRegister());

        HBox footerBox = new HBox(5, noAccountLabel, registerLink);
        footerBox.setAlignment(Pos.CENTER);

        // --- ASSEMBLY (Your exact layout preserved) ---
        VBox loginCard = new VBox(20);
        loginCard.getStyleClass().add("glass-card");
        loginCard.setPadding(new Insets(40));
        loginCard.getChildren().addAll(loginLabel, emailBox, passwordBox, optionsBox, messageLabel, signInBtn, progressBar, footerBox);
        loginCard.setMaxSize(420, 600);
        loginCard.setAlignment(Pos.TOP_LEFT);

        StackPane root = new StackPane(loginCard);
        root.getStyleClass().add("main-background");
        return root;
    }

    // --- HELPER METHODS ---
    private void loadSavedEmails(ComboBox<String> combo, CheckBox cb) {
        try {
            File file = new File(REMEMBER_FILE);
            if (file.exists()) {
                Set<String> emails = new LinkedHashSet<>();
                Scanner scanner = new Scanner(file);
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine().trim();
                    if (!line.isEmpty()) emails.add(line);
                }
                scanner.close();
                if (!emails.isEmpty()) {
                    combo.getItems().addAll(emails);
                    combo.getSelectionModel().selectFirst();
                    cb.setSelected(true);
                }
            }
        } catch (Exception ex) { }
    }

    private void handleLogin(String email, String password, Label msg, ProgressBar pb, Button btn, boolean remember) {
        String sql = "SELECT * FROM users WHERE email = ? AND password = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement psmt = conn.prepareStatement(sql)) {
            psmt.setString(1, email);
            psmt.setString(2, password);
            ResultSet rs = psmt.executeQuery();

            if (rs.next()) {
                saveEmailToFile(email, remember);
                currentUserId = rs.getInt("id");
                currentUserName = rs.getString("full_name");
                msg.setStyle("-fx-text-fill: #51cf66;"); 
                msg.setText("Login Successful!");
                pb.setProgress(1.0);
                PauseTransition delay = new PauseTransition(Duration.seconds(0.3));
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

    private void saveEmailToFile(String email, boolean remember) {
        try {
            Set<String> emails = new LinkedHashSet<>();
            File file = new File(REMEMBER_FILE);
            if (file.exists()) {
                Scanner scanner = new Scanner(file);
                while (scanner.hasNextLine()) emails.add(scanner.nextLine().trim());
                scanner.close();
            }
            if (remember) {
                emails.remove(email);
                Set<String> reordered = new LinkedHashSet<>();
                reordered.add(email);
                reordered.addAll(emails);
                emails = reordered;
            } else {
                emails.remove(email);
            }
            PrintWriter out = new PrintWriter(file);
            for (String e : emails) out.println(e);
            out.close();
        } catch (Exception ex) { }
    }
}