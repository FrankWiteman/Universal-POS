package com.universalpos.service;

import com.universalpos.domain.Receipt;
import com.universalpos.domain.Transaction;
import com.universalpos.domain.TransactionItem;
import com.universalpos.exception.BusinessException;
import com.universalpos.exception.ResourceNotFoundException;
import com.universalpos.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReceiptService {

    private final TransactionRepository transactionRepository;
    private final JavaMailSender        mailSender;
    private final TemplateEngine        templateEngine;

    @Value("${universalpos.receipt.pdf-output-dir:./receipts}")
    private String pdfOutputDir;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // ── Email Receipt ────────────────────────────────────────────

    @Transactional
    public void emailReceipt(Long txnId, Long tenantId, String emailAddress) {
        Transaction transaction = findTransaction(txnId, tenantId);

        try {
            // Build Thymeleaf context
            Context ctx = new Context();
            ctx.setVariable("txn",           transaction);
            ctx.setVariable("tenant",         transaction.getTenant());
            ctx.setVariable("items",          transaction.getItems());
            ctx.setVariable("receiptNumber",  transaction.getReceiptNumber());
            ctx.setVariable("dateFormatted",
                    transaction.getCompletedAt().format(
                            DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a")));

            // Render HTML template
            String htmlBody = templateEngine.process("email/receipt", ctx);

            // Send email
            var message = mailSender.createMimeMessage();
            var helper  = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(emailAddress);
            helper.setSubject("Your receipt from " + transaction.getTenant().getCompanyName()
                              + " — " + transaction.getReceiptNumber());
            helper.setText(htmlBody, true);

            mailSender.send(message);

            // Update receipt record
            Receipt receipt = getOrCreateReceipt(transaction);
            receipt.setEmailed(true);
            receipt.setEmailAddress(emailAddress);
            receipt.setEmailedAt(java.time.LocalDateTime.now());

            log.info("Receipt emailed: {} → {}", transaction.getReceiptNumber(), emailAddress);

        } catch (Exception e) {
            log.error("Failed to email receipt {}: {}", transaction.getReceiptNumber(), e.getMessage());
            throw new BusinessException("Failed to send email receipt. Please try printing instead.");
        }
    }

    // ── PDF Receipt ──────────────────────────────────────────────

    @Transactional
    public byte[] generatePdf(Long txnId, Long tenantId) {
        Transaction txn = findTransaction(txnId, tenantId);

        try (PDDocument doc = new PDDocument()) {

            // Thermal-receipt-style narrow page (3.25" wide)
            float pageWidth  = 234f;  // ~3.25 inches at 72dpi
            float pageHeight = 700f;  // tall enough for typical receipt
            PDPage page = new PDPage(new PDRectangle(pageWidth, pageHeight));
            doc.addPage(page);

            PDFont fontBold    = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float margin = 10f;
                float y      = pageHeight - 20f;
                float w      = pageWidth - (margin * 2);

                // ── Header ────────────────────────────────────────
                y = drawCenteredText(cs, fontBold, 10, txn.getTenant().getCompanyName(), pageWidth, y);
                y -= 4;

                if (txn.getTenant().getReceiptHeader() != null) {
                    y = drawCenteredText(cs, fontRegular, 7,
                            txn.getTenant().getReceiptHeader(), pageWidth, y);
                }

                y -= 4;
                y = drawLine(cs, margin, y, w);
                y -= 4;

                // ── Transaction info ──────────────────────────────
                y = drawKeyValue(cs, fontRegular, fontRegular, 7, margin, y, w,
                        "Receipt:", txn.getReceiptNumber());
                y = drawKeyValue(cs, fontRegular, fontRegular, 7, margin, y, w,
                        "Date:", txn.getCompletedAt() != null
                                ? txn.getCompletedAt().format(
                                    DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a"))
                                : "");
                y = drawKeyValue(cs, fontRegular, fontRegular, 7, margin, y, w,
                        "Cashier:", txn.getEmployee().getFullName());

                if (txn.getCustomer() != null) {
                    y = drawKeyValue(cs, fontRegular, fontRegular, 7, margin, y, w,
                            "Customer:", txn.getCustomer().getFullName());
                }

                y -= 4;
                y = drawLine(cs, margin, y, w);
                y -= 4;

                // ── Line items ────────────────────────────────────
                for (TransactionItem item : txn.getItems()) {
                    y = drawText(cs, fontBold, 7, margin, y, item.getProduct().getName());
                    y = drawKeyValue(cs, fontRegular, fontRegular, 7, margin, y, w,
                            "  " + item.getQty() + " x $" +
                            item.getUnitPrice().toPlainString(),
                            "$" + item.getLineTotal().toPlainString());
                    if (item.getDiscountApplied().compareTo(BigDecimal.ZERO) > 0) {
                        y = drawKeyValue(cs, fontRegular, fontRegular, 7, margin, y, w,
                                "  Discount (" + item.getDiscountLabel() + ")",
                                "-$" + item.getDiscountApplied().toPlainString());
                    }
                }

                y -= 4;
                y = drawLine(cs, margin, y, w);
                y -= 4;

                // ── Totals ────────────────────────────────────────
                y = drawKeyValue(cs, fontRegular, fontRegular, 7, margin, y, w,
                        "Subtotal:", "$" + txn.getSubtotal().toPlainString());

                if (txn.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                    y = drawKeyValue(cs, fontRegular, fontRegular, 7, margin, y, w,
                            "Discount:", "-$" + txn.getDiscountAmount().toPlainString());
                }

                y = drawKeyValue(cs, fontRegular, fontRegular, 7, margin, y, w,
                        "Tax:", "$" + txn.getTaxAmount().toPlainString());

                y -= 2;
                y = drawKeyValue(cs, fontBold, fontBold, 9, margin, y, w,
                        "TOTAL:", "$" + txn.getTotal().toPlainString());

                y -= 4;
                y = drawKeyValue(cs, fontRegular, fontRegular, 7, margin, y, w,
                        "Payment:", txn.getPaymentMethod().name().replace("_", " "));

                if (txn.getChangeDue() != null &&
                        txn.getChangeDue().compareTo(BigDecimal.ZERO) > 0) {
                    y = drawKeyValue(cs, fontRegular, fontRegular, 7, margin, y, w,
                            "Change Due:", "$" + txn.getChangeDue().toPlainString());
                }

                // ── Loyalty ───────────────────────────────────────
                if (txn.getCustomer() != null && txn.getLoyaltyPointsEarned() > 0) {
                    y -= 4;
                    y = drawLine(cs, margin, y, w);
                    y -= 4;
                    y = drawCenteredText(cs, fontRegular, 7,
                            "Points earned: +" + txn.getLoyaltyPointsEarned(), pageWidth, y);
                    y = drawCenteredText(cs, fontRegular, 7,
                            "Total points: " + txn.getCustomer().getLoyaltyPoints(), pageWidth, y);
                }

                // ── Footer ────────────────────────────────────────
                y -= 6;
                y = drawLine(cs, margin, y, w);
                y -= 4;
                if (txn.getTenant().getReceiptFooter() != null) {
                    y = drawCenteredText(cs, fontRegular, 7,
                            txn.getTenant().getReceiptFooter(), pageWidth, y);
                }
                drawCenteredText(cs, fontRegular, 6,
                        "Powered by UniversalPOS", pageWidth, y - 8);
            }

            // Write to bytes
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);

            log.info("PDF receipt generated for transaction {}", txn.getReceiptNumber());
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Failed to generate PDF for transaction {}: {}", txnId, e.getMessage());
            throw new BusinessException("Failed to generate PDF receipt.");
        }
    }

    // ── PDF drawing helpers ──────────────────────────────────────

    private float drawText(PDPageContentStream cs, PDFont font, float size,
                           float x, float y, String text) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitize(text));
        cs.endText();
        return y - (size + 2);
    }

    private float drawCenteredText(PDPageContentStream cs, PDFont font, float size,
                                   String text, float pageWidth, float y) throws IOException {
        float textWidth = font.getStringWidth(sanitize(text)) / 1000 * size;
        float x = (pageWidth - textWidth) / 2;
        return drawText(cs, font, size, x, y, text);
    }

    private float drawKeyValue(PDPageContentStream cs, PDFont keyFont, PDFont valFont,
                                float size, float x, float y, float width,
                                String key, String value) throws IOException {
        drawText(cs, keyFont, size, x, y, key);
        float valWidth = valFont.getStringWidth(sanitize(value)) / 1000 * size;
        drawText(cs, valFont, size, x + width - valWidth, y, value);
        return y - (size + 2);
    }

    private float drawLine(PDPageContentStream cs, float x, float y, float width) throws IOException {
        cs.setLineWidth(0.5f);
        cs.moveTo(x, y);
        cs.lineTo(x + width, y);
        cs.stroke();
        return y - 2;
    }

    private String sanitize(String text) {
        if (text == null) return "";
        return text.replaceAll("[^\\x20-\\x7E]", "?");
    }

    // ── Internal helpers ─────────────────────────────────────────

    private Transaction findTransaction(Long txnId, Long tenantId) {
        return transactionRepository.findById(txnId)
                .filter(t -> t.getTenant().getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", txnId));
    }

    private Receipt getOrCreateReceipt(Transaction transaction) {
        if (transaction.getReceipt() != null) return transaction.getReceipt();
        return Receipt.builder()
                .transaction(transaction)
                .tenant(transaction.getTenant())
                .build();
    }
}
