-- ============================================================
--  UniversalPOS — V5 Returns & Exchanges Schema
--  Oracle 21c / Flyway Migration
--
--  Adds:
--    RETURN_REASONS    — configurable reason codes per tenant
--    RETURN_ITEMS      — tracks which items were returned
--                        (returns link back to the original
--                         TRANSACTION via ORIGINAL_TXN_ID)
--
--  Note: The TRANSACTIONS table already has:
--    TXN_TYPE  = RETURN | EXCHANGE | SALE | VOID
--    STATUS    = REFUNDED
--    ORIGINAL_TXN_ID — FK back to the original sale
--  So returns ARE transactions — we extend, not replace.
-- ============================================================

-- ── RETURN REASONS ────────────────────────────────────────────
-- Configurable reason codes shown to cashiers at return time
CREATE TABLE RETURN_REASONS (
    REASON_ID       NUMBER          NOT NULL,
    TENANT_ID       NUMBER          NOT NULL,
    CODE            VARCHAR2(30)    NOT NULL,
    DESCRIPTION     VARCHAR2(200)   NOT NULL,
    REQUIRES_MANAGER NUMBER(1)      DEFAULT 0 NOT NULL,
    ACTIVE          NUMBER(1)       DEFAULT 1 NOT NULL,
    SORT_ORDER      NUMBER(5)       DEFAULT 0,
    CONSTRAINT PK_RETURN_REASONS    PRIMARY KEY (REASON_ID),
    CONSTRAINT FK_RR_TENANT         FOREIGN KEY (TENANT_ID) REFERENCES TENANTS(TENANT_ID),
    CONSTRAINT UQ_RR_CODE_TENANT    UNIQUE (CODE, TENANT_ID),
    CONSTRAINT CK_RR_MANAGER        CHECK (REQUIRES_MANAGER IN (0, 1)),
    CONSTRAINT CK_RR_ACTIVE         CHECK (ACTIVE IN (0, 1))
);

-- ── RETURN ITEMS ──────────────────────────────────────────────
-- Tracks exactly which line items were returned and how many.
-- One row per product per return transaction.
CREATE TABLE RETURN_ITEMS (
    RETURN_ITEM_ID      NUMBER          NOT NULL,
    RETURN_TXN_ID       NUMBER          NOT NULL,
    ORIGINAL_TXN_ID     NUMBER          NOT NULL,
    ORIGINAL_ITEM_ID    NUMBER          NOT NULL,
    PRODUCT_ID          NUMBER          NOT NULL,
    QTY_RETURNED        NUMBER(10)      NOT NULL,
    UNIT_PRICE          NUMBER(10,2)    NOT NULL,
    REFUND_AMOUNT       NUMBER(10,2)    NOT NULL,
    REASON_ID           NUMBER,
    RESTOCK             NUMBER(1)       DEFAULT 1 NOT NULL,
    CONSTRAINT PK_RETURN_ITEMS      PRIMARY KEY (RETURN_ITEM_ID),
    CONSTRAINT FK_RI_RETURN_TXN     FOREIGN KEY (RETURN_TXN_ID)    REFERENCES TRANSACTIONS(TXN_ID),
    CONSTRAINT FK_RI_ORIG_TXN       FOREIGN KEY (ORIGINAL_TXN_ID)  REFERENCES TRANSACTIONS(TXN_ID),
    CONSTRAINT FK_RI_ORIG_ITEM      FOREIGN KEY (ORIGINAL_ITEM_ID) REFERENCES TRANSACTION_ITEMS(ITEM_ID),
    CONSTRAINT FK_RI_PRODUCT        FOREIGN KEY (PRODUCT_ID)       REFERENCES PRODUCTS(PRODUCT_ID),
    CONSTRAINT FK_RI_REASON         FOREIGN KEY (REASON_ID)        REFERENCES RETURN_REASONS(REASON_ID),
    CONSTRAINT CK_RI_QTY            CHECK (QTY_RETURNED > 0),
    CONSTRAINT CK_RI_RESTOCK        CHECK (RESTOCK IN (0, 1))
);

-- ── SEQUENCES ─────────────────────────────────────────────────
CREATE SEQUENCE RETURN_REASON_SEQ   START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE RETURN_ITEM_SEQ     START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- ── INDEXES ───────────────────────────────────────────────────
CREATE INDEX IDX_RI_RETURN_TXN  ON RETURN_ITEMS (RETURN_TXN_ID);
CREATE INDEX IDX_RI_ORIG_TXN    ON RETURN_ITEMS (ORIGINAL_TXN_ID);
CREATE INDEX IDX_RI_PRODUCT     ON RETURN_ITEMS (PRODUCT_ID);
CREATE INDEX IDX_RR_TENANT      ON RETURN_REASONS (TENANT_ID, SORT_ORDER);

-- ── SEED DEFAULT RETURN REASONS ───────────────────────────────
-- These run as part of V5, not V2, so they don't affect V2 checksum
-- Each tenant gets these seeded; tenants can add their own via API

-- Seed default return reason codes for the demo tenant
-- New tenants added via API will need to add their own, or we can
-- add a tenant-creation hook in Phase 3
INSERT INTO RETURN_REASONS (REASON_ID, TENANT_ID, CODE, DESCRIPTION, REQUIRES_MANAGER, SORT_ORDER)
VALUES (RETURN_REASON_SEQ.NEXTVAL,
        (SELECT TENANT_ID FROM TENANTS WHERE TENANT_SLUG = 'demo-store'),
        'DEFECTIVE', 'Item was defective or not working', 0, 1);

INSERT INTO RETURN_REASONS (REASON_ID, TENANT_ID, CODE, DESCRIPTION, REQUIRES_MANAGER, SORT_ORDER)
VALUES (RETURN_REASON_SEQ.NEXTVAL,
        (SELECT TENANT_ID FROM TENANTS WHERE TENANT_SLUG = 'demo-store'),
        'WRONG_ITEM', 'Wrong item received', 0, 2);

INSERT INTO RETURN_REASONS (REASON_ID, TENANT_ID, CODE, DESCRIPTION, REQUIRES_MANAGER, SORT_ORDER)
VALUES (RETURN_REASON_SEQ.NEXTVAL,
        (SELECT TENANT_ID FROM TENANTS WHERE TENANT_SLUG = 'demo-store'),
        'CHANGED_MIND', 'Customer changed their mind', 0, 3);

INSERT INTO RETURN_REASONS (REASON_ID, TENANT_ID, CODE, DESCRIPTION, REQUIRES_MANAGER, SORT_ORDER)
VALUES (RETURN_REASON_SEQ.NEXTVAL,
        (SELECT TENANT_ID FROM TENANTS WHERE TENANT_SLUG = 'demo-store'),
        'DAMAGED_SHIPPING', 'Item was damaged during shipping', 0, 4);

INSERT INTO RETURN_REASONS (REASON_ID, TENANT_ID, CODE, DESCRIPTION, REQUIRES_MANAGER, SORT_ORDER)
VALUES (RETURN_REASON_SEQ.NEXTVAL,
        (SELECT TENANT_ID FROM TENANTS WHERE TENANT_SLUG = 'demo-store'),
        'NOT_AS_DESCRIBED', 'Item did not match description', 0, 5);

INSERT INTO RETURN_REASONS (REASON_ID, TENANT_ID, CODE, DESCRIPTION, REQUIRES_MANAGER, SORT_ORDER)
VALUES (RETURN_REASON_SEQ.NEXTVAL,
        (SELECT TENANT_ID FROM TENANTS WHERE TENANT_SLUG = 'demo-store'),
        'NO_RECEIPT', 'No receipt — manager approval required', 1, 6);

INSERT INTO RETURN_REASONS (REASON_ID, TENANT_ID, CODE, DESCRIPTION, REQUIRES_MANAGER, SORT_ORDER)
VALUES (RETURN_REASON_SEQ.NEXTVAL,
        (SELECT TENANT_ID FROM TENANTS WHERE TENANT_SLUG = 'demo-store'),
        'DUPLICATE_ORDER', 'Duplicate or accidental purchase', 0, 7);

COMMIT;
