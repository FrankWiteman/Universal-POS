package com.universalpos.terminal.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.universalpos.terminal.api.ApiClient;
import com.universalpos.terminal.api.ApiClient.ApiException;
import com.universalpos.terminal.api.dto.TransactionResponse;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.stage.Stage;

import java.util.*;

/**
 * Return / Exchange screen controller.
 *
 * Workflow:
 *   1. Cashier enters original receipt number
 *   2. We load the transaction and display its line items
 *   3. Cashier checks which items to return, sets quantity, and reason
 *   4. Click "Process Return" → calls POST /returns
 *   5. Shows success/failure and allows closing
 */
public class ReturnController {

    @FXML private TextField   receiptField;
    @FXML private javafx.scene.layout.VBox txnPanel;
    @FXML private Label       txnInfoLabel;

    @FXML private TableView<ReturnLineItem>                          itemsTable;
    @FXML private TableColumn<ReturnLineItem, Boolean>               colReturn;
    @FXML private TableColumn<ReturnLineItem, String>                colItemName;
    @FXML private TableColumn<ReturnLineItem, Number>                colItemQty;
    @FXML private TableColumn<ReturnLineItem, Number>                colReturnQty;
    @FXML private TableColumn<ReturnLineItem, Boolean>               colRestock;

    @FXML private ComboBox<String> reasonCombo;
    @FXML private ComboBox<String> refundMethodCombo;
    @FXML private Label   errorLabel;
    @FXML private Label   successLabel;
    @FXML private Button  processButton;

    private Long originalTxnId;
    private final Map<String, Long>   reasonNameToId  = new LinkedHashMap<>();
    private final ApiClient           api             = ApiClient.getInstance();
    private final ObservableList<ReturnLineItem> lineItems = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        refundMethodCombo.setItems(FXCollections.observableArrayList(
                "CASH", "CREDIT_CARD", "DEBIT_CARD", "GIFT_CARD"));
        refundMethodCombo.getSelectionModel().selectFirst();

        // Wire table columns
        colReturn.setCellValueFactory(d -> d.getValue().selectedProperty());
        colReturn.setCellFactory(CheckBoxTableCell.forTableColumn(colReturn));
        colReturn.setEditable(true);
        itemsTable.setEditable(true);

        colItemName.setCellValueFactory(d -> d.getValue().nameProperty());
        colItemQty.setCellValueFactory(d -> d.getValue().qtyBoughtProperty());
        colReturnQty.setCellValueFactory(d -> d.getValue().qtyReturnProperty());
        colReturnQty.setCellFactory(col -> new TableCell<>() {
            final Spinner<Integer> spinner = new Spinner<>(1, 999, 1);
            { spinner.setEditable(true); spinner.setPrefWidth(75); }
            @Override protected void updateItem(Number v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                ReturnLineItem item = getTableView().getItems().get(getIndex());
                spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        1, item.getQtyBought(), item.getQtyReturn()));
                spinner.valueProperty().addListener((obs, o, n) -> item.setQtyReturn(n));
                setGraphic(spinner);
            }
        });
        colRestock.setCellValueFactory(d -> d.getValue().restockProperty());
        colRestock.setCellFactory(CheckBoxTableCell.forTableColumn(colRestock));
        colRestock.setEditable(true);

        itemsTable.setItems(lineItems);
        loadReturnReasons();
    }

    private void loadReturnReasons() {
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                JsonNode data = api.get("/returns/reasons");
                if (data.isArray()) {
                    for (JsonNode r : data) {
                        reasonNameToId.put(r.path("description").asText(),
                                           r.path("reasonId").asLong());
                    }
                }
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            reasonCombo.setItems(FXCollections.observableArrayList(
                    new ArrayList<>(reasonNameToId.keySet())));
            if (!reasonCombo.getItems().isEmpty())
                reasonCombo.getSelectionModel().selectFirst();
        });
        runBg(task);
    }

    @FXML
    private void handleLookup() {
        String receipt = receiptField.getText().trim();
        if (receipt.isEmpty()) { showError("Enter a receipt number."); return; }

        Task<JsonNode> task = new Task<>() {
            @Override protected JsonNode call() throws Exception {
                return api.get("/transactions?receiptNumber=" + receipt);
            }
        };
        task.setOnSucceeded(e -> populateTransaction(task.getValue()));
        task.setOnFailed(e -> showError("Transaction not found: " + receipt));
        runBg(task);
    }

    private void populateTransaction(JsonNode data) {
        if (data == null || data.isMissingNode()) {
            showError("Transaction not found."); return;
        }
        originalTxnId = data.path("txnId").asLong();
        String receipt = data.path("receiptNumber").asText();
        String total   = "$" + data.path("total").asText();
        txnInfoLabel.setText("Receipt: " + receipt + "   Total: " + total);

        lineItems.clear();
        JsonNode items = data.path("items");
        if (items.isArray()) {
            for (JsonNode item : items) {
                lineItems.add(new ReturnLineItem(
                        item.path("itemId").asLong(),
                        item.path("productName").asText(),
                        item.path("qty").asInt()
                ));
            }
        }
        txnPanel.setVisible(true);
        txnPanel.setManaged(true);
        processButton.setVisible(true);
        processButton.setManaged(true);
        hideError();
    }

    @FXML
    private void handleProcessReturn() {
        List<ReturnLineItem> selected = lineItems.stream()
                .filter(ReturnLineItem::isSelected).toList();

        if (selected.isEmpty()) { showError("Select at least one item to return."); return; }

        String reasonName = reasonCombo.getSelectionModel().getSelectedItem();
        Long reasonId = reasonName != null ? reasonNameToId.get(reasonName) : null;
        String method = refundMethodCombo.getSelectionModel().getSelectedItem();

        List<Map<String, Object>> returnItems = new ArrayList<>();
        for (ReturnLineItem li : selected) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("originalItemId", li.getItemId());
            line.put("qtyToReturn", li.getQtyReturn());
            line.put("restock", li.isRestock());
            if (reasonId != null) line.put("reasonId", reasonId);
            returnItems.add(line);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("originalTxnId", originalTxnId);
        body.put("returnItems", returnItems);
        body.put("refundPaymentMethod", method);

        processButton.setDisable(true);
        Task<JsonNode> task = new Task<>() {
            @Override protected JsonNode call() throws Exception {
                return api.post("/returns", body);
            }
        };
        task.setOnSucceeded(e -> {
            processButton.setDisable(false);
            JsonNode resp = task.getValue();
            showSuccess("✓ Return processed: " + resp.path("receiptNumber").asText()
                    + "  Refund: $" + resp.path("total").asText().replace("-",""));
            processButton.setVisible(false);
            processButton.setManaged(false);
        });
        task.setOnFailed(e -> {
            processButton.setDisable(false);
            showError(task.getException().getMessage());
        });
        runBg(task);
    }

    @FXML private void handleClose() {
        ((Stage) receiptField.getScene().getWindow()).close();
    }

    private void showError(String msg) {
        errorLabel.setText("⚠ " + msg);
        errorLabel.setVisible(true); errorLabel.setManaged(true);
        successLabel.setVisible(false); successLabel.setManaged(false);
    }
    private void showSuccess(String msg) {
        successLabel.setText(msg);
        successLabel.setVisible(true); successLabel.setManaged(true);
        errorLabel.setVisible(false); errorLabel.setManaged(false);
    }
    private void hideError() {
        errorLabel.setVisible(false); errorLabel.setManaged(false);
    }
    private void runBg(Task<?> t) {
        Thread th = new Thread(t); th.setDaemon(true); th.start();
    }

    // ── Inner model class for the return items table ──────────
    public static class ReturnLineItem {
        private final Long                  itemId;
        private final SimpleStringProperty  name;
        private final int                   qtyBought;
        private       int                   qtyReturn;
        private final SimpleBooleanProperty selected = new SimpleBooleanProperty(true);
        private final SimpleBooleanProperty restock  = new SimpleBooleanProperty(true);

        public ReturnLineItem(Long itemId, String name, int qtyBought) {
            this.itemId    = itemId;
            this.name      = new SimpleStringProperty(name);
            this.qtyBought = qtyBought;
            this.qtyReturn = qtyBought;
        }
        public Long    getItemId()    { return itemId; }
        public int     getQtyBought() { return qtyBought; }
        public int     getQtyReturn() { return qtyReturn; }
        public void    setQtyReturn(int v) { qtyReturn = v; }
        public boolean isSelected()   { return selected.get(); }
        public boolean isRestock()    { return restock.get(); }
        public SimpleStringProperty  nameProperty()      { return name; }
        public SimpleIntegerProperty qtyBoughtProperty() { return new SimpleIntegerProperty(qtyBought); }
        public SimpleIntegerProperty qtyReturnProperty() { return new SimpleIntegerProperty(qtyReturn); }
        public SimpleBooleanProperty selectedProperty()  { return selected; }
        public SimpleBooleanProperty restockProperty()   { return restock; }
    }
}
