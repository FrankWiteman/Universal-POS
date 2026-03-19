-- ============================================================
--  UniversalPOS — V2 Seed Data
--  Creates a demo tenant + admin employee for first run
-- ============================================================

-- Demo Tenant: "UniversalPOS Demo Store"
INSERT INTO TENANTS (TENANT_ID, COMPANY_NAME, TENANT_SLUG, BRAND_COLOR,
                     RECEIPT_HEADER, RECEIPT_FOOTER, TAX_RATE, CURRENCY_CODE, TIMEZONE)
VALUES (TENANT_SEQ.NEXTVAL, 'UniversalPOS Demo Store', 'demo-store', '#1A1A2E',
        'Thank you for shopping with us!',
        'Returns accepted within 30 days with receipt.',
        0.0825, 'USD', 'America/Chicago');

-- Default Admin Employee
-- Password: ChangeMe123!  (BCrypt hash below)
-- CHANGE THIS IMMEDIATELY after first login
INSERT INTO EMPLOYEES (EMPLOYEE_ID, TENANT_ID, FIRST_NAME, LAST_NAME,
                        EMAIL, PASSWORD_HASH, ROLE, EMPLOYEE_NUMBER)
VALUES (EMPLOYEE_SEQ.NEXTVAL,
        (SELECT TENANT_ID FROM TENANTS WHERE TENANT_SLUG = 'demo-store'),
        'Admin', 'User',
        'admin@universalpos.local',
        '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/lewFHMz.R9d.Gk7dS',
        'ADMIN', 'EMP-001');

-- Sample Discount Rules
INSERT INTO DISCOUNTS (DISCOUNT_ID, TENANT_ID, NAME, DESCRIPTION,
                        DISCOUNT_TYPE, VALUE, LOYALTY_TIER_REQUIRED, ACTIVE)
VALUES (DISCOUNT_SEQ.NEXTVAL,
        (SELECT TENANT_ID FROM TENANTS WHERE TENANT_SLUG = 'demo-store'),
        'Silver Member 5% Off', '5% discount for Silver tier loyalty members',
        'LOYALTY_TIER', 5.00, 'SILVER', 1);

INSERT INTO DISCOUNTS (DISCOUNT_ID, TENANT_ID, NAME, DESCRIPTION,
                        DISCOUNT_TYPE, VALUE, LOYALTY_TIER_REQUIRED, ACTIVE)
VALUES (DISCOUNT_SEQ.NEXTVAL,
        (SELECT TENANT_ID FROM TENANTS WHERE TENANT_SLUG = 'demo-store'),
        'Gold Member 10% Off', '10% discount for Gold tier loyalty members',
        'LOYALTY_TIER', 10.00, 'GOLD', 1);

INSERT INTO DISCOUNTS (DISCOUNT_ID, TENANT_ID, NAME, DESCRIPTION,
                        DISCOUNT_TYPE, VALUE, LOYALTY_TIER_REQUIRED, ACTIVE)
VALUES (DISCOUNT_SEQ.NEXTVAL,
        (SELECT TENANT_ID FROM TENANTS WHERE TENANT_SLUG = 'demo-store'),
        'Platinum Member 15% Off', '15% discount for Platinum tier loyalty members',
        'LOYALTY_TIER', 15.00, 'PLATINUM', 1);

INSERT INTO DISCOUNTS (DISCOUNT_ID, TENANT_ID, NAME, DESCRIPTION,
                        DISCOUNT_TYPE, VALUE, MIN_PURCHASE, ACTIVE)
VALUES (DISCOUNT_SEQ.NEXTVAL,
        (SELECT TENANT_ID FROM TENANTS WHERE TENANT_SLUG = 'demo-store'),
        '$10 Off Orders Over $100', 'Fixed $10 off when cart total exceeds $100',
        'FIXED_AMOUNT', 10.00, 100.00, 1);

COMMIT;
