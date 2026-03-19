package com.universalpos.terminal.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.math.BigDecimal;

/** Subset of the backend Product entity we need in the terminal */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductDto {
    private Long       productId;
    private String     sku;
    private String     name;
    private String     brand;
    private String     category;
    private BigDecimal price;
    private Integer    stockQty;
    private String     barcode;
    private Boolean    active;

    public boolean isInStock() {
        return stockQty != null && stockQty > 0;
    }
}
