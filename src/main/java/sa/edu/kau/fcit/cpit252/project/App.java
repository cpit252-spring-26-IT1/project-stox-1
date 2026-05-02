package sa.edu.kau.fcit.cpit252.project;

import javafx.application.Application;
import javafx.stage.Stage;
import sa.edu.kau.fcit.cpit252.project.db.DatabaseConnection;
import sa.edu.kau.fcit.cpit252.project.ui.MainView;

/**
 * Application entry point.
 * initialize the DB connection, then hand off to MainView.
 */
public class App extends Application {

    @Override
    public void start(Stage stage) {
        // ensures only one DB connection exists
        DatabaseConnection.getConnection();

        // delegate all UI construction to MainView
        new MainView().show(stage);
    }

    public static void main(String[] args) {
        launch();
    }
}
