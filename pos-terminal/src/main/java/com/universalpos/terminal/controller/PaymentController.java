package com.universalpos.terminal.controller;

import com.universalpos.terminal.api.dto.CustomerDto;
import com.universalpos.terminal.api.dto.TransactionResponse;
import com.universalpos.terminal.model.CartItem;
import com.universalpos.terminal.service.TransactionService;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Consumer;

/**
 * Payment screen — collects payment method and processes the transaction.
 *
 * How it communicates back to RegisterController:
 *   setup() is called before the dialog opens, passing:
 *     - the cart items
 *     - the selected customer (if any)
 *     - an onSuccess callback — a Consumer<TransactionResponse>
 *   When the sale completes, we call onSuccess.accept(txn) which triggers
 *   RegisterController.onSaleCompleted().
 *
 * This "callback" pattern keeps PaymentController decoupled from RegisterController.
 */
public class PaymentController {

    @FXML private Label             summarySubtotal;
    @FXML private Label             summaryDiscount;
    @FXML private Label             summaryTax;
    @FXML private Label             summaryTotal;
    @FXML private ToggleButton      btnCash;
    @FXML private ToggleButton      btnCredit;
    @FXML private ToggleButton      btnDebit;
    @FXML private ToggleButton      btnGiftCard;
    @FXML private ToggleGroup       paymentGroup;
    @FXML private javafx.scene.layout.VBox cashPanel;
    @FXML private TextField         tenderedField;
    @FXML private Label             changeLabel;
    @FXML private Label             errorLabel;
    @FXML private Button            chargeButton;
    @FXML private ProgressIndicator loadingIndicator;

    private List<CartItem>                cartItems;
    private CustomerDto                   customer;
    private Consumer<TransactionResponse> onSuccess;
    private BigDecimal                    orderTotal;

    private final TransactionService transactionService = new TransactionService();

    /**
     * Called by RegisterController before showing the dialog.
     */
    public void setup(List<CartItem> cartItems, CustomerDto customer,
                      Consumer<TransactionResponse> onSuccess) {
        this.cartItems = cartItems;
        this.customer  = customer;
        this.onSuccess = onSuccess;
    }

    @FXML
    public void initialize() {
        // Wire cash panel visibility to payment method toggle
        paymentGroup.selectedToggleProperty().addListener(
                (obs, old, newToggle) -> {
                    boolean isCash = newToggle == btnCash;
                    cashPanel.setVisible(isCash);
                    cashPanel.setManaged(isCash);
                });

        // Live change calculation as cashier types tendered amount
        tenderedField.textProperty().addListener((obs, old, text) -> {
            if (orderTotal != null) updateChange(text);
        });
    }

    /**
     * Called after initialize() by setup() — but FXML loads before setup().
     * We call this manually after setup() to populate the summary totals.
     */
    public void refreshSummary() {
        if (cartItems == null) return;

        BigDecimal subtotal = cartItems.stream()
                .map(CartItem::calcLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tax   = subtotal.multiply(new BigDecimal("0.0825"))
                           .setScale(2, RoundingMode.HALF_UP);
        orderTotal       = subtotal.add(tax);

        summarySubtotal.setText("$" + subtotal.setScale(2, RoundingMode.HALF_UP));
        summaryDiscount.setText("-$0.00");
        summaryTax.setText("$" + tax);
        summaryTotal.setText("$" + orderTotal);

        // Pre-fill tendered with exact total for convenience
        tenderedField.setText(orderTotal.toPlainString());
    }

    private void updateChange(String tenderedText) {
        try {
            BigDecimal tendered = new BigDecimal(tenderedText);
            BigDecimal change   = tendered.subtract(orderTotal).setScale(2, RoundingMode.HALF_UP);
            changeLabel.setText("Change: $" + (change.compareTo(BigDecimal.ZERO) >= 0
                    ? change : "—"));
            changeLabel.getStyleClass().removeAll("change-negative");
            if (change.compareTo(BigDecimal.ZERO) < 0)
                changeLabel.getStyleClass().add("change-negative");
        } catch (NumberFormatException ignored) {
            changeLabel.setText("Change: $—");
        }
    }

    @FXML
    private void handleCharge() {
        String method = getSelectedMethod();
        BigDecimal tendered = null;

        if ("CASH".equals(method)) {
            try {
                tendered = new BigDecimal(tenderedField.getText().trim());
                if (tendered.compareTo(orderTotal) < 0) {
                    showError("Amount tendered is less than the total.");
                    return;
                }
            } catch (NumberFormatException e) {
                showError("Enter a valid cash amount.");
                return;
            }
        }

        setLoading(true);
        final BigDecimal finalTendered = tendered;
        final Long customerId = customer != null ? customer.getCustomerId() : null;

        Task<TransactionResponse> task = new Task<>() {
            @Override protected TransactionResponse call() throws Exception {
                return transactionService.submitSale(cartItems, customerId,
                        method, finalTendered);
            }
        };

        task.setOnSucceeded(e -> {
            setLoading(false);
            TransactionResponse txn = task.getValue();
            // Notify register screen and close this dialog
            onSuccess.accept(txn);
            showSuccessAndClose(txn);
        });

        task.setOnFailed(e -> {
            setLoading(false);
            showError(task.getException().getMessage());
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void showSuccessAndClose(TransactionResponse txn) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Sale Complete");
        alert.setHeaderText("✓ Transaction " + txn.getReceiptNumber());

        String body = "Total: $" + txn.getTotal() + "\n";
        if (txn.getChangeDue() != null && txn.getChangeDue().compareTo(BigDecimal.ZERO) > 0) {
            body += "Change due: $" + txn.getChangeDue() + "\n";
        }
        if (txn.getLoyaltyPointsEarned() != null && txn.getLoyaltyPointsEarned() > 0) {
            body += "Loyalty points earned: " + txn.getLoyaltyPointsEarned();
        }
        alert.setContentText(body);
        alert.showAndWait();
        ((Stage) chargeButton.getScene().getWindow()).close();
    }

    @FXML
    private void handleCancel() {
        ((Stage) chargeButton.getScene().getWindow()).close();
    }

    private String getSelectedMethod() {
        if (paymentGroup.getSelectedToggle() == btnCredit)   return "CREDIT_CARD";
        if (paymentGroup.getSelectedToggle() == btnDebit)    return "DEBIT_CARD";
        if (paymentGroup.getSelectedToggle() == btnGiftCard) return "GIFT_CARD";
        return "CASH";
    }

    private void setLoading(boolean loading) {
        chargeButton.setDisable(loading);
        loadingIndicator.setVisible(loading);
        loadingIndicator.setManaged(loading);
    }

    private void showError(String msg) {
        errorLabel.setText("⚠ " + msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
