package com.universalpos.terminal;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * UniversalPOS Terminal — JavaFX Entry Point
 *
 * Phase 1: Stub launcher.
 * Phase 5: Full POS terminal UI (login, cart, customer lookup,
 *          payment screen, receipt prompt).
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        Label label = new Label("UniversalPOS Terminal — Phase 5 UI coming soon!");
        StackPane root = new StackPane(label);
        Scene scene = new Scene(root, 1280, 720);

        primaryStage.setTitle("UniversalPOS Terminal");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
