# 🏪 UniversalPOS

An enterprise-grade, multi-tenant Point-of-Sale system built in Java — inspired by Oracle Retail Xstore, the platform powering Guitar Center and thousands of major retailers.

Any company can deploy this, configure it for their brand, and run a full store operation from it.

---

## ✨ Features — Phase 1 Complete

| Feature | Status |
|---|---|
| Employee login + JWT auth | ✅ |
| Multi-store / multi-tenant isolation | ✅ |
| Tenant configuration API | ✅ |
| Employee management (create, update, password change) | ✅ |
| Customer lookup (phone, email, loyalty card, name) | ✅ |
| Loyalty tier system (Bronze → Silver → Gold → Platinum) | ✅ |
| Discount Engine (percent, fixed, tier-based, coupon codes) | ✅ |
| Discount rule management API | ✅ |
| Product catalog (create, update, search, barcode lookup) | ✅ |
| Full sales transaction processing | ✅ |
| Cash change calculation | ✅ |
| Transaction void (Manager+) | ✅ |
| Transaction lookup by ID | ✅ |
| PDF receipt generation | ✅ |
| Email receipt (branded HTML) | ✅ |
| Async audit trail | ✅ |
| Supplier management | ✅ |
| Purchase orders (create, submit, receive) | ✅ |
| Partial PO receipt support | ✅ |
| Low stock alerts | ✅ |
| Manual stock adjustments (damage, theft, correction) | ✅ |
| Physical stock count sessions with variance tracking | ✅ |
| Full return processing (partial + full) | ✅ |
| Return reason codes (configurable per store) | ✅ |
| Exchange workflow (return + new items, net billing) | ✅ |
| Inventory auto-restocked on return | ✅ |
| Loyalty points reversed on return | ✅ |
| Manager reporting dashboard | 🔜 Phase 3 |
| JavaFX register terminal UI | 🔜 Phase 4 |

---

## ⚠️ Java Version Requirement

**Use Java 21 LTS exactly.** Java 22, 23, 24, and 25 all break Lombok due to internal compiler changes. Java 21 is supported until 2031.

Download: https://adoptium.net/temurin/releases/?version=21

---

## 🧰 Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 LTS |
| Framework | Spring Boot 3.4.4 |
| Database | Oracle 21c |
| ORM | Hibernate 6 / Spring Data JPA |
| Migrations | Flyway 10 |
| Security | Spring Security + JWT |
| Terminal UI | JavaFX 21 (Phase 4) |
| PDF | Apache PDFBox 3 |
| Email | Jakarta Mail + Thymeleaf |
| Build | Maven (multi-module) |
| Containers | Docker + Docker Compose |

---

## 🚀 Quick Start

**Prerequisites:** Java 21 LTS, Maven 3.9+, Docker Desktop (4 GB RAM)

```bash
# 1. Start Oracle + MailHog
cd docker && docker-compose up -d
docker logs -f universalpos-oracle   # wait for: DATABASE IS READY TO USE!

# 2. Build and run
cd .. && mvn clean install -DskipTests
cd pos-api && mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Swagger UI: `http://localhost:8080/api/swagger-ui.html`

**Quick login (saves token to $TOKEN / %TOKEN%):**
```bash
# Mac/Linux
source scripts/login.sh

# Windows
scripts\login.bat

# Then use the token:
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/customers/search?q=Jane
```

---

## 📁 Project Structure

```
universal-pos/
├── pos-api/          Spring Boot REST API
├── pos-terminal/     JavaFX register UI (Phase 4)
├── docker/           Docker Compose + Oracle init
├── scripts/          login.sh / login.bat — JWT token helpers
├── PROJECT.md        Full architecture and decision log
└── README.md         This file
```

---

## 📡 API Overview

| Area | Endpoint | Notes |
|---|---|---|
| Auth | `POST /auth/login` | Returns JWT token |
| Tenants | `GET/PUT /tenants/current` | View/update store config |
| Employees | `GET/POST /employees` | Staff management (Admin) |
| Customers | `GET /customers/search` | Search + loyalty info |
| Products | `GET /products/search` | + barcode lookup |
| Discounts | `GET/POST /discounts` | Rule management |
| Transactions | `POST /transactions` | Process a sale |
| Transactions | `POST /transactions/{id}/void` | Manager+ |
| Receipts | `POST /receipts/{id}/email` | Email receipt |
| Receipts | `GET /receipts/{id}/pdf` | Download PDF |
| Inventory | `GET /inventory/low-stock` | Reorder alerts |
| Inventory | `POST /inventory/purchase-orders` | Create PO |
| Inventory | `POST /inventory/stock-counts/start` | Count session |
| Returns | `GET /returns/reasons` | List return reason codes |
| Returns | `POST /returns` | Process a return (refund) |
| Returns | `POST /returns/exchange` | Process an exchange |

Full interactive docs at `/api/swagger-ui.html`

---

## 🗺️ Roadmap

- [x] Phase 1 — Full backend: auth, customers, products, discounts, transactions, receipts, inventory
- [x] Phase 2 — Returns & exchanges (partial returns, exchanges, reason codes, restock, loyalty reversal)
- [ ] Phase 2 — Returns & exchanges
- [ ] Phase 3 — Reporting dashboard + admin tools
- [ ] Phase 4 — JavaFX register terminal UI
- [ ] Phase 5 — Integration tests + production hardening

---

## 📄 License

MIT — free to use, modify, and deploy.