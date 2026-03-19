package com.universalpos.terminal;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * UniversalPOS Terminal — Entry Point
 *
 * How JavaFX apps start:
 *   1. main() calls launch() — this is JavaFX's bootstrap method
 *   2. JavaFX creates the application thread
 *   3. start(Stage) is called on that thread — this is where we build the UI
 *
 * Stage  = the window
 * Scene  = what's inside the window (the FXML layout + stylesheet)
 * FXML   = the layout file (like HTML for the UI structure)
 * CSS    = the stylesheet (like CSS for the UI appearance)
 *
 * To run from Maven:
 *   cd pos-terminal
 *   mvn javafx:run
 *
 * The backend API must be running at localhost:8080 first.
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load the login screen FXML
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/login.fxml"));
        Parent root = loader.load();

        // Build the scene with our dark stylesheet applied
        Scene scene = new Scene(root, 1280, 720);
        scene.getStylesheets().add(
                getClass().getResource("/css/terminal.css").toExternalForm());

        primaryStage.setTitle("UniversalPOS Terminal");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1280);
        primaryStage.setMinHeight(720);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
