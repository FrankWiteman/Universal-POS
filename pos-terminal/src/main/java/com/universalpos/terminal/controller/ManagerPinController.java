package com.universalpos.terminal.controller;

import com.universalpos.terminal.service.AuthService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

/**
 * Manager verification overlay.
 * Rather than a PIN, we use full credential re-authentication —
 * the manager enters their email and password. If the login succeeds
 * and the returned role is MANAGER or ADMIN, we proceed.
 *
 * Why re-auth instead of a PIN?
 *   - No separate PIN table to manage
 *   - Reuses the existing auth endpoint
 *   - Auditable — the audit log shows the manager's actual employee ID
 */
public class ManagerPinController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button        verifyButton;
    @FXML private Label         errorLabel;

    private Runnable onVerified;

    private final AuthService authService = new AuthService();

    /** Called by the parent screen to set what happens after verification */
    public void setOnVerified(Runnable callback) {
        this.onVerified = callback;
    }

    @FXML
    public void initialize() {
        passwordField.setOnAction(e -> handleVerify());
    }

    @FXML
    private void handleVerify() {
        String email = emailField.getText().trim();
        String pass  = passwordField.getText();
        String tenant = com.universalpos.terminal.model.SessionState
                .getInstance().getTenantSlug();

        if (email.isEmpty() || pass.isEmpty()) {
            showError("Enter email and password.");
            return;
        }

        verifyButton.setDisable(true);

        // Save current session state — we'll restore it after verification
        com.universalpos.terminal.model.SessionState session =
                com.universalpos.terminal.model.SessionState.getInstance();
        String savedToken  = session.getJwtToken();
        Long   savedEmpId  = session.getEmployeeId();
        String savedName   = session.getEmployeeName();
        String savedRole   = session.getRole();
        String savedTenant = session.getTenantSlug();

        Task<String> task = new Task<>() {
            @Override protected String call() throws Exception {
                return authService.login(email, pass, tenant);
            }
        };

        task.setOnSucceeded(e -> {
            String verifiedRole = session.getRole();
            // Restore the original cashier session
            session.login(savedToken, savedEmpId, savedName, savedRole, savedTenant);

            if ("MANAGER".equals(verifiedRole) || "ADMIN".equals(verifiedRole)) {
                verifyButton.setDisable(false);
                ((Stage) verifyButton.getScene().getWindow()).close();
                if (onVerified != null) onVerified.run();
            } else {
                verifyButton.setDisable(false);
                showError("Account does not have Manager or Admin role.");
            }
        });

        task.setOnFailed(e -> {
            session.login(savedToken, savedEmpId, savedName, savedRole, savedTenant);
            verifyButton.setDisable(false);
            showError("Invalid credentials.");
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void handleCancel() {
        ((Stage) verifyButton.getScene().getWindow()).close();
    }

    private void showError(String msg) {
        errorLabel.setText("⚠ " + msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
