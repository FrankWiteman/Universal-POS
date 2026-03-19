package com.universalpos.terminal.controller;

import com.universalpos.terminal.service.AuthService;
import com.universalpos.terminal.api.ApiClient.ApiException;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

/**
 * Controller for the login screen.
 *
 * How JavaFX controllers work:
 *   1. JavaFX loads login.fxml
 *   2. It creates a LoginController instance
 *   3. It injects the UI elements by matching @FXML field names to fx:id values
 *   4. It calls initialize() if it exists
 *   5. User interactions trigger @FXML-annotated methods like handleLogin()
 *
 * Why run the login on a background thread (Task)?
 *   JavaFX has one UI thread. If you make a network call on it, the whole
 *   window freezes until it finishes. Task<> runs work in the background
 *   and lets you update the UI safely once it's done via Platform.runLater().
 */
public class LoginController {

    // @FXML injects these from the FXML file by matching the fx:id values
    @FXML private TextField         emailField;
    @FXML private PasswordField     passwordField;
    @FXML private TextField         tenantSlugField;
    @FXML private Button            loginButton;
    @FXML private Label             errorLabel;
    @FXML private ProgressIndicator loadingIndicator;

    private final AuthService authService = new AuthService();

    /** Pre-fill tenant slug for convenience */
    @FXML
    public void initialize() {
        tenantSlugField.setText("demo-store");

        // Allow pressing Enter in any field to trigger login
        emailField.setOnAction(e -> handleLogin());
        passwordField.setOnAction(e -> handleLogin());
        tenantSlugField.setOnAction(e -> handleLogin());
    }

    /** Called when the Sign In button is clicked (wired via onAction="#handleLogin") */
    @FXML
    private void handleLogin() {
        String email  = emailField.getText().trim();
        String pass   = passwordField.getText();
        String tenant = tenantSlugField.getText().trim();

        // Basic client-side validation before hitting the network
        if (email.isEmpty() || pass.isEmpty() || tenant.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }

        // Show loading state
        setLoading(true);

        /*
         * Task<String> runs login on a background thread.
         * call() does the work, setOnSucceeded/setOnFailed run back on the UI thread.
         * This pattern is how you do "async" work in JavaFX.
         */
        Task<String> loginTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                // This runs on a background thread — safe to make network calls here
                return authService.login(email, pass, tenant);
            }
        };

        loginTask.setOnSucceeded(e -> {
            // Back on UI thread — navigate to the register screen
            setLoading(false);
            navigateToRegister();
        });

        loginTask.setOnFailed(e -> {
            // Back on UI thread — show the error message
            setLoading(false);
            Throwable cause = loginTask.getException();
            String msg = cause instanceof ApiException
                    ? cause.getMessage()
                    : "Connection failed. Is the server running?";
            showError(msg);
        });

        // Start the task on a daemon thread (won't block app shutdown)
        Thread thread = new Thread(loginTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void navigateToRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/register.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root, 1280, 720));
            stage.setTitle("UniversalPOS — Register");
        } catch (Exception e) {
            showError("Failed to load register screen: " + e.getMessage());
        }
    }

    private void setLoading(boolean loading) {
        loginButton.setDisable(loading);
        loadingIndicator.setVisible(loading);
        loadingIndicator.setManaged(loading);
        if (loading) hideError();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
