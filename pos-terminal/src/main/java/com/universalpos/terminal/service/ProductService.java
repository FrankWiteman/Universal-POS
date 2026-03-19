package com.universalpos.terminal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.universalpos.terminal.api.ApiClient;
import com.universalpos.terminal.api.ApiClient.ApiException;
import com.universalpos.terminal.api.dto.ProductDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Looks up products from the backend.
 * Used for both text search and barcode scan.
 */
public class ProductService {

    private final ApiClient api = ApiClient.getInstance();

    /**
     * Search products by name, SKU, barcode, or brand.
     * Returns up to 20 results.
     */
    public List<ProductDto> search(String term) throws ApiException {
        JsonNode data = api.get("/products/search?q=" +
                java.net.URLEncoder.encode(term, java.nio.charset.StandardCharsets.UTF_8)
                + "&size=20");

        List<ProductDto> results = new ArrayList<>();
        JsonNode content = data.path("content");
        if (content.isArray()) {
            for (JsonNode node : content) {
                results.add(api.getJson().convertValue(node, ProductDto.class));
            }
        }
        return results;
    }

    /**
     * Look up a product by barcode — used when a barcode scanner
     * sends input to the terminal. Returns null if not found.
     */
    public ProductDto findByBarcode(String barcode) throws ApiException {
        JsonNode data = api.get("/products/barcode/" + barcode);
        return api.getJson().convertValue(data, ProductDto.class);
    }
}
