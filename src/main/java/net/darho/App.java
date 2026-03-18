package net.darho;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.geometry.Rectangle2D;

public class App extends Application {
    
    private static Stage primaryStage;
    
    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        
        stage.initStyle(StageStyle.DECORATED);
        stage.setTitle("Student Management System");
        
        stage.setMinWidth(1000);
        stage.setMinHeight(700);
        
        showLogin();
        stage.show();
    }
    
    public void showLogin() {
        Login login = new Login(this);
        updateScene(new Scene(login.getView()));
        
        primaryStage.setMaximized(false);
        primaryStage.setWidth(950);
        primaryStage.setHeight(700);
        primaryStage.centerOnScreen();
    }
    
    public void showRegister() {
        Register register = new Register(this);
        updateScene(new Scene(register.getView()));
        primaryStage.setMaximized(false);
        primaryStage.centerOnScreen();
    }

    // --- ADDED METHOD FOR FORGOT PASSWORD ---
    public void showForgotPassword() {
        ForgotPassword forgot = new ForgotPassword(this);
        updateScene(new Scene(forgot.getView()));
        primaryStage.setMaximized(false);
        primaryStage.centerOnScreen();
    }
    
    public void showHome() {
        Home home = new Home(this);
        updateScene(new Scene(home.getView()));
        
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        primaryStage.setWidth(bounds.getWidth() * 0.9);
        primaryStage.setHeight(bounds.getHeight() * 0.9);
        
        primaryStage.setMaximized(true); 
        primaryStage.centerOnScreen();
    }

    private void updateScene(Scene scene) {
        try {
            // Updated check: Ensure the resource exists before calling toExternalForm()
            var resource = getClass().getResource("/net/darho/stylee.css");
            if (resource != null) {
                scene.getStylesheets().add(resource.toExternalForm());
            } else {
                System.out.println("Warning: CSS file 'stylee.css' not found in /net/darho/");
            }
        } catch (Exception e) {
            System.out.println("Error loading CSS: " + e.getMessage());
        }
        primaryStage.setScene(scene);
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}