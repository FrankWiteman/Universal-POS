package com.universalpos.terminal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.universalpos.terminal.api.ApiClient;
import com.universalpos.terminal.api.ApiClient.ApiException;
import com.universalpos.terminal.api.dto.CustomerDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Looks up customers — used for loyalty lookup and attaching a
 * customer to a transaction.
 */
public class CustomerService {

    private final ApiClient api = ApiClient.getInstance();

    /**
     * Search customers by name, phone, email, or loyalty card number.
     */
    public List<CustomerDto> search(String term) throws ApiException {
        JsonNode data = api.get("/customers/search?q=" +
                java.net.URLEncoder.encode(term, java.nio.charset.StandardCharsets.UTF_8));

        List<CustomerDto> results = new ArrayList<>();
        if (data.isArray()) {
            for (JsonNode node : data) {
                results.add(api.getJson().convertValue(node, CustomerDto.class));
            }
        }
        return results;
    }
}
