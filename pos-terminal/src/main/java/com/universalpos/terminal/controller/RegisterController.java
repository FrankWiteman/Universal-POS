package com.universalpos.terminal.controller;

import com.universalpos.terminal.api.ApiClient.ApiException;
import com.universalpos.terminal.api.dto.CustomerDto;
import com.universalpos.terminal.api.dto.ProductDto;
import com.universalpos.terminal.api.dto.TransactionResponse;
import com.universalpos.terminal.model.CartItem;
import com.universalpos.terminal.model.SessionState;
import com.universalpos.terminal.service.CustomerService;
import com.universalpos.terminal.service.ProductService;
import com.universalpos.terminal.service.TransactionService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Controller for the main register screen — the heart of the terminal.
 *
 * Manages:
 *   - Product search and adding to cart
 *   - Customer lookup and attachment
 *   - Cart display with live totals
 *   - Checkout (opens payment screen)
 *   - Void (manager PIN required)
 *   - Navigate to returns
 *
 * Key JavaFX pattern here:
 *   cartItems is an ObservableList — the TableView is bound to it.
 *   Whenever we add/remove/update items, the table updates automatically.
 */
public class RegisterController {

    // ── Header ────────────────────────────────────────────────
    @FXML private Label    employeeLabel;
    @FXML private Label    roleLabel;
    @FXML private TextField customerSearchField;
    @FXML private Label    customerNameLabel;
    @FXML private Label    customerInfoLabel;
    @FXML private javafx.scene.layout.VBox  customerPanel;
    @FXML private Button   clearCustomerBtn;

    // ── Product search ────────────────────────────────────────
    @FXML private TextField           productSearchField;
    @FXML private ListView<ProductDto> productListView;

    // ── Cart ──────────────────────────────────────────────────
    @FXML private TableView<CartItem>   cartTable;
    @FXML private TableColumn<CartItem, String>  colProduct;
    @FXML private TableColumn<CartItem, Integer> colQty;
    @FXML private TableColumn<CartItem, String>  colPrice;
    @FXML private TableColumn<CartItem, String>  colTotal;
    @FXML private TableColumn<CartItem, Void>    colRemove;
    @FXML private Label itemCountLabel;

    // ── Totals ────────────────────────────────────────────────
    @FXML private Label subtotalLabel;
    @FXML private Label discountLabel;
    @FXML private Label taxLabel;
    @FXML private Label totalLabel;
    @FXML private Label statusLabel;
    @FXML private Label statusBarLabel;
    @FXML private Label txnLabel;
    @FXML private Button checkoutButton;
    @FXML private Button voidButton;

    // ── State ─────────────────────────────────────────────────
    private final ObservableList<CartItem> cartItems = FXCollections.observableArrayList();
    private CustomerDto selectedCustomer;
    private Long        lastTxnId;

    private final ProductService     productService     = new ProductService();
    private final CustomerService    customerService    = new CustomerService();
    private final TransactionService transactionService = new TransactionService();

    // ── Initialization ────────────────────────────────────────

    @FXML
    public void initialize() {
        // Show who is logged in
        SessionState session = SessionState.getInstance();
        employeeLabel.setText(session.getEmployeeName());
        roleLabel.setText(session.getRole());

        // Wire up cart table columns to CartItem properties
        colProduct.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colQty.setCellValueFactory(new PropertyValueFactory<>("qty"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("lineTotal"));

        // Remove button column — each row gets a × button
        colRemove.setCellFactory(col -> new TableCell<>() {
            final Button btn = new Button("×");
            {
                btn.getStyleClass().add("remove-button");
                btn.setOnAction(e -> {
                    CartItem item = getTableView().getItems().get(getIndex());
                    cartItems.remove(item);
                    updateTotals();
                });
            }
            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
            }
        });

        cartTable.setItems(cartItems);

        // Product list cell: show name, SKU, price, stock
        productListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ProductDto product, boolean empty) {
                super.updateItem(product, empty);
                if (empty || product == null) {
                    setText(null);
                } else {
                    String stock = product.isInStock()
                            ? "✓ " + product.getStockQty() + " in stock"
                            : "✗ Out of stock";
                    setText(product.getName() + " — $" + product.getPrice()
                            + "\nSKU: " + product.getSku() + "  " + stock);
                }
            }
        });

        updateTotals();
    }

    // ── Product search ────────────────────────────────────────

    @FXML
    private void handleProductSearch() {
        String term = productSearchField.getText().trim();
        if (term.isEmpty()) return;

        setStatus("Searching...");

        Task<List<ProductDto>> task = new Task<>() {
            @Override protected List<ProductDto> call() throws Exception {
                return productService.search(term);
            }
        };
        task.setOnSucceeded(e -> {
            List<ProductDto> results = task.getValue();
            productListView.setItems(FXCollections.observableArrayList(results));
            setStatus(results.isEmpty() ? "No products found." : results.size() + " products found.");
        });
        task.setOnFailed(e -> showError(task.getException().getMessage()));
        runInBackground(task);
    }

    @FXML
    private void handleProductSelected() {
        ProductDto selected = productListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (!selected.isInStock()) {
            showError("'" + selected.getName() + "' is out of stock.");
            return;
        }

        // Check if already in cart — if so, increment qty
        for (CartItem item : cartItems) {
            if (item.getProductId().equals(selected.getProductId())) {
                item.incrementQty();
                cartTable.refresh();
                updateTotals();
                productSearchField.clear();
                productListView.getItems().clear();
                setStatus("Qty updated: " + selected.getName());
                return;
            }
        }

        // New item — add to cart
        cartItems.add(new CartItem(
                selected.getProductId(),
                selected.getSku(),
                selected.getName(),
                1,
                selected.getPrice()
        ));
        updateTotals();
        productSearchField.clear();
        productListView.getItems().clear();
        setStatus("Added: " + selected.getName());
    }

    // ── Customer lookup ───────────────────────────────────────

    @FXML
    private void handleCustomerSearch() {
        String term = customerSearchField.getText().trim();
        if (term.isEmpty()) return;

        Task<List<CustomerDto>> task = new Task<>() {
            @Override protected List<CustomerDto> call() throws Exception {
                return customerService.search(term);
            }
        };
        task.setOnSucceeded(e -> {
            List<CustomerDto> results = task.getValue();
            if (results.isEmpty()) {
                showError("No customer found.");
            } else if (results.size() == 1) {
                attachCustomer(results.get(0));
            } else {
                // Multiple results — show selection dialog
                showCustomerSelectionDialog(results);
            }
        });
        task.setOnFailed(e -> showError(task.getException().getMessage()));
        runInBackground(task);
    }

    private void attachCustomer(CustomerDto customer) {
        selectedCustomer = customer;
        customerNameLabel.setText(customer.getFullName());
        customerInfoLabel.setText(customer.getDisplayInfo());
        customerPanel.setVisible(true);
        customerPanel.setManaged(true);
        clearCustomerBtn.setVisible(true);
        clearCustomerBtn.setManaged(true);
        customerSearchField.setText(customer.getFullName());
        setStatus("Customer: " + customer.getFullName() + " attached.");
    }

    @FXML
    private void handleClearCustomer() {
        selectedCustomer = null;
        customerPanel.setVisible(false);
        customerPanel.setManaged(false);
        clearCustomerBtn.setVisible(false);
        clearCustomerBtn.setManaged(false);
        customerSearchField.clear();
        setStatus("Customer removed.");
    }

    private void showCustomerSelectionDialog(List<CustomerDto> customers) {
        ChoiceDialog<CustomerDto> dialog = new ChoiceDialog<>(customers.get(0), customers);
        dialog.setTitle("Select Customer");
        dialog.setHeaderText("Multiple customers found. Select one:");
        dialog.setContentText("Customer:");
        dialog.showAndWait().ifPresent(this::attachCustomer);
    }

    // ── Cart management ───────────────────────────────────────

    @FXML
    private void handleClearCart() {
        if (cartItems.isEmpty()) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Clear all items from the cart?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Clear Cart");
        confirm.showAndWait().filter(b -> b == ButtonType.YES)
               .ifPresent(b -> {
                   cartItems.clear();
                   updateTotals();
                   setStatus("Cart cleared.");
               });
    }

    private void updateTotals() {
        BigDecimal subtotal = cartItems.stream()
                .map(CartItem::calcLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Tax estimated at 8.25% — actual tax calculated server-side on checkout
        BigDecimal taxRate  = new BigDecimal("0.0825");
        BigDecimal tax      = subtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total    = subtotal.add(tax);

        subtotalLabel.setText("$" + subtotal.setScale(2, RoundingMode.HALF_UP));
        discountLabel.setText("-$0.00");   // actual discount applied server-side
        taxLabel.setText("$" + tax);
        totalLabel.setText("$" + total);

        int count = cartItems.stream().mapToInt(CartItem::getQty).sum();
        itemCountLabel.setText("(" + count + " item" + (count != 1 ? "s" : "") + ")");

        checkoutButton.setDisable(cartItems.isEmpty());
    }

    // ── Checkout ──────────────────────────────────────────────

    @FXML
    private void handleCheckout() {
        if (cartItems.isEmpty()) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/payment.fxml"));
            Parent root = loader.load();

            PaymentController paymentCtrl = loader.getController();
            paymentCtrl.setup(cartItems, selectedCustomer, this::onSaleCompleted);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(checkoutButton.getScene().getWindow());
            dialog.setTitle("Payment");
            dialog.setScene(new Scene(root, 520, 580));
            dialog.showAndWait();
        } catch (Exception e) {
            showError("Could not open payment screen: " + e.getMessage());
        }
    }

    /** Called back from PaymentController after a successful sale */
    public void onSaleCompleted(TransactionResponse txn) {
        lastTxnId = txn.getTxnId();
        cartItems.clear();
        selectedCustomer = null;
        handleClearCustomer();
        updateTotals();
        txnLabel.setText("Last: " + txn.getReceiptNumber() + " — $" + txn.getTotal());
        voidButton.setVisible(SessionState.getInstance().isManager());
        voidButton.setManaged(SessionState.getInstance().isManager());
        setStatus("✓ Sale complete: " + txn.getReceiptNumber());
    }

    // ── Void ──────────────────────────────────────────────────

    @FXML
    private void handleVoid() {
        if (lastTxnId == null) return;
        if (!SessionState.getInstance().isManager()) {
            showError("Manager or Admin role required to void.");
            return;
        }
        openManagerPinDialog(() -> doVoid());
    }

    private void doVoid() {
        Task<TransactionResponse> task = new Task<>() {
            @Override protected TransactionResponse call() throws Exception {
                return transactionService.voidTransaction(lastTxnId, "Voided at terminal");
            }
        };
        task.setOnSucceeded(e -> {
            setStatus("✓ Transaction voided: " + task.getValue().getReceiptNumber());
            voidButton.setVisible(false);
            voidButton.setManaged(false);
            lastTxnId = null;
            txnLabel.setText("");
        });
        task.setOnFailed(e -> showError(task.getException().getMessage()));
        runInBackground(task);
    }

    // ── Returns ───────────────────────────────────────────────

    @FXML
    private void handleOpenReturn() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/return.fxml"));
            Parent root = loader.load();
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(checkoutButton.getScene().getWindow());
            dialog.setTitle("Return / Exchange");
            dialog.setScene(new Scene(root, 860, 620));
            dialog.showAndWait();
        } catch (Exception e) {
            showError("Could not open returns screen: " + e.getMessage());
        }
    }

    // ── Manager PIN ───────────────────────────────────────────

    private void openManagerPinDialog(Runnable onVerified) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/manager_pin.fxml"));
            Parent root = loader.load();

            ManagerPinController ctrl = loader.getController();
            ctrl.setOnVerified(onVerified);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(checkoutButton.getScene().getWindow());
            dialog.setTitle("Manager Verification");
            dialog.setScene(new Scene(root, 360, 320));
            dialog.showAndWait();
        } catch (Exception e) {
            showError("Could not open PIN dialog: " + e.getMessage());
        }
    }

    // ── Logout ────────────────────────────────────────────────

    @FXML
    private void handleLogout() {
        new com.universalpos.terminal.service.AuthService().logout();
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) employeeLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 1280, 720));
            stage.setTitle("UniversalPOS — Sign In");
        } catch (Exception e) {
            showError("Logout error: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    private void setStatus(String msg) {
        statusBarLabel.setText(msg);
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
    }

    private void showError(String msg) {
        statusLabel.setText("⚠ " + msg);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
        statusBarLabel.setText("Error");
    }

    private void runInBackground(Task<?> task) {
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }
}
