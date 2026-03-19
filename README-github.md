# 🏪 UniversalPOS

An enterprise-grade, multi-tenant Point-of-Sale system built in Java — inspired by Oracle Retail Xstore, the platform powering Guitar Center and thousands of major retailers.

Any company can deploy this, configure it for their brand, and run a full store operation from it.

---

## ✨ Features

| Feature | Status |
|---|---|
| Employee login + JWT auth | ✅ |
| Multi-store / multi-tenant isolation | ✅ |
| Customer lookup (phone, email, loyalty card, name) | ✅ |
| Loyalty tier system (Bronze → Silver → Gold → Platinum) | ✅ |
| Discount Engine (percent, fixed, tier-based, coupon codes) | ✅ |
| Product search + barcode lookup | ✅ |
| Full sales transaction processing | ✅ |
| Cash change calculation | ✅ |
| Transaction void (Manager+) | ✅ |
| PDF receipt generation | ✅ |
| Email receipt (branded HTML) | ✅ |
| Async audit trail | ✅ |
| Returns & exchanges | 🔜 Phase 2 |
| Manager reporting dashboard | 🔜 Phase 3 |
| JavaFX register terminal UI | 🔜 Phase 4 |

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

## ⚠️ Java Version Requirement

**Use Java 21 LTS exactly.** Java 22, 23, 24, and 25 all break Lombok due to internal compiler changes in newer JDKs. Java 21 is supported until 2031 and is the standard for enterprise Java.

Download: https://adoptium.net/temurin/releases/?version=21

---

## 🏗️ Architecture

```
JavaFX Terminal  →  Spring Boot REST API  →  Oracle Database
                         ↓
              Discount Engine + Receipt Service
                         ↓
                  SMTP Email / PDF Print
```

**Multi-tenant:** One running instance powers multiple stores. Every table is scoped by `tenant_id` — stores never see each other's data.

---

## 🚀 Quick Start

**Prerequisites:** Java 21 LTS, Maven 3.9+, Docker Desktop (4 GB RAM minimum)

```bash
# 1. Start Oracle database + MailHog (local email catcher)
cd docker && docker-compose up -d

# 2. Wait for Oracle (~90 seconds first time)
docker logs -f universalpos-oracle
# Wait for: DATABASE IS READY TO USE!

# 3. Build
cd .. && mvn clean install -DskipTests

# 4. Run
cd pos-api && mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Server: `http://localhost:8080/api`

Swagger UI: `http://localhost:8080/api/swagger-ui.html`

**Test login:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@universalpos.local","password":"ChangeMe123!","tenantSlug":"demo-store"}'
```

---

## 📁 Project Structure

```
universal-pos/
├── pos-api/          Spring Boot REST API (main backend)
├── pos-terminal/     JavaFX desktop register UI (Phase 4)
├── docker/           Docker Compose + Oracle init scripts
├── PROJECT.md        Full architecture, schema, and decision log
└── README.md         This file
```

---

## 📡 API Overview

| Endpoint | Description |
|---|---|
| `POST /auth/login` | Employee login → JWT token |
| `GET /customers/search?q=` | Search customers |
| `POST /customers` | Create customer |
| `GET /products/search?q=` | Search products |
| `GET /products/barcode/{barcode}` | Barcode scanner lookup |
| `POST /transactions` | Process a sale |
| `POST /transactions/{id}/void` | Void transaction (Manager+) |
| `POST /receipts/{id}/email` | Email receipt |
| `GET /receipts/{id}/pdf` | Download PDF receipt |

---

## 🗺️ Roadmap

- [x] Phase 1 — Full backend foundation (auth, customers, products, discounts, transactions, receipts)
- [ ] Phase 2 — Returns & exchanges
- [ ] Phase 3 — Reporting + admin dashboard + tenant management
- [ ] Phase 4 — JavaFX register terminal UI
- [ ] Phase 5 — Integration tests + production hardening
- [ ] Future — Hardware barcode scanner, credit card terminal (Stripe/PAX), offline mode

---

## 📄 License

MIT — free to use, modify, and deploy.