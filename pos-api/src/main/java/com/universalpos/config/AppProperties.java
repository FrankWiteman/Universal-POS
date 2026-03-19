package com.universalpos.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe binding of application.yml custom properties.
 *
 * Accessed via injection:
 *   @Autowired AppProperties appProperties;
 */
@Data
@ConfigurationProperties(prefix = "universalpos")
public class AppProperties {

    private Receipt receipt = new Receipt();
    private double defaultTaxRate = 0.0825;
    private double maxDiscountPercent = 50.0;

    @Data
    public static class Receipt {
        private String pdfOutputDir = "./receipts";
    }
}
