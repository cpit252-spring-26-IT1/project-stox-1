package sa.edu.kau.fcit.cpit252.project;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import sa.edu.kau.fcit.cpit252.project.db.DatabaseConnection;

/**
 * Main JavaFX Application
 */
public class App extends Application {

    @Override
    public void start(Stage stage) {
        // Test database connection
        System.out.println("Testing Database Connection on Startup...");
        DatabaseConnection.getConnection();

        Label label = new Label("Welcome to Stox!");
        Scene scene = new Scene(new StackPane(label), 640, 480);
        
        stage.setTitle("Stox Application");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
