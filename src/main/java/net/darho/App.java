package net.darho;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class App extends Application {
    
    private static Stage primaryStage;
    
    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        
        // Configure stage
        stage.initStyle(StageStyle.DECORATED);
        stage.setTitle("Student Management System");
        stage.setMinWidth(900);
        stage.setMinHeight(650);
        
        // Start with login view
        showLogin();
        
        stage.show();
    }
    
    public void showLogin() {
        Login login = new Login(this);
        Scene scene = new Scene(login.getView(), 900, 650);
        scene.getStylesheets().add(getClass().getResource("/net/darho/stylee.css").toExternalForm());
        primaryStage.setScene(scene);
    }
    
    public void showRegister() {
        Register register = new Register(this);
        Scene scene = new Scene(register.getView(), 900, 650);
        scene.getStylesheets().add(getClass().getResource("/net/darho/stylee.css").toExternalForm());
        primaryStage.setScene(scene);
    }
    
    public void showHome() {
        Home home = new Home(this);
        Scene scene = new Scene(home.getView(), 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/net/darho/stylee.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}