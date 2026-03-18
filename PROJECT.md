# PROJECT.md — UniversalPOS Technical Reference
> Last Updated: 2026-03-17 | Version: 0.2.0 — Phase 1 Complete

This document is the technical brain dump — architecture decisions, schema details, why things were built the way they were, and commands for every task. The README.md is for getting started. This file is for going deep.

---

## 📌 What We Are Building

An enterprise Point-of-Sale system in Java, modeled after Oracle Retail Xstore POS (the real platform Guitar Center uses). The goal is a fully working, multi-tenant POS engine that any business can deploy and configure — not a tutorial toy, but code structured the way a real company would build it.

**Multi-tenant** means one running instance of this software can power multiple different stores simultaneously. "Guitar Center #042" and "Main Street Music" are both "tenants" — they share the same database tables but their data is completely isolated from each other via a `tenant_id` column on every table.

---

## 🧱 Full Tech Stack

### Backend

| Layer | Technology | Version | Decision Rationale |
|---|---|---|---|
| Language | Java | 21 (LTS) | Oracle Xstore is Java; strong OOP model for a complex domain |
| Framework | Spring Boot | 3.2.x | Industry standard; brings DI, security, JPA, email, REST in one |
| Build Tool | Maven | 3.9+ | Standard in enterprise Java; multi-module support |
| Database | Oracle 21c XE | 21.3 | Same DB as Guitar Center; free Express Edition for dev |
| ORM | Hibernate 6 / Spring Data JPA | 6.x | Maps Java objects to Oracle tables; handles SQL generation |
| DB Migrations | Flyway | 10.x | All schema changes tracked in version-controlled SQL files |
| Security | Spring Security + JWT | jjwt 0.12.x | Stateless auth; tokens carry identity without server-side sessions |
| Password hashing | BCrypt | strength 12 | ~250ms per hash — slow enough to resist brute force |
| Email | Jakarta Mail + Spring Mail | Latest | Java's standard SMTP library |
| Email templates | Thymeleaf | 3.x | Server-side HTML templates for styled email receipts |
| PDF generation | Apache PDFBox | 3.x | Pure Java PDF creation; no external dependencies |
| API docs | SpringDoc OpenAPI | 2.x | Auto-generates Swagger UI from controller annotations |
| Logging | SLF4J + Logback | Latest | Structured logging with file rotation |
| Testing | JUnit 5 + Mockito + AssertJ | Latest | Unit and integration testing |

### Frontend (Phase 4)

| Layer | Technology | Rationale |
|---|---|---|
| Desktop UI | JavaFX 21 | Real POS terminals are desktop apps, not browser tabs |
| HTTP client | OkHttp 4.x | Makes REST calls from the JavaFX terminal to the Spring Boot API |
| JSON | Jackson | Deserializes API responses into Java objects |

### Infrastructure

| Tool | Purpose | Command to use |
|---|---|---|
| VS Code | Primary code editor (what you're using) | `code .` opens the project |
| Extension Pack for Java | Adds Java support to VS Code | `code --install-extension vscjava.vscode-java-pack` |
| Spring Boot Extension Pack | Adds Spring Boot support to VS Code | `code --install-extension vmware.vscode-boot-dev-pack` |
| Docker Desktop | Runs Oracle and MailHog in containers | Install from docker.com |
| Docker Compose | Starts all containers with one command | `docker-compose up -d` |
| MailHog | Catches emails locally for testing | View at http://localhost:8025 |
| Oracle SQL Developer | Visual DB browser for inspecting tables | Optional; download from oracle.com |
| Postman | GUI for testing API endpoints | Optional; useful for complex requests |

---

## 🏗️ Architecture — How the Pieces Connect

```
┌──────────────────────────────────────────────────────────────────┐
│                   POS TERMINAL (JavaFX)  — Phase 4              │
│                                                                  │
│  [ Login Screen ] → [ Product Search ] → [ Cart ] → [ Payment ] │
│              ↕ HTTP requests via OkHttp + Jackson                │
└──────────────────────────┬───────────────────────────────────────┘
                           │
                           │  REST/HTTP (JSON)
                           │  Authorization: Bearer <JWT>
                           │
┌──────────────────────────▼───────────────────────────────────────┐
│               SPRING BOOT API SERVER  (:8080/api)                │
│                                                                  │
│  JwtAuthenticationFilter  →  SecurityConfig  →  Controllers      │
│                                                                  │
│  AuthController       CustomerController     ProductController   │
│  TransactionController  ReceiptController                        │
│           ↓                   ↓                   ↓              │
│  AuthService      CustomerService       TransactionService       │
│  ReceiptService   AuditService (async)  DiscountEngine           │
│           ↓                   ↓                   ↓              │
│  Repositories (Spring Data JPA — query translation)             │
└──────────────────────────┬───────────────────────────────────────┘
                           │
                           │  JDBC via Hibernate 6 / Oracle dialect
                           │
┌──────────────────────────▼───────────────────────────────────────┐
│                  ORACLE DATABASE 21c (:1521)                     │
│                                                                  │
│  TENANTS  EMPLOYEES  CUSTOMERS  PRODUCTS  DISCOUNTS              │
│  TRANSACTIONS  TRANSACTION_ITEMS  RECEIPTS  AUDIT_LOG            │
└──────────────────────────┬───────────────────────────────────────┘
                           │
               ┌───────────┴──────────────┐
               │                          │
      ┌────────▼────────┐       ┌─────────▼──────────┐
      │  MailHog / SMTP │       │  PDFBox Receipt Gen │
      │  Email receipts │       │  PDF → download     │
      └─────────────────┘       └────────────────────┘
```

**Data flow for a sale (what happens when a cashier hits "Complete Sale"):**

1. JavaFX terminal sends `POST /api/transactions` with cart items, customer ID, payment method
2. `JwtAuthenticationFilter` reads the token → populates `PosUserPrincipal` (who + which store)
3. `TransactionController` receives the request → calls `TransactionService`
4. `TransactionService` validates stock for every item
5. `DiscountEngine.evaluate()` runs all active discount rules → returns best discount
6. Tax is calculated on the taxable portion after discount
7. Transaction + line items written to Oracle in a single `@Transactional` commit
8. Inventory decremented for each product
9. Loyalty points added to customer; tier recalculated
10. `AuditService.log()` fires asynchronously (doesn't delay the response)
11. Response with full transaction details returned to terminal
12. Terminal shows receipt prompt → cashier chooses email / print / both

---

## 📁 Complete File Map

```
universal-pos/
│
├── pom.xml                              # Parent POM — defines modules, shared Java version
├── .gitignore                           # Excludes build output, secrets, IDE files from Git
├── README.md                            # Getting started guide (non-technical friendly)
├── PROJECT.md                           # This file — technical deep dive
│
├── pos-api/                             # Spring Boot module — the backend server
│   ├── pom.xml                          # API module dependencies (Spring, Oracle, JWT, PDFBox...)
│   └── src/
│       ├── main/
│       │   ├── java/com/universalpos/
│       │   │   │
│       │   │   ├── PosApiApplication.java       # Entry point — @SpringBootApplication
│       │   │   │
│       │   │   ├── config/
│       │   │   │   ├── SecurityConfig.java      # JWT filter chain, CORS, role rules
│       │   │   │   ├── AsyncConfig.java         # Thread pool for @Async audit writes
│       │   │   │   └── AppProperties.java       # Type-safe config binding (@ConfigurationProperties)
│       │   │   │
│       │   │   ├── domain/                      # JPA entities — map to Oracle tables
│       │   │   │   ├── Tenant.java              # A company using the POS system
│       │   │   │   ├── Employee.java            # A staff member; has role enum
│       │   │   │   ├── Customer.java            # Loyalty member; has LoyaltyTier enum + fromPoints()
│       │   │   │   ├── Product.java             # Item in the catalog; BigDecimal price
│       │   │   │   ├── Discount.java            # A discount rule; has DiscountType enum
│       │   │   │   ├── Transaction.java         # A completed sale or void; has Status + Type enums
│       │   │   │   ├── TransactionItem.java     # One line on a transaction (product + qty + price snapshot)
│       │   │   │   ├── Receipt.java             # Tracks email/print delivery for a transaction
│       │   │   │   └── AuditLog.java            # Append-only action log; never updated or deleted
│       │   │   │
│       │   │   ├── dto/
│       │   │   │   ├── request/
│       │   │   │   │   ├── LoginRequest.java            # email + password + tenantSlug
│       │   │   │   │   ├── CreateCustomerRequest.java   # firstName, lastName, email, phone...
│       │   │   │   │   └── CreateTransactionRequest.java # items[], paymentMethod, customerId?...
│       │   │   │   └── response/
│       │   │   │       ├── ApiResponse.java     # Standard wrapper: {success, message, data, timestamp}
│       │   │   │       ├── LoginResponse.java   # token, employeeName, role, tenantId, expiresInMs
│       │   │   │       ├── CustomerResponse.java # Full customer + loyalty info + nextTier calculation
│       │   │   │       └── TransactionResponse.java # Full transaction + line items + totals
│       │   │   │
│       │   │   ├── engine/
│       │   │   │   └── DiscountEngine.java      # Core: evaluates rules → picks best → returns DiscountResult record
│       │   │   │
│       │   │   ├── exception/
│       │   │   │   ├── GlobalExceptionHandler.java      # @RestControllerAdvice — catches all exceptions
│       │   │   │   ├── ResourceNotFoundException.java   # Thrown when a record isn't found (→ 404)
│       │   │   │   └── BusinessException.java           # Business rule violations (→ 422)
│       │   │   │
│       │   │   ├── repository/                  # Spring Data JPA — one interface per table
│       │   │   │   ├── TenantRepository.java
│       │   │   │   ├── EmployeeRepository.java
│       │   │   │   ├── CustomerRepository.java  # includes searchCustomers() JPQL
│       │   │   │   ├── ProductRepository.java   # includes searchProducts() JPQL + barcode lookup
│       │   │   │   ├── DiscountRepository.java  # includes findValidDiscounts() JPQL
│       │   │   │   ├── TransactionRepository.java # includes sales totals aggregate queries
│       │   │   │   └── AuditLogRepository.java
│       │   │   │
│       │   │   ├── security/
│       │   │   │   ├── JwtTokenProvider.java           # generateToken() + validateToken() + parse claims
│       │   │   │   ├── JwtAuthenticationFilter.java    # OncePerRequestFilter — runs on every request
│       │   │   │   └── PosUserPrincipal.java           # Carries employeeId + tenantId + role in SecurityContext
│       │   │   │
│       │   │   └── service/
│       │   │       ├── AuthService.java         # login() → validate pw → lastLoginAt → generateToken → audit
│       │   │       ├── CustomerService.java     # search(), create(), addLoyaltyPoints()
│       │   │       ├── TransactionService.java  # createSale() 10-step pipeline; voidTransaction()
│       │   │       ├── ReceiptService.java      # generatePdf() via PDFBox; emailReceipt() via Thymeleaf+SMTP
│       │   │       └── AuditService.java        # @Async + REQUIRES_NEW; audit never fails main flow
│       │   │
│       │   └── resources/
│       │       ├── application.yml              # Main config (uses env vars for secrets)
│       │       ├── application-local.yml        # Local overrides: MailHog SMTP, SQL logging
│       │       ├── db/migration/
│       │       │   ├── V1__initial_schema.sql   # Creates all 9 tables, sequences, indexes, constraints
│       │       │   └── V2__seed_data.sql        # Demo tenant + admin + 4 discount rules
│       │       └── templates/email/
│       │           └── receipt.html             # Thymeleaf HTML email template
│       │
│       └── test/java/com/universalpos/
│           ├── engine/
│           │   └── DiscountEngineTest.java      # 11 test cases — no Spring context needed
│           └── service/
│               └── CustomerServiceTest.java     # 4 test cases — Mockito stubs for repositories
│
├── pos-terminal/                        # JavaFX module — the register UI (Phase 4 stub)
│   ├── pom.xml
│   └── src/main/java/com/universalpos/terminal/
│       └── MainApp.java                 # Launches a placeholder JavaFX window
│
└── docker/
    ├── docker-compose.yml               # Oracle XE + MailHog
    └── oracle-init/
        └── 01_grants.sql                # Runs on Oracle first boot — sets tablespace quota
```

---

## 🗄️ Database Schema — Every Table Explained

### TENANTS
The root of multi-tenancy. Every other table has a `TENANT_ID` foreign key.

```sql
TENANT_ID       NUMBER          PK, from TENANT_SEQ
COMPANY_NAME    VARCHAR2(100)   "Guitar Center"
TENANT_SLUG     VARCHAR2(50)    UNIQUE — "guitar-center" — used in login and API paths
LOGO_URL        VARCHAR2(500)   Optional branding
BRAND_COLOR     VARCHAR2(7)     Hex color, e.g. "#C8102E"
RECEIPT_HEADER  VARCHAR2(200)   Text printed at top of receipts
RECEIPT_FOOTER  VARCHAR2(200)   Text printed at bottom of receipts
TAX_RATE        NUMBER(5,4)     Default 0.0825 = 8.25%; configurable per tenant
CURRENCY_CODE   VARCHAR2(3)     "USD"
TIMEZONE        VARCHAR2(50)    "America/Chicago"
ACTIVE          NUMBER(1)       1=active, 0=deactivated; CHECK constraint enforces this
```

### EMPLOYEES
Staff who operate the POS terminal.

```sql
EMPLOYEE_ID     NUMBER          PK
TENANT_ID       NUMBER          FK → TENANTS
EMAIL           VARCHAR2(150)   UNIQUE per tenant (same email can exist in different tenants)
PASSWORD_HASH   VARCHAR2(255)   BCrypt hash — NEVER store plaintext
ROLE            VARCHAR2(20)    CHECK: 'CASHIER' | 'MANAGER' | 'ADMIN'
EMPLOYEE_NUMBER VARCHAR2(20)    Printed on receipts, e.g. "EMP-001"
ACTIVE          NUMBER(1)
LAST_LOGIN_AT   TIMESTAMP       Updated on every successful login
```

### CUSTOMERS
People who buy things. Optional — walk-in customers can transact without an account.

```sql
CUSTOMER_ID         NUMBER
TENANT_ID           NUMBER          FK → TENANTS
EMAIL               VARCHAR2(150)   UNIQUE per tenant
PHONE               VARCHAR2(20)    Indexed for fast lookup
LOYALTY_CARD_NUMBER VARCHAR2(20)    UNIQUE globally; auto-generated on create
LOYALTY_TIER        VARCHAR2(20)    CHECK: NONE|BRONZE|SILVER|GOLD|PLATINUM
LOYALTY_POINTS      NUMBER(10)      CHECK >= 0; recalculated after every sale
DATE_OF_BIRTH       DATE            Optional; could be used for birthday discounts
EMAIL_OPT_IN        NUMBER(1)       Whether they want marketing emails
```

**Loyalty Tier Thresholds:**
| Tier | Points Needed |
|---|---|
| NONE | 0 |
| BRONZE | 500 |
| SILVER | 1,500 |
| GOLD | 5,000 |
| PLATINUM | 10,000 |

Points earned: 1 point per dollar spent (rounded down).

### PRODUCTS
The item catalog.

```sql
PRODUCT_ID      NUMBER
TENANT_ID       NUMBER          FK → TENANTS
SKU             VARCHAR2(50)    UNIQUE per tenant — "GC-GUITAR-001"
NAME            VARCHAR2(150)   "Fender Stratocaster Standard"
PRICE           NUMBER(10,2)    BigDecimal in Java — always 2 decimal places
COST            NUMBER(10,2)    Optional — used for margin reporting
BARCODE         VARCHAR2(50)    Indexed — used for scanner lookup
STOCK_QTY       NUMBER(10)      Decremented on sale; triggers low-stock check
REORDER_POINT   NUMBER(10)      Default 5 — alert threshold
TAXABLE         NUMBER(1)       Whether this item contributes to tax calculation
ACTIVE          NUMBER(1)
```

### DISCOUNTS
Rules that the Discount Engine evaluates.

```sql
DISCOUNT_ID         NUMBER
TENANT_ID           NUMBER
NAME                VARCHAR2(100)   "Gold Member 10% Off"
DISCOUNT_TYPE       VARCHAR2(30)    CHECK: PERCENT|FIXED_AMOUNT|LOYALTY_TIER|COUPON_CODE|EMPLOYEE
VALUE               NUMBER(10,2)    For PERCENT: 0-100. For FIXED_AMOUNT: dollar value
MIN_PURCHASE        NUMBER(10,2)    Optional minimum cart subtotal to qualify
LOYALTY_TIER_REQUIRED VARCHAR2(20)  Optional — only customers at this tier+ qualify
COUPON_CODE         VARCHAR2(30)    Optional — customer must type this code at checkout
MAX_USES            NUMBER(10)      NULL = unlimited
TIMES_USED          NUMBER(10)      Incremented when applied
START_DATE          DATE            Optional active window
END_DATE            DATE            Optional active window
REQUIRES_MANAGER    NUMBER(1)       If 1, manager must be logged in to apply
ACTIVE              NUMBER(1)
```

### TRANSACTIONS
Each completed (or voided) sale.

```sql
TXN_ID              NUMBER          PK
TENANT_ID           NUMBER          FK → TENANTS
CUSTOMER_ID         NUMBER          FK → CUSTOMERS (nullable — walk-in allowed)
EMPLOYEE_ID         NUMBER          FK → EMPLOYEES — who rang it up
RECEIPT_NUMBER      VARCHAR2(30)    UNIQUE — "TXN-20240317-0042"
SUBTOTAL            NUMBER(10,2)    Cart total before discount
DISCOUNT_AMOUNT     NUMBER(10,2)    Amount deducted by discount engine
TAX_AMOUNT          NUMBER(10,2)    Tax on taxable items after discount
TOTAL               NUMBER(10,2)    Final amount customer pays
PAYMENT_METHOD      VARCHAR2(20)    CHECK: CASH|CREDIT_CARD|DEBIT_CARD|GIFT_CARD|SPLIT
AMOUNT_TENDERED     NUMBER(10,2)    Cash transactions only — what they handed over
CHANGE_DUE          NUMBER(10,2)    Cash transactions only — what to give back
STATUS              VARCHAR2(20)    CHECK: IN_PROGRESS|COMPLETED|VOIDED|REFUNDED
TXN_TYPE            VARCHAR2(20)    CHECK: SALE|RETURN|EXCHANGE|VOID
ORIGINAL_TXN_ID     NUMBER          For voids/returns — links back to original
LOYALTY_POINTS_EARNED NUMBER(10)   Points added to customer on this transaction
COMPLETED_AT        TIMESTAMP       Set when status transitions to COMPLETED
```

### TRANSACTION_ITEMS
One row per product per transaction.

```sql
ITEM_ID             NUMBER
TXN_ID              NUMBER          FK → TRANSACTIONS
PRODUCT_ID          NUMBER          FK → PRODUCTS
QTY                 NUMBER(10)      CHECK > 0
UNIT_PRICE          NUMBER(10,2)    Price AT TIME OF SALE (snapshot — not live price)
DISCOUNT_APPLIED    NUMBER(10,2)    Pro-rated discount for this line item
LINE_TOTAL          NUMBER(10,2)    (UNIT_PRICE * QTY) - DISCOUNT_APPLIED
DISCOUNT_LABEL      VARCHAR2(100)   "Gold Member 10% Off" — printed on receipt
```

**Why price snapshot?** If a product's price changes after a sale, the historical receipt should still show what the customer actually paid.

### RECEIPTS
Tracks whether each transaction had its receipt delivered.

```sql
RECEIPT_ID      NUMBER
TXN_ID          NUMBER          FK → TRANSACTIONS (UNIQUE — one receipt per transaction)
TENANT_ID       NUMBER
EMAIL_ADDRESS   VARCHAR2(150)   Where the email was sent
EMAILED         NUMBER(1)       0 or 1
EMAILED_AT      TIMESTAMP
PRINTED         NUMBER(1)       0 or 1
PRINTED_AT      TIMESTAMP
PDF_PATH        VARCHAR2(500)   Path on disk to generated PDF
```

### AUDIT_LOG
Immutable action log. Never updated or deleted.

```sql
LOG_ID          NUMBER
TENANT_ID       NUMBER          NOT a FK — audit log survives even if tenant is deleted
EMPLOYEE_ID     NUMBER          Who did it
EMPLOYEE_NAME   VARCHAR2(120)   Snapshot of name at time of action
ACTION          VARCHAR2(50)    "LOGIN", "SALE", "VOID", "CREATE_CUSTOMER", etc.
ENTITY_TYPE     VARCHAR2(50)    "TRANSACTION", "CUSTOMER", "EMPLOYEE", etc.
ENTITY_ID       NUMBER          PK of the affected record
DETAILS         VARCHAR2(2000)  JSON or plain text with context
CREATED_AT      TIMESTAMP       DEFAULT SYSTIMESTAMP — Oracle sets this
```

---

## ⚙️ Discount Engine — How It Works

The `DiscountEngine` is a pure calculation component. It never writes to the database. It takes in the cart state and returns an immutable `DiscountResult` record.

**Evaluation flow:**

```
1. Load all ACTIVE discount rules for the tenant
   WHERE active=1 AND (start_date <= today OR start_date IS NULL)
          AND (end_date >= today OR end_date IS NULL)
          AND (times_used < max_uses OR max_uses IS NULL)

2. For each rule, call the appropriate evaluator:
   PERCENT      → check minPurchase → calculate percent of subtotal
   FIXED_AMOUNT → check minPurchase → return fixed dollar amount (capped at subtotal)
   LOYALTY_TIER → check customer exists + tier >= required tier → percent of subtotal
   COUPON_CODE  → check code matches exactly (case-insensitive) → percent or fixed
   EMPLOYEE     → skipped (applied manually by manager via override)

3. Collect all eligible DiscountResult objects

4. Select the one with the highest discountAmount (best for customer)

5. Cap at maxDiscountPercent (default 50%) of subtotal — configured in application.yml

6. Return the final DiscountResult{discountAmount, discountLabel, discountId}
```

**Why best-wins instead of stacking?** Stacking discounts is complex and prone to abuse (two 50% discounts = 75% off, etc.). Best-wins is what most retailers do. Stacking can be added per-tenant in Phase 3 as a configurable option.

---

## 🔐 Security Architecture

### JWT Token Contents
Every token carries these claims:

```
sub         → employee email
employeeId  → database PK of the employee
tenantId    → database PK of the tenant
tenantSlug  → "demo-store"
role        → "CASHIER" | "MANAGER" | "ADMIN"
iat         → issued at (Unix timestamp)
exp         → expires at (iat + 24 hours by default)
```

### How Every Request Is Secured

```
Request arrives
    ↓
JwtAuthenticationFilter.doFilterInternal()
    ↓ Extract "Bearer ..." from Authorization header
    ↓ Validate signature and expiry
    ↓ Parse claims → build PosUserPrincipal
    ↓ Set in SecurityContextHolder
    ↓
SecurityConfig authorization rules
    ↓ /auth/login → permit all
    ↓ /admin/** → ADMIN only
    ↓ /transactions/*/void → MANAGER or ADMIN
    ↓ everything else → any valid JWT
    ↓
Controller method executes
    ↓ Gets PosUserPrincipal from @AuthenticationPrincipal
    ↓ Uses principal.getTenantId() to scope ALL database queries
```

**Multi-tenant data isolation:** Every service method receives `tenantId` from the `PosUserPrincipal` and passes it to every repository query. It is structurally impossible for a logged-in user to query data from a different tenant without being detected.

### BCrypt Password Hashing
Strength 12 means 2^12 = 4,096 iterations. On modern hardware this takes ~250ms per hash check — slow enough that brute-forcing a leaked password hash would take years.

---

## 🧪 Testing Strategy

### Unit Tests (no database, no Spring context)
Run with: `cd pos-api && mvn test`

```bash
# Run all tests
mvn test

# Run one test class
mvn test -Dtest=DiscountEngineTest

# Run one specific test method
mvn test -Dtest=DiscountEngineTest#shouldApplyPercentDiscount

# Run with verbose output (see each test name as it runs)
mvn test -Dtest=DiscountEngineTest -pl pos-api
```

Tests use Mockito to stub repositories — no real database connection needed. They run in milliseconds.

**DiscountEngineTest covers:**
- Percent discount applied correctly
- Min purchase threshold blocks discount
- Fixed amount discount
- Fixed discount capped at subtotal
- Gold tier customer gets loyalty discount
- Bronze customer blocked from Gold discount
- Walk-in (null) customer blocked from loyalty discount
- Coupon code: correct code applies, wrong code does not
- Best discount selected when multiple eligible
- Hard cap at 50% applies when discount would exceed it
- No discounts available → zero result

**CustomerServiceTest covers:**
- New customer created with NONE tier and 0 points
- Duplicate email throws BusinessException
- Adding points crosses BRONZE threshold → tier upgraded
- `LoyaltyTier.fromPoints()` correct for all boundaries

### Integration Tests (Phase 5)
Will use Testcontainers to spin up a real Oracle XE database in Docker and run the full stack.

---

## 💻 Developer Commands Reference

### Setting Up VS Code for This Project

Run these once to install all required VS Code extensions:

```bash
# Install all Java + Spring Boot extensions in one shot
code --install-extension vscjava.vscode-java-pack
code --install-extension vmware.vscode-boot-dev-pack
code --install-extension vscjava.vscode-spring-boot-dashboard
code --install-extension redhat.vscode-xml
code --install-extension redhat.vscode-yaml
```

Open the project in VS Code:
```bash
# From inside the universal-pos folder
code .
```

Create the VS Code launch config so you can press F5 to start the server.
Create a file at `.vscode/launch.json` with this content:

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Run POS API (local profile)",
      "request": "launch",
      "mainClass": "com.universalpos.PosApiApplication",
      "projectName": "pos-api",
      "args": "--spring.profiles.active=local"
    }
  ]
}
```

After saving, press **F5** in VS Code to start the server — it's identical to running `mvn spring-boot:run -Dspring-boot.run.profiles=local`.

Useful VS Code keyboard shortcuts for this project:

| Shortcut (Windows) | Shortcut (Mac) | What it does |
|---|---|---|
| Ctrl+` | Ctrl+` | Open/close built-in terminal |
| Ctrl+P | Cmd+P | Open any file by name (e.g., type "TransactionService") |
| Ctrl+Shift+F | Cmd+Shift+F | Search across all files |
| F12 | F12 | Jump to the definition of a class or method |
| Shift+F12 | Shift+F12 | Find all usages of a method |
| Ctrl+. | Cmd+. | Quick fix (add missing import, implement interface, etc.) |
| Ctrl+Shift+P | Cmd+Shift+P | Command Palette — run any VS Code or Java command |
| F5 | F5 | Start the app in debug mode (uses launch.json above) |
| Shift+F5 | Shift+F5 | Stop the running app |

---

### Starting Everything From Scratch

```bash
# 1. Start the database and mail server
cd docker
docker-compose up -d

# 2. Wait for Oracle to be ready (first run only)
docker logs -f universalpos-oracle
# Press Ctrl+C once you see "DATABASE IS READY TO USE!"

# 3. Go back to project root
cd ..

# 4. Build everything
mvn clean install -DskipTests

# 5. Start the API server (local profile)
cd pos-api
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Stopping Everything

```bash
# Stop the API server
# Press Ctrl+C in the terminal where it's running

# Stop Docker containers (preserves data)
cd docker
docker-compose down

# Stop Docker containers AND wipe all database data
docker-compose down -v
```

### Restarting After a Code Change

```bash
# Stop the running server (Ctrl+C), then:
cd pos-api
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Spring Boot compiles and restarts in ~5 seconds.

### Checking What's Running

```bash
# See all running Docker containers
docker ps

# See all containers including stopped ones
docker ps -a

# Check Oracle logs
docker logs universalpos-oracle

# Check MailHog logs
docker logs universalpos-mailhog
```

### Database Operations

```bash
# Run Flyway migrations manually (usually automatic on startup)
cd pos-api
mvn flyway:migrate -Dspring-boot.run.profiles=local

# Check migration status
mvn flyway:info -Dspring-boot.run.profiles=local

# Repair Flyway (fixes checksum mismatches after editing a migration)
mvn flyway:repair -Dspring-boot.run.profiles=local

# Reset the entire database (WIPES ALL DATA)
cd docker
docker-compose down -v
docker-compose up -d
```

### Maven Build Commands

```bash
# Full clean build (compiles everything, skips tests)
mvn clean install -DskipTests

# Full clean build WITH tests
mvn clean install

# Run tests only (no compile if nothing changed)
mvn test

# Run tests with coverage report
mvn verify
# Report: pos-api/target/site/jacoco/index.html

# Compile only (no tests, no packaging)
mvn compile

# Package into a runnable JAR
mvn package -DskipTests
# JAR location: pos-api/target/pos-api-0.1.0-SNAPSHOT.jar

# Run the packaged JAR directly (alternative to mvn spring-boot:run)
java -jar pos-api/target/pos-api-0.1.0-SNAPSHOT.jar --spring.profiles.active=local
```

### Running the JavaFX Terminal

```bash
# From project root
cd pos-terminal
mvn javafx:run
```

### Useful curl Commands for Testing

```bash
# Health check
curl http://localhost:8080/api/actuator/health

# Login (get a token)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@universalpos.local","password":"ChangeMe123!","tenantSlug":"demo-store"}'

# Search customers (replace TOKEN)
curl -H "Authorization: Bearer TOKEN" \
  "http://localhost:8080/api/customers/search?q=Jane"

# Search products
curl -H "Authorization: Bearer TOKEN" \
  "http://localhost:8080/api/products/search?q=guitar"

# Create a customer
curl -X POST http://localhost:8080/api/customers \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Jane","lastName":"Doe","email":"jane@test.com","phone":"555-0100"}'

# Process a sale
curl -X POST http://localhost:8080/api/transactions \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"customerId":1,"items":[{"productId":1,"qty":1}],"paymentMethod":"CASH","amountTendered":600.00}'

# Email receipt (replace txnId)
curl -X POST http://localhost:8080/api/receipts/1/email \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"email":"jane@test.com"}'

# Download PDF receipt
curl -H "Authorization: Bearer TOKEN" \
  "http://localhost:8080/api/receipts/1/pdf" \
  --output receipt.pdf

# Void a transaction (Manager+ only)
curl -X POST http://localhost:8080/api/transactions/1/void \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"reason":"Customer changed mind"}'
```

---

## 🧠 Key Architectural Decisions — The "Why" Behind Every Choice

| Decision | What we chose | Why |
|---|---|---|
| **Money types** | `BigDecimal` everywhere | `float` and `double` have rounding errors. `0.1 + 0.2 = 0.30000000000000004` in floating point. Never acceptable for currency. |
| **Multi-tenancy model** | Shared schema + `tenant_id` column on every table | Simpler than separate database schemas per tenant. Still fully isolated via query scoping. Scales well for dozens of tenants. |
| **Terminal UI** | JavaFX desktop app | Real POS terminals (Xstore, NCR, Lightspeed) run as desktop applications installed on register hardware — not browser tabs. JavaFX matches this reality. |
| **Auth type** | Stateless JWT | No server-side session storage. Each token is self-contained. The terminal can be offline briefly and tokens still work. Scales horizontally (any API server can validate any token). |
| **Discount engine** | Pure calculation, no DB writes | The engine takes inputs and returns a result. It never touches the database. This makes it trivially testable (no Mockito needed for the calculation logic itself) and reusable. |
| **Best-wins discount** | One discount applies — the best one | Simpler than stacking. Matches what most retailers do. No abuse vectors from combined 50% + 50% discounts. |
| **Audit log propagation** | `REQUIRES_NEW` + `@Async` | `REQUIRES_NEW` means the audit entry commits in its own separate transaction — even if the main sale rolls back, the audit record of the attempt survives. `@Async` means audit writes never add latency to the checkout response. |
| **Price snapshots** | `UNIT_PRICE` on `TRANSACTION_ITEMS` | Stores the price at time of sale, not a FK to the current product price. Historical receipts must be accurate even if prices change later. |
| **BCrypt strength 12** | 2^12 = 4,096 iterations | ~250ms per hash on modern hardware. Slow enough to make brute-forcing leaked hashes impractical. Fast enough that a cashier logging in doesn't notice the delay. |
| **Flyway over Hibernate DDL** | Flyway manages schema | `hibernate.ddl-auto=validate` means Hibernate only validates — it never creates or modifies tables. Flyway owns the schema. This is what production systems do: database changes are tracked, reviewed, and applied in order. |
| **MailHog for dev email** | Local SMTP catch-all | All emails in development land in MailHog instead of going to real inboxes. Safe, visible, requires zero email server setup. |

---

## 📦 Phase Build Plan

### Phase 1 — Foundation ✅ COMPLETE
Everything needed for a basic sale cycle.

### Phase 2 — Returns & Exchanges
- Return a specific item or full transaction
- Partial returns (only some items)
- Exchange (return + new sale in one transaction)
- Return reason codes (damaged, wrong item, changed mind)
- Return eligibility check (within X days, has receipt)

### Phase 3 — Reporting & Admin API
- Daily sales summary (total revenue, transaction count, avg ticket)
- Top products by revenue and units
- Discount usage report (which discounts were used, savings given)
- Employee performance (transactions per employee)
- Tenant configuration API (update branding, tax rate, receipt text)
- Employee management API (create, update, deactivate staff)

### Phase 4 — JavaFX Terminal UI
- Login screen with tenant slug input
- Product search panel + barcode scan simulation (type barcode)
- Customer lookup panel (search + display tier/points)
- Cart view with live discount calculation as items are added
- Payment screen (cash change calculator, card/split options)
- Receipt prompt (email / print / both / skip)
- Manager PIN/login overlay for voids and overrides

### Phase 5 — Production Hardening
- Integration tests (Testcontainers + Oracle)
- Rate limiting on login endpoint (prevent brute force)
- Refresh token flow (extend sessions without re-login)
- Input sanitization audit
- Error monitoring (structured JSON logs for log aggregators)
- Docker multi-stage build for production JAR image

---

## 🔖 Version History

| Version | Date | What changed |
|---|---|---|
| 0.1.0 | 2026-03-17 | Initial architecture, tech stack decisions, schema design, build plan |
| 0.2.0 | 2026-03-17 | Phase 1 complete — full backend foundation: all entities, security, services, discount engine, receipt generation, tests |
