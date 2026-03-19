# 🏪 UniversalPOS
### An Enterprise-Grade, Multi-Tenant Point-of-Sale System in Java

> Inspired by Oracle Retail Xstore POS — the platform powering Guitar Center and thousands of retailers worldwide.
> Built so **any company** can use it — configure it once, and it runs your store.

---

## 📖 What Is This? (Plain English)

Think of this project as building the software that runs a cash register — but a really powerful one, the kind you'd find at Guitar Center, Best Buy, or a clothing chain.

When a cashier rings you up, a lot happens behind the scenes:
- The system looks up your loyalty account and checks if you're a Gold member
- It scans every item in your cart and knows the price from the database
- It automatically finds the best discount you qualify for
- It calculates tax based on the store's location
- It saves the transaction, prints or emails your receipt, and adds loyalty points to your account

This project builds all of that, in Java, using the same technologies that real enterprise POS systems use.

**Who is this for?**
- Developers learning Java who want a real, meaty project
- Anyone who wants to understand how enterprise software is structured
- Developers who want to build a POS system they can actually deploy

---

## 🧰 Tech Stack (What We're Using and Why)

| What | Tool | Why we chose it |
|---|---|---|
| Programming language | Java 21 | Same language Oracle Xstore POS uses; great for large systems |
| Web framework | Spring Boot 3.2 | Makes building REST APIs fast; industry standard |
| Database | Oracle 21c | Same DB Guitar Center uses; free "Express Edition" for dev |
| Database migration | Flyway | Keeps the database structure in version control |
| Security | Spring Security + JWT | Token-based login that works for POS terminals |
| Desktop terminal UI | JavaFX 21 | Builds the actual register screen (coming Phase 4) |
| PDF receipts | Apache PDFBox | Java library that creates PDF files |
| Email receipts | Jakarta Mail | Java's standard email-sending library |
| Email templates | Thymeleaf | Lets us write HTML email templates with variables |
| Build tool | Maven | Downloads dependencies and builds the project |
| Containers | Docker | Runs Oracle database locally without a full install |
| Local email testing | MailHog | Catches emails locally so we can see them without a real email server |
| Testing | JUnit 5 + Mockito | Runs automated tests to verify the code works |

---

## 🗂️ Project Layout (What Each Folder Does)

```
universal-pos/               ← Root of the whole project
│
├── pos-api/                 ← The backend server (brain of the system)
│   └── src/
│       ├── main/java/       ← All the Java source code
│       │   ├── domain/      ← Database models (Customer, Product, Transaction...)
│       │   ├── service/     ← Business logic (how a sale actually works)
│       │   ├── controller/  ← API endpoints (the "doors" the terminal talks through)
│       │   ├── engine/      ← Discount calculator
│       │   ├── security/    ← Login and JWT token handling
│       │   └── repository/  ← Database queries
│       └── resources/
│           ├── application.yml       ← Configuration file (DB connection, email, etc.)
│           ├── db/migration/         ← SQL files that set up the database tables
│           └── templates/email/      ← The HTML email receipt template
│
├── pos-terminal/            ← The JavaFX desktop register UI (Phase 4)
│
└── docker/
    └── docker-compose.yml   ← Starts Oracle database + MailHog with one command
```

---

## 📝 Changelog

All notable changes are documented here.

---

### [0.2.0] — 2026-03-17 — Phase 1: Full Backend Foundation Built

**What was added:**

All 9 database tables created in Oracle (Tenants, Employees, Customers, Products, Discounts, Transactions, Transaction Items, Receipts, Audit Log) with proper Oracle sequences, constraints, and performance indexes.

Employee login system built — BCrypt password hashing, JWT token generation, role-based access control (Cashier / Manager / Admin).

Customer management — search by phone, email, name, or loyalty card; create new customers; automatic loyalty card number generation; loyalty point tracking with automatic tier upgrades (NONE → BRONZE → SILVER → GOLD → PLATINUM).

Discount Engine — evaluates every active discount rule for a tenant and selects the best one the customer qualifies for. Supports percent-off, fixed-amount, loyalty-tier, and coupon-code discounts. Hardcapped at 50% of subtotal.

Transaction processing — full sale pipeline: validates stock → calculates subtotal → runs discount engine → calculates tax proportionally → saves everything to Oracle → decrements inventory → awards loyalty points → writes audit log.

Transaction void — managers can cancel a completed transaction; inventory and loyalty points are reversed automatically.

PDF receipt generation — thermal-receipt style narrow PDF using Apache PDFBox with company branding, line items, totals, and loyalty points summary.

Email receipt — branded HTML email via Jakarta Mail and Thymeleaf template engine. Uses MailHog in local development.

Async audit logging — every action is permanently logged. Runs in a background thread so it never slows down the checkout response.

Global error handling — all errors return a consistent `{success, message, data}` shape.

Docker setup — Oracle XE and MailHog run with a single `docker-compose up -d` command.

15 unit tests covering the Discount Engine (11 cases) and Customer Service (4 cases).

**Key technical decisions made:**
- `BigDecimal` is used for all money — float/double are banned because they cause rounding errors with currency
- Discount engine is pure calculation and never touches the database itself
- Audit logs use a separate transaction so they save even if the main transaction fails
- All data queries are automatically scoped to the current tenant so stores can never see each other's data

---

### [0.1.0] — 2026-03-17 — Initial Architecture

Project structure created. Tech stack selected. Architecture designed. Database schema planned. Build phases mapped out.

---

## 🗺️ What's Coming Next

- [x] Phase 1: Full backend — login, customers, products, discounts, sales, receipts
- [ ] Phase 2: Returns and exchanges
- [ ] Phase 3: Reporting dashboard + tenant management API
- [ ] Phase 4: JavaFX terminal UI — the actual visual cash register screen
- [ ] Phase 5: Integration tests + production hardening
- [ ] Future: Physical barcode scanner integration
- [ ] Future: Credit card terminal (Stripe Terminal or PAX device SDK)
- [ ] Future: Offline mode (keeps working if the network drops)

---

## 📄 License

MIT License — free to use, modify, and deploy for personal or commercial use.
