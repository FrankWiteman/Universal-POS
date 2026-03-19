package com.universalpos.terminal.model;

import javafx.beans.property.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * One line item in the shopping cart.
 *
 * Why JavaFX Properties (SimpleStringProperty, etc.)?
 * JavaFX TableView can "bind" to these properties — meaning when you
 * change the qty on a CartItem, the table row automatically updates
 * on screen without you needing to refresh anything manually.
 * This is the JavaFX data-binding pattern.
 */
public class CartItem {

    private final Long           productId;
    private final String         sku;
    private final SimpleStringProperty  productName;
    private final SimpleIntegerProperty qty;
    private final BigDecimal            unitPrice;
    private final SimpleStringProperty  unitPriceDisplay;
    private final SimpleStringProperty  lineTotalDisplay;
    private String discountLabel = "";

    public CartItem(Long productId, String sku, String productName,
                    int qty, BigDecimal unitPrice) {
        this.productId        = productId;
        this.sku              = sku;
        this.productName      = new SimpleStringProperty(productName);
        this.qty              = new SimpleIntegerProperty(qty);
        this.unitPrice        = unitPrice;
        this.unitPriceDisplay = new SimpleStringProperty(formatMoney(unitPrice));
        this.lineTotalDisplay = new SimpleStringProperty(formatMoney(calcLineTotal()));
    }

    // ── qty helpers ──────────────────────────────────────────

    public void incrementQty() {
        qty.set(qty.get() + 1);
        refreshLineTotal();
    }

    public void decrementQty() {
        if (qty.get() > 1) {
            qty.set(qty.get() - 1);
            refreshLineTotal();
        }
    }

    public void setQty(int newQty) {
        qty.set(Math.max(1, newQty));
        refreshLineTotal();
    }

    private void refreshLineTotal() {
        lineTotalDisplay.set(formatMoney(calcLineTotal()));
    }

    public BigDecimal calcLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(qty.get()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    // ── JavaFX Property accessors (needed by TableView) ──────

    public StringProperty  productNameProperty()  { return productName; }
    public IntegerProperty qtyProperty()          { return qty; }
    public StringProperty  unitPriceProperty()    { return unitPriceDisplay; }
    public StringProperty  lineTotalProperty()    { return lineTotalDisplay; }

    // ── Plain getters ─────────────────────────────────────────

    public Long       getProductId()   { return productId; }
    public String     getSku()         { return sku; }
    public String     getProductName() { return productName.get(); }
    public int        getQty()         { return qty.get(); }
    public BigDecimal getUnitPrice()   { return unitPrice; }
    public String     getDiscountLabel() { return discountLabel; }
    public void       setDiscountLabel(String label) { this.discountLabel = label; }

    private String formatMoney(BigDecimal amount) {
        return "$" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
