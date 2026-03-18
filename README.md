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

## ✨ What's Built So Far (Phase 1 Complete)

| Feature | Done? | What it means in plain English |
|---|---|---|
| Employee login | ✅ | Staff log in with email + password; system knows their role |
| Multi-store support | ✅ | One system can run Guitar Center AND Best Buy independently |
| Customer lookup | ✅ | Search a customer by phone, email, name, or loyalty card |
| Customer creation | ✅ | Add new customers and automatically give them a loyalty card |
| Loyalty tiers | ✅ | Bronze → Silver → Gold → Platinum based on points earned |
| Discount Engine | ✅ | Automatically finds the best deal the customer qualifies for |
| Product search | ✅ | Find any product by name, SKU, barcode, or brand |
| Process a sale | ✅ | Full checkout: scan items → apply discount → calc tax → save |
| Cash change calc | ✅ | Customer pays $60 on a $47 order → system calculates $13 change |
| Void a transaction | ✅ | Managers can cancel a sale and reverse everything |
| Print receipt (PDF) | ✅ | Generates a real PDF receipt like a thermal printer would print |
| Email receipt | ✅ | Sends a branded HTML email receipt to the customer |
| Audit trail | ✅ | Every action is permanently logged — who did what and when |
| Returns / exchanges | 🔜 | Coming in Phase 2 |
| Manager reports | 🔜 | Coming in Phase 3 |
| Register screen (UI) | 🔜 | The actual visual cash register — coming in Phase 4 |

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

## 📋 What You Need to Install First

Before you can run this project, you need 4 things on your computer. Here's exactly what to do for each one.

---

### 🟠 1. Java JDK 21

**What it is:** Java is the programming language. The JDK (Java Development Kit) is the toolbox you need to compile and run Java code.

**How to install:**
1. Go to https://adoptium.net
2. Download **Temurin 21 (LTS)** for your operating system
3. Run the installer — click Next → Next → Finish
4. Verify it worked by opening a terminal (see below) and typing:

```bash
java -version
```

You should see something like:
```
openjdk version "21.0.2" 2024-01-16
```

If you see a version number starting with 21, you're good. If you get `command not found`, the JDK isn't on your PATH — restart your terminal and try again, or restart your computer.

---

### 🟠 2. Apache Maven 3.9+

**What it is:** Maven is a build tool. It downloads all the libraries your project needs and compiles your Java code. Think of it like npm for Java.

**How to install on Mac (using Homebrew):**
```bash
# First install Homebrew if you don't have it:
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Then install Maven:
brew install maven
```

**How to install on Windows:**
1. Go to https://maven.apache.org/download.cgi
2. Download the **Binary zip archive** (e.g., `apache-maven-3.9.x-bin.zip`)
3. Unzip it to `C:\Program Files\Apache\maven`
4. Add `C:\Program Files\Apache\maven\bin` to your System PATH:
   - Search "Environment Variables" in Start menu
   - Click "Environment Variables"
   - Under "System variables", find `Path` → Edit → New → paste the path above
5. Open a **new** terminal window and verify:

```bash
mvn -version
```

You should see something like:
```
Apache Maven 3.9.6
```

**Common problem:** If you get `mvn: command not found` after installing, you need to open a brand new terminal window. The PATH change doesn't apply to already-open windows.

---

### 🟠 3. Docker Desktop

**What it is:** Docker lets you run software in isolated "containers." We use it to run Oracle Database and MailHog locally without having to install them directly on your computer. Think of containers as lightweight virtual machines.

**How to install:**
1. Go to https://www.docker.com/products/docker-desktop
2. Download for your OS and install it
3. Open Docker Desktop and wait for it to say "Docker Desktop is running" (green dot)
4. **Important:** Go to Settings → Resources and set Memory to at least **4096 MB (4 GB)**. Oracle needs this much RAM to start.
5. Verify in terminal:

```bash
docker --version
```

You should see: `Docker version 24.x.x` or similar.

**Common problem:** If Docker shows a whale icon that keeps spinning, it's still starting up. Wait a minute and try again. If it won't start, check that you have virtualization enabled in your BIOS (usually on by default on modern computers).

---

### 🟠 4. Git

**What it is:** Git is version control — it tracks changes to your code and lets you download this project.

**How to install:**
- **Mac:** Run `git --version` in terminal. If it's not installed, Mac will prompt you to install Xcode Command Line Tools — say yes.
- **Windows:** Download from https://git-scm.com/download/win and run the installer

Verify:
```bash
git --version
```

---

### ⭐ Your Code Editor — VS Code (Recommended for This Project)

**What it is:** VS Code (Visual Studio Code) is a free, lightweight code editor made by Microsoft. It works great for Java with the right extensions — and since you're already using it, you're all set.

**Install VS Code** (if you haven't already): https://code.visualstudio.com

#### Required Extensions for Java + Spring Boot

VS Code doesn't know Java out of the box — you need to install these extensions. Open VS Code, press `Ctrl+Shift+X` (Windows) or `Cmd+Shift+X` (Mac) to open the Extensions panel, then search for and install each one:

| Extension | Who makes it | What it does |
|---|---|---|
| **Extension Pack for Java** | Microsoft | Installs 6 Java tools in one click — syntax, debugging, Maven support |
| **Spring Boot Extension Pack** | VMware | Understands Spring Boot — shows beans, endpoints, live config |
| **Spring Boot Dashboard** | Microsoft | Adds a panel to start/stop your Spring Boot server with one click |
| **XML** | Red Hat | Highlights and validates XML files (Maven POM files are XML) |
| **YAML** | Red Hat | Highlights `application.yml` config files |

**The easiest way** — paste this into your terminal to install all of them at once:

```bash
code --install-extension vscjava.vscode-java-pack
code --install-extension vmware.vscode-boot-dev-pack
code --install-extension vscjava.vscode-spring-boot-dashboard
code --install-extension redhat.vscode-xml
code --install-extension redhat.vscode-yaml
```

After installing, **close and reopen VS Code** so it picks up all the new extensions.

#### Open the Project in VS Code

```bash
# From inside the universal-pos folder:
code .
```

The `.` means "open the current folder." VS Code will open with the entire project visible in the file explorer on the left.

The first time you open a Java project, VS Code will show a notification in the bottom right saying it's "importing Java projects" — wait for that to finish (30–60 seconds). You'll see a progress bar in the status bar at the bottom.

#### Using the Spring Boot Dashboard

Once the extensions are installed, you'll see a **Spring Boot Dashboard** icon in the left sidebar (looks like a leaf 🌿). Click it and you'll see your `pos-api` application listed. You can:

- Click the ▶️ **play button** to start the server (same as `mvn spring-boot:run`)
- Click the ⏹️ **stop button** to stop it
- See whether it's running or stopped at a glance

To pass the local profile (so emails go to MailHog), you need to configure the launch settings. Create this file in your project:

**File:** `.vscode/launch.json` (create the `.vscode` folder in your project root if it doesn't exist)

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Run POS API (local)",
      "request": "launch",
      "mainClass": "com.universalpos.PosApiApplication",
      "projectName": "pos-api",
      "args": "--spring.profiles.active=local",
      "env": {}
    }
  ]
}
```

After saving this file, press `F5` or go to **Run → Start Debugging** to launch the server. The terminal panel at the bottom of VS Code will show the Spring Boot startup logs — same output you'd see running `mvn spring-boot:run` in a terminal.

#### Using the Built-in Terminal

You don't need to open a separate terminal window. VS Code has one built in:

- Press **Ctrl+` ** (backtick — the key above Tab) to open/close it
- All the `mvn`, `docker`, `curl` commands in this guide can be run right here

#### Navigating the Code in VS Code

These shortcuts save a lot of time:

| Shortcut | What it does |
|---|---|
| `Ctrl+P` / `Cmd+P` | Open any file by typing its name |
| `Ctrl+Shift+F` / `Cmd+Shift+F` | Search across all files |
| `F12` | Jump to the definition of a class or method |
| `Shift+F12` | See everywhere a method is used |
| `Ctrl+Space` | Show autocomplete suggestions |
| `Ctrl+.` / `Cmd+.` | Quick fix (add missing import, etc.) |
| `Alt+Shift+F` / `Opt+Shift+F` | Auto-format the current file |

#### What VS Code Can't Do vs IntelliJ

VS Code is excellent for this project, but IntelliJ does have a few things VS Code doesn't:

- IntelliJ Ultimate has a built-in database browser (you'd use Oracle SQL Developer separately with VS Code)
- IntelliJ has better Spring-specific refactoring tools
- IntelliJ's debugger is slightly more powerful for complex Java debugging

For learning and building this project, VS Code with the extensions above is completely sufficient. You can always switch to IntelliJ later if you want to — the project works identically in both.

---

### 💡 Alternative: IntelliJ IDEA

If you ever want to try IntelliJ, download the free **Community Edition** from https://www.jetbrains.com/idea/download. Open the project by selecting `File → Open` and choosing the `universal-pos` folder. IntelliJ will detect Maven and import everything automatically.

---

## 🚀 Running the Project — Step by Step

### 🔵 Step 1: Open a Terminal

A terminal is a text-based window where you type commands. This is where you'll run everything.

**Easiest option — use VS Code's built-in terminal:**
Open VS Code, then press **Ctrl+` ** (the backtick key, above Tab on your keyboard). A terminal panel opens at the bottom of VS Code. You can run every command in this guide right there without switching windows.

**Or use a standalone terminal:**
- **Mac:** Press `Cmd + Space`, type "Terminal", press Enter
- **Windows:** Press `Win + R`, type `cmd`, press Enter — or install **Windows Terminal** from the Microsoft Store for a better experience

---

### 🔵 Step 2: Download the Project

```bash
# This downloads the project to your computer
git clone https://github.com/YOUR_USERNAME/universal-pos.git

# This moves you into the project folder
cd universal-pos
```

**What just happened?** `git clone` downloaded all the files. `cd universal-pos` means "change directory into the universal-pos folder." All future commands assume you are inside this folder.

---

### 🔵 Step 3: Start the Database (Oracle + MailHog)

```bash
# Move into the docker folder
cd docker

# Start both Oracle Database and MailHog
docker-compose up -d
```

**What does `-d` mean?** It means "detached" — run in the background so your terminal is free to type more commands.

**What's happening?** Docker is downloading the Oracle database image (first time only — about 2 GB) and starting it up. Oracle needs 60–90 seconds to initialize on the very first run.

Watch it until it's ready:
```bash
# This shows you Oracle's startup log. Press Ctrl+C when you want to stop watching
docker logs -f universalpos-oracle
```

Wait until you see this line in the log:
```
#########################
DATABASE IS READY TO USE!
#########################
```

Once you see that, press `Ctrl+C` to stop watching the log. The database keeps running in the background.

**Verify MailHog is running** by opening this in your browser: http://localhost:8025
You should see the MailHog inbox. This is where test emails will appear.

---

### 🔵 Step 4: Go Back to the Project Root

```bash
# Go back up one folder to the project root
cd ..
```

**Why?** You navigated into `docker/` in the previous step. All future Maven commands need to run from the root of the project where `pom.xml` lives.

Confirm you're in the right place:
```bash
# List the files in the current folder
ls
```

You should see: `pom.xml`, `pos-api/`, `pos-terminal/`, `docker/`, `README.md`, `PROJECT.md`

---

### 🔵 Step 5: Set Your JWT Secret

**What is a JWT secret?** It's a secret key the server uses to sign login tokens — like a wax seal on a letter. Without this key, tokens can't be trusted. You need to set it before running the app.

Open `pos-api/src/main/resources/application.yml` in VS Code (or any text editor) and find this line:

```yaml
jwt:
  secret: universalpos-super-secret-jwt-key-change-this-in-production-must-be-256-bits
```

For development you can leave it as-is. For anything serious, replace that value with a long random string (at least 32 characters).

---

### 🔵 Step 6: Build the Project

```bash
# This downloads all dependencies and compiles the Java code
# -DskipTests means "don't run tests during this build step"
mvn clean install -DskipTests
```

**What's happening?**
- `clean` — deletes any previously compiled files to start fresh
- `install` — downloads all the libraries listed in `pom.xml` and compiles everything
- `-DskipTests` — skips running tests for now so the build is faster

**This will take 1–3 minutes the first time** because Maven is downloading ~50 libraries from the internet. After that, they're cached locally and it's much faster.

When it succeeds you'll see:
```
[INFO] BUILD SUCCESS
```

If you see `BUILD FAILURE`, read the error message above it — it usually tells you exactly what went wrong.

---

### 🔵 Step 7: Start the API Server

```bash
# Move into the backend module
cd pos-api

# Start Spring Boot (the local profile uses MailHog for emails)
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**What's happening?**
- `spring-boot:run` — compiles and starts the Spring Boot server
- `-Dspring-boot.run.profiles=local` — activates the `local` config profile which routes emails to MailHog instead of a real email server

**Flyway will run automatically here.** It reads the SQL files in `db/migration/` and creates all the database tables, indexes, and seed data. You'll see lines like:
```
Flyway Community Edition ... will be used.
Migrating schema "UNIVERSALPOS" to version 1 - initial schema
Migrating schema "UNIVERSALPOS" to version 2 - seed data
Successfully applied 2 migrations
```

When the server is ready, you'll see:
```
Started PosApiApplication in 8.432 seconds
```

**The server is now running at:** http://localhost:8080/api

**Don't close this terminal window** — the server stops if you do. Open a new terminal window for the next steps.

---

### 🔵 Step 8: Verify Everything is Working

Open a new terminal window and run:

```bash
# Check that the server is healthy
curl http://localhost:8080/api/actuator/health
```

You should see:
```json
{"status":"UP"}
```

Or just open this URL in your browser: http://localhost:8080/api/actuator/health

---

### 🔵 Step 9: Open the API Documentation

Open this URL in your browser:
```
http://localhost:8080/api/swagger-ui.html
```

**What is Swagger UI?** It's an auto-generated visual interface that shows every API endpoint, what data it expects, and lets you test them right in the browser without writing any code. This is your best friend for exploring and testing the API.

---

### 🔵 Step 10: Your First Login

Open a new terminal and run this command to log in as the default admin:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@universalpos.local",
    "password": "ChangeMe123!",
    "tenantSlug": "demo-store"
  }'
```

**Breaking down this command:**
- `curl` — a command-line tool for making HTTP requests (like a browser, but in the terminal)
- `-X POST` — this is a POST request (sending data, not just fetching it)
- `-H "Content-Type: application/json"` — tells the server we're sending JSON
- `-d '{...}'` — the data (body) of the request

**You'll get back something like:**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "employeeName": "Admin User",
    "role": "ADMIN",
    "tenantSlug": "demo-store",
    "companyName": "UniversalPOS Demo Store"
  }
}
```

That long string starting with `eyJ...` is your **JWT token**. Copy it — you'll need it for every other request.

> ⚠️ **Change the default admin password** — the seed data creates it as `ChangeMe123!`. Once the employee management API is built in Phase 3, update it.

---

### 🔵 Step 11: Make Your First API Call

Using the token from the login response, search for customers:

```bash
# Replace YOUR_TOKEN_HERE with the actual token from Step 10
curl -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  "http://localhost:8080/api/customers/search?q=Jane"
```

**What `Authorization: Bearer <token>` means:** Every protected endpoint requires you to prove who you are. You do that by sending the token in this header. The server reads it, verifies it, and knows you're a logged-in ADMIN for the demo-store tenant.

---

### 🔵 Step 12 (Optional): Launch the JavaFX Terminal

> **Note:** The terminal UI is a stub in Phase 1 — it opens a placeholder window. The full register screen comes in Phase 4.

Open a new terminal window, go back to the project root, then:

```bash
# Go back to project root if you're in pos-api
cd ..

# Go into the terminal module
cd pos-terminal

# Launch the JavaFX window
mvn javafx:run
```

A window titled "UniversalPOS Terminal" will open. It currently shows a placeholder message.

---

## 💳 Full End-to-End Sale — Copy & Paste Commands

Here is a complete sale from login to email receipt. Open a terminal and run these one by one:

```bash
# ── STEP 1: Login and grab the token ────────────────────────
# The response will contain a "token" field — copy that value
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@universalpos.local",
    "password": "ChangeMe123!",
    "tenantSlug": "demo-store"
  }'

# ── STEP 2: Search for a product ────────────────────────────
# Replace TOKEN with your actual token from Step 1
curl -H "Authorization: Bearer TOKEN" \
  "http://localhost:8080/api/products/search?q=guitar"

# ── STEP 3: Create a test customer ──────────────────────────
curl -X POST http://localhost:8080/api/customers \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Jane",
    "lastName": "Doe",
    "email": "jane@example.com",
    "phone": "555-0100",
    "emailOptIn": true
  }'
# Note the customerId in the response — use it in Step 4

# ── STEP 4: Process a sale ───────────────────────────────────
# Replace customerId and productId with real IDs from above steps
curl -X POST http://localhost:8080/api/transactions \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "items": [
      {"productId": 1, "qty": 1}
    ],
    "paymentMethod": "CASH",
    "amountTendered": 600.00
  }'
# Note the txnId in the response — use it in Step 5

# ── STEP 5: Email the receipt ────────────────────────────────
# Replace 1 with the actual txnId from Step 4
curl -X POST http://localhost:8080/api/receipts/1/email \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"email": "jane@example.com"}'

# ── STEP 6: View the email in MailHog ───────────────────────
# Open this URL in your browser:
# http://localhost:8025

# ── STEP 7: Download the PDF receipt ────────────────────────
# This saves the PDF to your current folder
# Replace 1 with the actual txnId
curl -H "Authorization: Bearer TOKEN" \
  "http://localhost:8080/api/receipts/1/pdf" \
  --output receipt.pdf
```

---

## 🧪 Running the Tests

Tests verify the code works correctly. Run them from the `pos-api` folder:

```bash
# Make sure you're in pos-api
cd pos-api

# Run ALL unit tests
# (These don't need the database — they run entirely in memory)
mvn test
```

You'll see output like:
```
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Run a specific test file by name:
```bash
# Just the discount engine tests
mvn test -Dtest=DiscountEngineTest

# Just the customer service tests
mvn test -Dtest=CustomerServiceTest
```

Run tests AND generate a coverage report:
```bash
mvn verify
```

After that, open this file in your browser to see which lines of code are covered:
```
pos-api/target/site/jacoco/index.html
```

**What is test coverage?** It shows you what percentage of your code is actually exercised by tests. Higher is better — it means fewer surprises when things change.

---

## 📡 API Endpoints Reference

**Base URL:** `http://localhost:8080/api`

All endpoints except `/auth/login` require: `Authorization: Bearer YOUR_TOKEN`

### Authentication
| Method | URL | What it does |
|---|---|---|
| POST | `/auth/login` | Log in → get JWT token |

### Customers
| Method | URL | What it does |
|---|---|---|
| GET | `/customers/search?q=term` | Search by phone, email, name, or loyalty card |
| GET | `/customers/{id}` | Get one customer's full profile |
| POST | `/customers` | Create a new customer |

### Products
| Method | URL | What it does |
|---|---|---|
| GET | `/products/search?q=term` | Search catalog |
| GET | `/products/barcode/{barcode}` | Scan a barcode → get product |
| GET | `/products/{id}` | Get one product |
| POST | `/products` | Add a product (Manager+ only) |
| PUT | `/products/{id}` | Update a product (Manager+ only) |

### Transactions
| Method | URL | What it does |
|---|---|---|
| POST | `/transactions` | Process a sale |
| POST | `/transactions/{id}/void` | Cancel/void a transaction (Manager+ only) |

### Receipts
| Method | URL | What it does |
|---|---|---|
| POST | `/receipts/{txnId}/email` | Email receipt to an address |
| GET | `/receipts/{txnId}/pdf` | Download PDF receipt |

---

## 🔐 Employee Roles Explained

| Role | What they can do |
|---|---|
| **CASHIER** | Look up customers, search products, process sales, email or print receipts |
| **MANAGER** | Everything a cashier can + void transactions, apply manual discounts, add/edit products |
| **ADMIN** | Everything a manager can + manage employees, configure store settings, view audit logs |

The system enforces these automatically. If a cashier tries to void a transaction, they get a `403 Forbidden` error.

---

## 🛠️ Troubleshooting — Plain English Fixes

### "docker-compose: command not found"
Docker Compose is now built into Docker Desktop as `docker compose` (no hyphen).
Try: `docker compose up -d` instead of `docker-compose up -d`

---

### Oracle won't start / container keeps restarting

**Most common cause:** Not enough memory.

Fix: Open Docker Desktop → Settings → Resources → Memory → drag to at least **4096 MB** → Apply & Restart

Then try again:
```bash
cd docker
docker-compose down
docker-compose up -d
```

---

### "ORA-12505: TNS:listener does not currently know of SID"

Oracle is still starting up. It takes 60–90 seconds on first launch.

Wait and watch the log:
```bash
docker logs -f universalpos-oracle
```

Wait for `DATABASE IS READY TO USE!` then press `Ctrl+C`.

---

### "ORA-01017: invalid username/password"

The username/password in your `application.yml` doesn't match what's in `docker-compose.yml`.

Check `docker/docker-compose.yml` — the `APP_USER` and `APP_USER_PASSWORD` values.
Check `pos-api/src/main/resources/application.yml` — the `datasource.username` and `datasource.password` values.

They must match exactly (case-sensitive).

---

### "Flyway migration error" when starting the server

This usually means the schema already exists in a partial state. Fix:

```bash
# Option 1: Reset everything (nuclear option — wipes all data)
cd docker
docker-compose down -v   # -v removes the volume (all DB data)
docker-compose up -d
# Then restart the server — Flyway will start fresh

# Option 2: Just repair Flyway's tracking table
cd pos-api
mvn flyway:repair
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

---

### "mvn: command not found"

Maven isn't on your PATH. Fix:

**Mac:**
```bash
# Add this to your ~/.zshrc or ~/.bash_profile
export PATH=$PATH:/opt/homebrew/opt/maven/bin

# Then reload your terminal config
source ~/.zshrc
```

**Windows:** Go back to the Maven install instructions above and make sure you added the `bin` directory to your System PATH, then open a brand new terminal window.

---

### "java: command not found" or wrong Java version

The JDK isn't on your PATH, or you have the wrong version.

Check your version:
```bash
java -version
```

If it says version 17 or 11 or something other than 21, you need to either install JDK 21 or set your `JAVA_HOME` to point to it.

**Mac (using sdkman — easiest way to manage Java versions):**
```bash
# Install sdkman
curl -s "https://get.sdkman.io" | bash

# Install Java 21
sdk install java 21.0.2-tem

# Use it
sdk use java 21.0.2-tem
```

---

### Email receipts not appearing

1. Confirm the server started with the local profile (you should see `local` in the startup logs)
2. Open MailHog at http://localhost:8025 — the email should be there
3. If MailHog is empty, check that MailHog is actually running:

```bash
docker ps
# You should see universalpos-mailhog in the list
```

If it's not there:
```bash
cd docker
docker-compose up -d mailhog
```

---

### "JWT token rejected / 401 Unauthorized"

JWT tokens expire after 24 hours. Just log in again:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@universalpos.local",
    "password": "ChangeMe123!",
    "tenantSlug": "demo-store"
  }'
```

Copy the new token from the response and use that going forward.

---

### Server starts but Swagger UI shows nothing / 404

Make sure you're using the full URL with `/api` in it:
```
http://localhost:8080/api/swagger-ui.html   ← correct
http://localhost:8080/swagger-ui.html       ← wrong (missing /api)
```

---

### VS Code shows red squiggles / "Cannot resolve symbol" on imports

VS Code hasn't finished importing the project yet, or needs to refresh its view of the dependencies. Here's how to fix it:

**Fix 1 — Wait for the import to finish:**
Look at the bottom status bar in VS Code. If it shows a spinning icon or says "Importing Java projects...", wait 30–60 seconds. The red errors usually disappear automatically once the import completes.

**Fix 2 — Clean and restart the Java language server:**
Press `Ctrl+Shift+P` (Windows) or `Cmd+Shift+P` (Mac) to open the Command Palette.
Type `Java: Clean Java Language Server Workspace` and press Enter.
Click "Restart and Delete" when prompted. VS Code will restart and re-import — wait about 60 seconds.

**Fix 3 — Force Maven to re-download all dependencies:**
Open VS Code's built-in terminal (Ctrl+backtick) and run:
```bash
mvn clean install -DskipTests
```
After it finishes (should say BUILD SUCCESS), go back to Fix 2.

**Fix 4 — Make sure VS Code is using JDK 21:**
Press `Ctrl+Shift+P` → type `Java: Configure Java Runtime` → confirm it shows JDK 21.
If it shows an older version (17, 11, etc.), click the gear icon to change it or download JDK 21.

---

### IntelliJ says "Cannot resolve symbol" on imports

IntelliJ needs to download the dependencies. Right-click `pom.xml` → Maven → Reload Project. Wait for the sync to finish (progress bar in the bottom right).

---

## 🏢 Adding a New Store/Company

Each company using UniversalPOS is called a **tenant**. The system comes with one demo tenant (`demo-store`). Adding more tenants will be a Phase 3 feature with a full admin UI. For now, you can insert one directly via SQL in Oracle SQL Developer:

```sql
INSERT INTO TENANTS (TENANT_ID, COMPANY_NAME, TENANT_SLUG, TAX_RATE)
VALUES (TENANT_SEQ.NEXTVAL, 'My Guitar Shop', 'my-guitar-shop', 0.0825);
COMMIT;
```

Every customer, product, and transaction is completely isolated between tenants.

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
