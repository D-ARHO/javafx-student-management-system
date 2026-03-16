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
        String css = getClass().getResource("/net/darho/stylee.css").toExternalForm();
        if (css != null) {
            scene.getStylesheets().add(css);
        }
        primaryStage.setScene(scene);
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}