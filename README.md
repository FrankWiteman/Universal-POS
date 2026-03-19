# 🏪 UniversalPOS

[![License: CC BY-NC-SA 4.0](https://img.shields.io/badge/License-CC%20BY--NC--SA%204.0-lightgrey.svg)](https://creativecommons.org/licenses/by-nc-sa/4.0/)

> An enterprise-grade, multi-tenant Point-of-Sale system built in Java —
> modeled after Oracle Retail Xstore, the platform powering Guitar Center
> and thousands of major retailers worldwide.

Any company can deploy this, configure it for their brand, and run a full
store operation from it — multiple locations, multiple staff roles, full
inventory, sales, returns, and reporting.

---

## 🤔 What Is This?

Most POS systems you interact with day-to-day (Guitar Center, Best Buy, Foot Locker)
run on enterprise Java backends. This project builds exactly that kind of system
from scratch — not a toy tutorial, but a real architecture with the same patterns
those systems use.

**Multi-tenant** means one running instance can power many different stores.
Every piece of data — products, customers, transactions — is isolated per company
using a `tenant_id`. Store A can never see Store B's data.

**Why Java?** Type safety, Oracle database compatibility, JavaFX for the
terminal UI, and direct alignment with real enterprise POS platforms.

---

## ✅ What's Built

| Area | Features |
|---|---|
| **Auth** | JWT login, BCrypt passwords, roles (CASHIER / MANAGER / ADMIN) |
| **Tenants** | Multi-store config — branding, tax rate, receipt text, timezone |
| **Employees** | Create, update, deactivate staff; password management |
| **Customers** | Search by name/phone/email/loyalty card; loyalty tier system |
| **Loyalty** | BRONZE → SILVER → GOLD → PLATINUM; points earned per sale |
| **Products** | Full catalog with SKU, barcode, category, cost, reorder point |
| **Discounts** | Percent, fixed amount, loyalty-tier, coupon code — engine picks best |
| **Transactions** | Full sale pipeline: stock check → discount → tax → persist → audit |
| **Void** | Manager-level void with full stock reversal |
| **Receipts** | PDF thermal-style receipt + branded HTML email receipt |
| **Inventory** | Suppliers, purchase orders (partial receipt), stock adjustments |
| **Stock Counts** | Physical count sessions with variance detection and auto-correction |
| **Returns** | Full and partial returns against original sale |
| **Exchanges** | Return items + take new ones; net billing calculated |
| **Return Reasons** | Configurable reason codes per store with manager-approval flag |
| **Reports** | Daily sales, top products, employee performance, shrinkage, low stock |
| **Audit Trail** | Every mutation permanently logged — who did what and when |

---

## ⚠️ Java Version Requirement

**You must use Java 21 LTS.** Java 22, 23, 24, and 25 all break Lombok,
the library that generates boilerplate code in this project. Java 21 is
the enterprise standard — it's what Oracle Xstore itself runs on — and is
supported until 2031.

Download Temurin 21: https://adoptium.net/temurin/releases/?version=21

If you already have a newer Java installed, you can keep it. Just tell Maven
which one to use before building:
```bash
# Windows
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.x.x-hotspot

# Mac/Linux
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
```

---

## 🧰 Tech Stack

| Layer | Technology | Why |
|---|---|---|
| Language | Java 21 LTS | Type safety, Oracle compatibility, JavaFX |
| Framework | Spring Boot 3.4.4 | Industry standard enterprise Java |
| Database | Oracle 21c | Same DB as real Xstore deployments |
| ORM | Hibernate 6 + Spring Data JPA | Entity mapping + repository pattern |
| Migrations | Flyway 10 | Schema versioning — DB changes are auditable |
| Security | Spring Security + JWT | Stateless auth, role-based access |
| Terminal UI | JavaFX 21 | Desktop register interface (Phase 4) |
| PDF Receipts | Apache PDFBox 3 | Thermal-style receipt generation |
| Email | Jakarta Mail + Thymeleaf | Branded HTML email receipts |
| Build | Maven (multi-module) | Separate modules for API and terminal |
| Containers | Docker + Docker Compose | Oracle XE + MailHog for local dev |

---

## 📁 Project Layout

```
universal-pos/
│
├── pos-api/                        ← Spring Boot REST API (the backend brain)
│   ├── src/main/java/
│   │   └── com/universalpos/
│   │       ├── controller/         ← HTTP endpoints (one per resource area)
│   │       ├── service/            ← Business logic lives here
│   │       ├── domain/             ← JPA entities (Java ↔ Oracle table mapping)
│   │       ├── repository/         ← Database queries (Spring Data)
│   │       ├── dto/                ← Request/response shapes
│   │       ├── engine/             ← Discount calculation engine
│   │       ├── security/           ← JWT filter, token provider
│   │       └── config/             ← Spring Security, async, app settings
│   └── src/main/resources/
│       ├── db/migration/           ← Flyway SQL files (V1, V2, V3 ... V5)
│       ├── templates/email/        ← Thymeleaf HTML email template
│       ├── application.yml         ← Main config (DB, Flyway, Hibernate)
│       └── application-local.yml   ← Local dev overrides (MailHog, logging)
│
├── pos-terminal/                   ← JavaFX register UI (Phase 4 — stub)
│
├── docker/
│   └── docker-compose.yml          ← Starts Oracle XE + MailHog
│
├── scripts/
│   ├── login.sh                    ← Mac/Linux: logs in and saves $TOKEN
│   └── login.bat                   ← Windows: logs in and saves %TOKEN%
│
└── .mvn/
    └── jvm.config                  ← JVM flags for Lombok on Java 21+
```

---

## 🔧 What You Need to Install

| Tool | Version | Link | Notes |
|---|---|---|---|
| Java JDK | **21 LTS exactly** | https://adoptium.net/temurin/releases/?version=21 | Not 22, 23, 24, or 25 |
| Maven | 3.9+ | https://maven.apache.org/download.cgi | Or use `mvnw` wrapper |
| Docker Desktop | Latest | https://www.docker.com/products/docker-desktop | Needs 4 GB RAM allocated |
| jq | Latest | https://jqlang.github.io/jq/download/ | Optional — used by login scripts |

No IDE required — VS Code with the Java Extension Pack works great.

---

## 🚀 Quick Start

```bash
# 1. Start Oracle database + MailHog (local email catcher)
cd docker && docker-compose up -d

# 2. Wait for Oracle to finish starting (~90 seconds first time)
docker logs -f universalpos-oracle
# Wait until you see:  DATABASE IS READY TO USE!
# Then press Ctrl+C

# 3. Build
cd .. && mvn clean install -DskipTests

# 4. Run
cd pos-api && mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Server starts at: `http://localhost:8080/api`

Swagger (interactive API docs): `http://localhost:8080/api/swagger-ui.html`

**Log in and save your token (do this once per terminal session):**
```bash
# Windows
scripts\login.bat

# Mac/Linux (must use "source")
source scripts/login.sh

# Then every curl uses the saved token:
curl -H "Authorization: Bearer %TOKEN%" http://localhost:8080/api/products/search?q=guitar
```

Default login: `admin@universalpos.local` / `ChangeMe123!` / `demo-store`

**Run the JavaFX terminal** (backend must be running first):
```bash
cd pos-terminal && mvn compile javafx:run
```

> **Change the default password** after first login. Get the admin employee ID
> from `GET /employees`, then call `POST /employees/{id}/change-password`
> with `{"newPassword": "YourNewPassword"}`.

---

## 📡 API Overview

All endpoints require `Authorization: Bearer <token>` except `/auth/login`.
Full interactive docs at `/api/swagger-ui.html`.

| Area | Key Endpoints |
|---|---|
| Auth | `POST /auth/login` |
| Tenants | `GET /PUT /tenants/current` |
| Employees | `GET/POST/PUT /employees` |
| Customers | `GET /customers/search?q=` |
| Products | `GET /products/search?q=`, `GET /products/barcode/{barcode}` |
| Discounts | `GET/POST/PUT/DELETE /discounts` |
| Transactions | `POST /transactions`, `GET /transactions/{id}`, `POST /transactions/{id}/void` |
| Receipts | `POST /receipts/{id}/email`, `GET /receipts/{id}/pdf` |
| Returns | `POST /returns`, `POST /returns/exchange`, `GET /returns/reasons` |
| Inventory | `GET /inventory/low-stock`, `POST /inventory/purchase-orders` |
| Reports | `GET /reports/daily`, `/reports/top-products`, `/reports/employee-performance` |

---

## 📦 Changelog (Summary)

| Version | What Changed |
|---|---|
| **0.1.0** | Project scaffolded — Maven multi-module, Oracle + Flyway + Spring Boot wired up |
| **0.2.0** | Phase 1 complete — full auth, customers, products, discount engine, transactions, receipts, audit trail |
| **0.3.0** | Inventory system — suppliers, purchase orders, stock counts, manual adjustments |
| **0.4.0** | Management APIs — employee, discount, tenant, product service layer; JWT token helper scripts |
| **0.5.0** | Phase 2 — returns and exchanges with partial return support, reason codes, inventory restock, loyalty reversal |
| **0.6.0** | Phase 3 — 5 reporting endpoints: daily sales, top products, employee performance, shrinkage, low stock |
| **0.7.0** | Phase 4 — JavaFX terminal UI: login, register, cart, payment, manager PIN, returns, dark theme |: daily sales, top products, employee performance, shrinkage, low stock |

---

## 🗺️ Roadmap

- [x] Phase 1 — Full backend: auth, customers, products, discounts, transactions, receipts, inventory
- [x] Phase 2 — Returns & exchanges (partial returns, exchanges, reason codes, restock, loyalty reversal)
- [x] Phase 3 — Reporting dashboard (daily sales, top products, employee performance, shrinkage, low stock)
- [x] Phase 4 — JavaFX register terminal UI (login, register, cart, payment, manager PIN, returns)
- [ ] Phase 5 — Integration tests + production hardening

---

## 📄 License

MIT — free to use, modify, and deploy.
