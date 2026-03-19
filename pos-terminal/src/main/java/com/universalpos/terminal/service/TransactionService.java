package com.universalpos.terminal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.universalpos.terminal.api.ApiClient;
import com.universalpos.terminal.api.ApiClient.ApiException;
import com.universalpos.terminal.api.dto.TransactionResponse;
import com.universalpos.terminal.model.CartItem;

import java.math.BigDecimal;
import java.util.*;

/**
 * Submits transactions to the backend.
 * Handles sale, void, and return workflows.
 */
public class TransactionService {

    private final ApiClient api = ApiClient.getInstance();

    /**
     * Submit a completed sale.
     *
     * @param cartItems     the items in the cart
     * @param customerId    optional — null for anonymous sales
     * @param paymentMethod "CASH", "CREDIT_CARD", "DEBIT_CARD", "GIFT_CARD"
     * @param amountTendered cash given by customer (for cash sales)
     */
    public TransactionResponse submitSale(List<CartItem> cartItems,
                                           Long customerId,
                                           String paymentMethod,
                                           BigDecimal amountTendered)
            throws ApiException {

        // Build the items list matching backend CreateTransactionRequest format
        List<Map<String, Object>> items = new ArrayList<>();
        for (CartItem item : cartItems) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("productId", item.getProductId());
            line.put("qty", item.getQty());
            items.add(line);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", items);
        body.put("paymentMethod", paymentMethod);
        if (customerId != null) body.put("customerId", customerId);
        if (amountTendered != null) body.put("amountTendered", amountTendered);

        JsonNode data = api.post("/transactions", body);
        return api.getJson().convertValue(data, TransactionResponse.class);
    }

    /**
     * Void a completed transaction (Manager+ only).
     */
    public TransactionResponse voidTransaction(Long txnId, String reason)
            throws ApiException {
        Map<String, String> body = Map.of("reason", reason);
        JsonNode data = api.post("/transactions/" + txnId + "/void", body);
        return api.getJson().convertValue(data, TransactionResponse.class);
    }

    /**
     * Look up a transaction by ID (to show after a sale completes).
     */
    public TransactionResponse getById(Long txnId) throws ApiException {
        JsonNode data = api.get("/transactions/" + txnId);
        return api.getJson().convertValue(data, TransactionResponse.class);
    }
}
