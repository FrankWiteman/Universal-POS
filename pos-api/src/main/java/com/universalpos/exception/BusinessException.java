package com.universalpos.exception;

/**
 * Thrown when a business rule is violated.
 * Examples:
 *   - Applying a discount that has expired
 *   - Voiding a transaction that's already voided
 *   - Processing a return for more items than were sold
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
