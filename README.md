# 🏪 UniversalPOS
### An Enterprise-Grade, Multi-Tenant Point-of-Sale System in Java

> Inspired by Oracle Retail Xstore POS — the platform powering Guitar Center and thousands of retailers worldwide.
> Built so **any company** can use it — configure it once, and it runs your store.

---

## 📄 About These Docs — Three Files, Each With a Job

This project uses three documentation files. Here is what each one is for and who sees it:

| File | Who sees it | What it contains |
|---|---|---|
| `README.md` | **GitHub + anyone** | Short public overview — features, quick start, API list, brief changelog |
| `README-personal.md` | **You only** (blocked by .gitignore) | This file — full setup guide, every command explained, troubleshooting, complete changelog |
| `PROJECT.md` | **You only** (blocked by .gitignore) | Architecture diagrams, database schema, all decisions and their reasons, developer commands |

**How `.gitignore` works:** Git reads `.gitignore` before every commit. Any file listed there is invisible to Git — it exists on your computer but never gets uploaded to GitHub. `README-personal.md` and `PROJECT.md` are listed there, so they stay private automatically. You never have to think about it.

**How to update all three docs when something changes:**

When you complete a feature, fix a bug, or make any meaningful change, update all three files:

1. **`README.md`** (GitHub) — Add one line to the changelog: `- Brief description of what changed`. Keep it short. No implementation details.
2. **`README-personal.md`** (this file) — Add the full entry to the Changelog section: what was added, what decisions were made, any commands that changed.
3. **`PROJECT.md`** — Update the Phase checklist (check off completed items), add any new architectural decisions to the decisions table, update the version history row at the bottom.

That's it. Three files, three levels of detail, only one goes to GitHub.

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

> **IMPORTANT — Use Java 21 LTS exactly, not any newer version.**
> Java 22, 23, 24, and 25 all break Lombok — the library this project uses to generate code.
> Lombok support for Java 25 does not exist yet. Java 21 LTS is supported until 2031 and is
> what Oracle Xstore and every major production Java shop runs on.

**How to install:**
1. Go to **https://adoptium.net/temurin/releases/?version=21** (goes directly to Java 21)
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

If you see a version number starting with **21**, you're good.

If you already have a newer Java installed (22, 23, 24, or 25), you can keep it — just also install Java 21 alongside it. Then set `JAVA_HOME` before running Maven commands:

```bash
# Windows — run this in your terminal before any mvn command
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.7.6-hotspot

# Mac/Linux
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
```

To find the exact path after installing, run:
```bash
# Windows
dir "C:\Program Files\Eclipse Adoptium\"

# Mac
/usr/libexec/java_home -V
```

See the **Permanent JAVA_HOME Setup** section in Troubleshooting to set this once and forget it.

If you get `command not found`, restart your terminal and try again.

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

### ⭐ Your Code Editor — VS Code Setup

**What it is:** VS Code (Visual Studio Code) is a free code editor made by Microsoft. It works great for Java but needs a few extensions installed first — it doesn't understand Java out of the box.

**Install VS Code** (if you don't have it yet): https://code.visualstudio.com

---

## 🚀 Getting Started — Complete Setup From Scratch

Follow these steps in order. Every single step is explained. Don't skip any — each one builds on the previous.

---

### 🔵 Step 1: Install the VS Code Java Extensions

Before you open the project, you need to teach VS Code how to understand Java and Spring Boot. Do this once and you'll never need to do it again.

**Option A — Install via terminal (fastest):**

Open your terminal:
- **Mac:** Press `Cmd + Space`, type "Terminal", press Enter
- **Windows:** Press `Win + R`, type `cmd`, press Enter

Then paste and run these commands one by one:

```bash
code --install-extension vscjava.vscode-java-pack
code --install-extension vmware.vscode-boot-dev-pack
code --install-extension vscjava.vscode-spring-boot-dashboard
code --install-extension redhat.vscode-xml
code --install-extension redhat.vscode-yaml
```

Each one will print `Extension '...' was successfully installed.`

**Option B — Install via VS Code's Extensions panel:**

1. Open VS Code
2. Press `Ctrl+Shift+X` (Windows) or `Cmd+Shift+X` (Mac) — the Extensions panel opens on the left
3. Search for and install each of these:

| Name to search | Who makes it | What it does |
|---|---|---|
| **Extension Pack for Java** | Microsoft | Core Java support — syntax, errors, debugging, Maven |
| **Spring Boot Extension Pack** | VMware | Understands Spring Boot specifically |
| **Spring Boot Dashboard** | Microsoft | One-click start/stop for your server |
| **XML** | Red Hat | Highlights Maven `pom.xml` files |
| **YAML** | Red Hat | Highlights `application.yml` config files |

After installing, **close VS Code completely and reopen it** so all extensions activate.

---

### 🔵 Step 2: Open a Terminal

A terminal is a text window where you type commands. You'll use it for the rest of this setup.

**Easiest: use VS Code's built-in terminal** — you never have to leave VS Code:
1. Open VS Code
2. Press **Ctrl+` ** (backtick — the key above Tab, to the left of the 1 key)
3. A terminal panel opens at the bottom of VS Code

Every command in this guide can be run right there. The screenshot below shows where it appears:

```
┌─────────────────────────────────────────┐
│  VS Code - Explorer | Editor            │
│                                         │
│  [your code files appear here]          │
│                                         │
├─────────────────────────────────────────┤
│  TERMINAL  ← you are here               │
│  $ _                                    │
└─────────────────────────────────────────┘
```

**Or use a standalone terminal (also fine):**
- **Mac:** `Cmd + Space` → type "Terminal" → Enter
- **Windows:** `Win + R` → type `cmd` → Enter (or install Windows Terminal from the Microsoft Store)

---

### 🔵 Step 3: Get the Project Files onto Your Computer

You already have the project files from Claude — either downloaded as a zip or copied into a folder. This step gets everything placed correctly, sets up Git, and optionally pushes to GitHub.

---

#### 3A — Place the Project Folder

1. Find the `universal-pos` folder you downloaded from Claude
2. Move it somewhere permanent — somewhere you can find it again:
   - **Mac:** `Documents/projects/universal-pos` or your Desktop
   - **Windows:** `C:\Users\YourName\Documents\projects\universal-pos`

3. Open VS Code
4. Go to **File → Open Folder**
5. Navigate to the `universal-pos` folder and click **Open**

The Explorer panel on the left should now show this structure:
```
📁 universal-pos
  📁 docker
  📁 pos-api
  📁 pos-terminal
  📄 pom.xml
  📄 README.md
  📄 PROJECT.md
  📄 .gitignore
```

If you see that, you're in the right place. If the panel is empty or looks wrong, make sure you opened `universal-pos` itself — not a parent folder above it.

---

#### 3B — Open the Built-in Terminal

Press the backtick key while holding Ctrl — that is **Ctrl+`** (the backtick key is above Tab, to the left of the 1 key).

A terminal panel opens at the bottom of VS Code. **This terminal automatically opens inside your project folder**, which is exactly where you need to be.

Confirm it by typing:

```bash
ls
```

**Windows users** — type `dir` instead:
```bash
dir
```

You should see the project files listed:
```
docker     pos-api    pos-terminal    pom.xml    README.md    PROJECT.md
```

If you see a completely different location, navigate to your project:
```bash
# Mac/Linux — adjust the path to match where you put the folder
cd ~/Documents/projects/universal-pos

# Windows — adjust to match your path
cd C:\Users\YourName\Documents\projects\universal-pos
```

Then run `ls` (or `dir`) again to confirm.

> **Why this matters:** Every `mvn` command you run must be run from inside the `universal-pos` folder. Wrong folder = confusing errors.

---

#### 3C — Set Up Git (Version Control)

Git tracks every change you make to the code — like save states in a video game. You use it to save your progress and push your code to GitHub.

Run each of these commands in your VS Code terminal:

**One-time setup — tell Git who you are** (skip if you've done this before):
```bash
git config --global user.name "Your Name"
git config --global user.email "your-email@example.com"
```

**Initialize Git in the project folder:**
```bash
git init
```

You should see:
```
Initialized empty Git repository in .../universal-pos/.git/
```

**Stage all files — tell Git to track everything:**
```bash
git add .
```

**Create your first commit — a snapshot of the project right now:**
```bash
git commit -m "Initial commit - Phase 1 foundation"
```

You should see something like:
```
[main (root-commit) a3f8c12] Initial commit - Phase 1 foundation
 60 files changed, 4823 insertions(+)
```

Git is now tracking all your files. Going forward, every time you finish a meaningful chunk of work, save your progress with:
```bash
git add .
git commit -m "Brief description of what you changed"
```

---

#### 3D — Push to GitHub (Backup Your Code Online)

This puts your code on GitHub so it's backed up and accessible from anywhere. Optional but strongly recommended.

**Don't have a GitHub account?** Sign up free at https://github.com

**Create a new GitHub repository:**
1. Go to https://github.com/new
2. Name it `universal-pos`
3. Set it to **Private** (your code, your project)
4. **Do NOT check** "Add a README" — you already have one
5. Click **Create repository**

GitHub will show a page with commands. Copy the ones under **"push an existing repository"** and run them in your terminal. They look like this (replace `YOUR_USERNAME` with yours):

```bash
git remote add origin https://github.com/YOUR_USERNAME/universal-pos.git
git branch -M main
git push -u origin main
```

**GitHub will ask for a password — use a Personal Access Token, not your account password:**
1. Go to https://github.com/settings/tokens
2. Click **Generate new token (classic)**
3. Name it `universal-pos-token`
4. Check the **repo** box
5. Scroll down and click **Generate token**
6. Copy the token that appears — paste it as your "password" when Git prompts you

After a successful push, go to `https://github.com/YOUR_USERNAME/universal-pos` in your browser — you'll see all your files there.

**Every time you want to save changes to GitHub going forward:**
```bash
git add .
git commit -m "What you changed"
git push
```

---

### 🔵 Step 4: Wait for VS Code to Import the Java Project

The very first time VS Code opens a Java project, it automatically scans and indexes all the files. You don't have to do anything — just wait.

Look at the **bottom status bar** of VS Code (the colored bar at the very bottom of the window):

```
[ Java: Importing projects... ]   ← watch for this
```

Wait until this disappears — usually 30–60 seconds. You may also see a popup notification in the bottom-right corner saying "Java projects are being imported" — that's normal, ignore it and wait.

**Do not skip ahead.** If you run the project before VS Code finishes indexing, you'll see red error lines everywhere even though the code is perfectly fine. Once the import finishes, most red marks clear up on their own.

If errors remain after the import finishes, see the **Troubleshooting** section at the bottom of this document.

---


---

### 🔵 Step 5: Create the VS Code Launch File

This is a one-time configuration that tells VS Code how to start your Spring Boot server. Without it, VS Code won't know which profile to use (and emails won't work).

**In VS Code:**

1. Look at the Explorer panel on the left. Right-click the `universal-pos` root folder
2. Select **New Folder** and name it `.vscode` (include the dot — it's part of the name)
3. Right-click the new `.vscode` folder → **New File** → name it `launch.json`
4. Paste this exact content into the file:

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

5. Press `Ctrl+S` (Windows) or `Cmd+S` (Mac) to save the file

**What does this do?** It tells VS Code:
- Which Java class to run (`PosApiApplication` — the entry point of the app)
- Which project it lives in (`pos-api`)
- That it should start with the `local` profile (so test emails go to MailHog instead of a real inbox)

---

### 🔵 Step 6: Start Docker Desktop

Docker is what runs the Oracle database and MailHog email catcher on your machine. It needs to be running before you start the app.

1. Open **Docker Desktop** (find it in your Applications or Start Menu)
2. Wait for it to show a green status — "Docker Desktop is running"
3. If it asks you to sign in, you can skip/close that — sign-in is not required to use Docker locally

**First time only — increase Docker's memory:**
1. Click the gear icon (Settings) in Docker Desktop
2. Click **Resources** on the left
3. Drag the **Memory** slider to at least **4096 MB** (4 GB)
4. Click **Apply & Restart**

Oracle needs 4 GB to start. If you skip this step, Oracle will crash silently and you'll wonder why nothing works.

---

### 🔵 Step 7: Start the Database

In your VS Code terminal (or standalone terminal), make sure you're in the `universal-pos` folder, then:

```bash
# Move into the docker folder
cd docker

# Start Oracle database and MailHog in the background
docker-compose up -d
```

`-d` means "detached" — both services start and run in the background. Your terminal is free immediately.

**First time only, this takes a few minutes** — Docker has to download the Oracle image (~2 GB). Subsequent starts take about 10 seconds.

Now watch Oracle start up:
```bash
# Shows Oracle's startup log live. Press Ctrl+C to stop watching.
docker logs -f universalpos-oracle
```

You will see a lot of text scrolling. **Wait for this exact message:**
```
#########################
DATABASE IS READY TO USE!
#########################
```

Once you see it, press `Ctrl+C`. The database keeps running in the background — you're just stopping the log viewer, not the database.

**Verify MailHog is up:** Open http://localhost:8025 in your browser. You should see an empty inbox. This is where test emails will land during development.

---

### 🔵 Step 8: Go Back to the Project Root

```bash
cd ..
```

That `..` means "go up one folder." You moved into `docker/` in Step 7 — this brings you back to `universal-pos/`.

Double-check you're back in the right place:
```bash
ls
```

You should see `pom.xml`, `pos-api/`, `pos-terminal/`, `docker/`, etc. again.

---

### 🔵 Step 9: Build the Project (Download All Libraries)

```bash
mvn clean install -DskipTests
```

**What each word means:**
- `mvn` — runs Maven, the build tool
- `clean` — deletes any old compiled files (fresh start)
- `install` — downloads all the Java libraries the project needs, then compiles the code
- `-DskipTests` — skips running tests for now so this step is faster

**This will take 1–3 minutes the first time** — Maven is downloading about 50 libraries (~200 MB) from the internet. They get cached on your machine so every build after this is much faster (under 30 seconds).

Watch the terminal — you'll see library names scrolling as they download. When it's done you'll see:
```
[INFO] BUILD SUCCESS
[INFO] Total time: 1:42 min
```

If instead you see `[INFO] BUILD FAILURE`, scroll up in the terminal to read the error. The most common causes are:
- Java isn't installed or is the wrong version (need JDK 21, not JRE)
- Internet connection dropped mid-download — just run the command again
- Oracle isn't running — go back to Step 7

---

### 🔵 Step 10: Start the Spring Boot Server

**Option A — Using VS Code (recommended):**

1. Press `F5` in VS Code
2. VS Code will read your `launch.json` from Step 5 and start the server
3. The built-in terminal panel at the bottom will open and show startup logs

**Option B — Using the terminal:**

```bash
# Make sure you're in the pos-api folder
cd pos-api

# Start the server with the local profile
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Either way, watch the logs. You'll see Flyway automatically create your database tables:
```
Migrating schema to version 1 - initial schema   ← creates all 9 tables
Migrating schema to version 2 - seed data        ← adds demo data
Successfully applied 2 migrations
```

Then you'll see Spring Boot finish starting:
```
Started PosApiApplication in 8.432 seconds (JVM running for 9.1)
```

**The server is now running.** Don't close this terminal — closing it stops the server. Open a new terminal tab for the next steps.

To open a new terminal tab in VS Code: click the `+` button in the terminal panel header.

---

### 🔵 Step 11: Confirm Everything is Working

In a **new terminal tab**, run:

```bash
curl http://localhost:8080/api/actuator/health
```

You should get back:
```json
{"status":"UP"}
```

If you get `Connection refused`, the server isn't running yet — go back to Step 10 and check the logs.

You can also just open this URL in your browser — same result:
```
http://localhost:8080/api/actuator/health
```

---

### 🔵 Step 12: Open the API Documentation (Swagger UI)

Open this in your browser:
```
http://localhost:8080/api/swagger-ui.html
```

**What is Swagger UI?** It's a web page that automatically documents every API endpoint. You can see what data each endpoint expects, what it returns, and even test them right in the browser — no code required. It's the best way to explore and test the API while you're building.

---

### 🔵 Step 13: Your First Login

In your terminal, run:

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
- `curl` — a tool for making web requests from the terminal (like a browser, but text-only)
- `-X POST` — send a POST request (submitting data, not just fetching a page)
- `-H "Content-Type: application/json"` — tells the server we're sending JSON data
- `-d '{ ... }'` — the data we're sending (the login form)

**You should get back something like:**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbi...",
    "employeeName": "Admin User",
    "role": "ADMIN",
    "companyName": "UniversalPOS Demo Store"
  }
}
```

That long `eyJ...` string is your **JWT token** — your proof of identity for every other request. Copy the whole token value.

> ⚠️ Change the default password (`ChangeMe123!`) once employee management is built in Phase 3.

---

### 🔵 Step 14: Make Your First Authenticated API Call

Use your token from Step 13 to search for customers. Replace `YOUR_TOKEN_HERE` with the actual token:

```bash
curl -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  "http://localhost:8080/api/customers/search?q=Jane"
```

**What `Authorization: Bearer <token>` means:** Every protected endpoint requires you to prove who you are. You do this by sending the token in this header. The server reads it, checks the signature, and knows you're a logged-in ADMIN for the demo-store tenant.

You'll get back an empty results list (no customers yet — that's expected). In the next section, you'll create a customer and process a full sale.

---

### ⭐ JWT Token Helper — Never Type Tokens by Hand

Instead of copying and pasting your JWT token into every curl command, use the login script. It logs you in and saves the token to a variable automatically.

**Windows (use in your VS Code terminal):**

```bash
# Run this once after starting the server:
scripts\login.bat

# After it runs, %TOKEN% is set. Use it like:
curl -H "Authorization: Bearer %TOKEN%" http://localhost:8080/api/customers/search?q=Jane

# Log in as a different user:
scripts\login.bat manager@demo.com ManagerPass demo-store
```

**Mac/Linux:**

```bash
# Must use "source" (not just run it) so $TOKEN stays in your current shell:
source scripts/login.sh

# After it runs, $TOKEN is set. Use it like:
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/customers/search?q=Jane

# Log in as a different user:
source scripts/login.sh manager@demo.com ManagerPass demo-store
```

**How it works:** The script calls `POST /auth/login`, extracts the token from the JSON response using `jq`, and runs `export TOKEN=...` in your shell. As long as you stay in the same terminal window, `$TOKEN` works until the token expires (24 hours) or you close the terminal.

**Requires `jq`** — a tiny command-line JSON parser.
- Mac: `brew install jq`
- Windows: Download from https://jqlang.github.io/jq/download/ and put it in your PATH
- Ubuntu/WSL: `sudo apt install jq`

---

### ⭐ VS Code Tips for Daily Use

Now that everything is running, here are the shortcuts that will save you the most time:

| Shortcut (Win) | Shortcut (Mac) | What it does |
|---|---|---|
| `Ctrl+\`` | `Ctrl+\`` | Open / close the built-in terminal |
| `Ctrl+P` | `Cmd+P` | Open any file by name — type "Transaction" to jump there instantly |
| `Ctrl+Shift+F` | `Cmd+Shift+F` | Search all files for any text |
| `F12` | `F12` | Jump to the definition of a class or method |
| `Shift+F12` | `Shift+F12` | See everywhere a method is called |
| `Ctrl+Space` | `Ctrl+Space` | Show autocomplete suggestions while typing |
| `Ctrl+.` | `Cmd+.` | Quick fix — adds missing imports automatically |
| `Alt+Shift+F` | `Opt+Shift+F` | Auto-format the current file |
| `F5` | `F5` | Start the server (uses launch.json) |
| `Shift+F5` | `Shift+F5` | Stop the running server |

**The Spring Boot Dashboard:** Look in the left sidebar for the leaf 🌿 icon. Click it to see your `pos-api` app. You can start and stop the server with one click from here — no terminal needed.

**What VS Code can't do (that IntelliJ can):**
- Built-in database browser — use Oracle SQL Developer separately for browsing tables
- Some advanced Spring refactoring tools

These won't block you on this project. VS Code with these extensions is fully capable for everything we're building.

---

### 💡 Alternative Editor: IntelliJ IDEA

If you ever want to try IntelliJ, the free **Community Edition** is at https://www.jetbrains.com/idea/download. Open the project via `File → Open` and select the `universal-pos` folder. IntelliJ auto-detects Maven and imports everything. The project runs identically in both editors.

---
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

### Tenants (Your Store Config)
| Method | URL | What it does |
|---|---|---|
| GET | `/tenants/current` | View your store settings |
| PUT | `/tenants/current` | Update branding, tax rate, etc (Admin only) |
| POST | `/tenants` | Create a new company/store (Admin only) |

### Employees
| Method | URL | What it does |
|---|---|---|
| GET | `/employees` | List all staff (Manager+) |
| GET | `/employees/{id}` | Get one employee (Manager+) |
| POST | `/employees` | Create employee (Admin only) |
| PUT | `/employees/{id}` | Update employee (Admin only) |
| POST | `/employees/{id}/change-password` | Change password (Admin only) |

### Customers
| Method | URL | What it does |
|---|---|---|
| GET | `/customers/search?q=term` | Search by phone, email, name, or loyalty card |
| GET | `/customers/{id}` | Get customer profile + loyalty info |
| POST | `/customers` | Create a new customer |

### Products
| Method | URL | What it does |
|---|---|---|
| GET | `/products/search?q=term` | Search catalog |
| GET | `/products/barcode/{barcode}` | Scan a barcode → get product |
| GET | `/products/{id}` | Get one product |
| POST | `/products` | Add a product (Manager+) |
| PUT | `/products/{id}` | Update a product (Manager+) |
| DELETE | `/products/{id}` | Deactivate a product (Manager+) |

### Discounts
| Method | URL | What it does |
|---|---|---|
| GET | `/discounts` | List all discount rules |
| GET | `/discounts/active` | List currently valid rules |
| GET | `/discounts/{id}` | Get one rule |
| POST | `/discounts` | Create a discount rule (Manager+) |
| PUT | `/discounts/{id}` | Update a rule (Manager+) |
| DELETE | `/discounts/{id}` | Deactivate a rule (Manager+) |

### Transactions
| Method | URL | What it does |
|---|---|---|
| POST | `/transactions` | Process a sale |
| GET | `/transactions/{id}` | Look up a transaction |
| POST | `/transactions/{id}/void` | Void a transaction (Manager+) |

### Receipts
| Method | URL | What it does |
|---|---|---|
| POST | `/receipts/{txnId}/email` | Email receipt to an address |
| GET | `/receipts/{txnId}/pdf` | Download PDF receipt |

### Reports (Manager+ only)
| Method | URL | What it does |
|---|---|---|
| GET | `/reports/daily?from=2026-01-01&to=2026-01-31` | Daily sales summary |
| GET | `/reports/top-products?sortBy=REVENUE&limit=10` | Top products |
| GET | `/reports/employee-performance` | Per-cashier stats |
| GET | `/reports/shrinkage` | Inventory shrinkage |
| GET | `/reports/low-stock` | Products needing reorder |

All report endpoints default to today (daily) or last 30 days if no dates provided.
`sortBy` accepts `REVENUE` or `UNITS`.

### Returns & Exchanges
| Method | URL | What it does |
|---|---|---|
| GET | `/returns/reasons` | List all return reason codes |
| POST | `/returns/reasons` | Create a reason code (Manager+) |
| POST | `/returns` | Process a return against a sale |
| POST | `/returns/exchange` | Process an exchange |

### Inventory
| Method | URL | What it does |
|---|---|---|
| GET | `/inventory/low-stock` | Products at or below reorder point |
| GET | `/inventory/suppliers` | List all suppliers |
| GET | `/inventory/suppliers/search?q=` | Search suppliers |
| POST | `/inventory/suppliers` | Create supplier (Manager+) |
| PUT | `/inventory/suppliers/{id}` | Update supplier (Manager+) |
| GET | `/inventory/purchase-orders` | List all purchase orders |
| GET | `/inventory/purchase-orders/{id}` | Get PO detail |
| POST | `/inventory/purchase-orders` | Create purchase order (Manager+) |
| POST | `/inventory/purchase-orders/{id}/items` | Add item to PO (Manager+) |
| POST | `/inventory/purchase-orders/{id}/submit` | Submit PO to supplier (Manager+) |
| POST | `/inventory/purchase-orders/{id}/receive` | Receive items → updates stock (Manager+) |
| POST | `/inventory/adjustments` | Manual stock adjustment (Manager+) |
| POST | `/inventory/stock-counts/start` | Start stock count (Manager+) |
| POST | `/inventory/stock-counts/{id}/complete` | Complete count + apply variances (Manager+) |

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

### "Migration checksum mismatch for migration version X"

**What it means:** Flyway stores a checksum of every SQL file when it first runs it. If you edit that file after it has already been applied to the database, Flyway detects the change and refuses to start. This happened to us in development because V1, V2, and later files were edited after being applied.

**The permanent fix** (already applied in `application.yml`):

```yaml
spring:
  flyway:
    validate-on-migrate: false
```

This tells Flyway "don't check if migration files have changed since they were applied." The tradeoff is you lose protection against accidental edits to applied migrations — acceptable during active development.

**What NOT to do:** Editing an already-applied migration file is only safe in development when you can wipe the database. In production, always create a new migration file (V5, V6, etc.) instead of editing old ones.

**If you still see this error after adding the config**, it means the `application.yml` change didn't get compiled into `target/classes`. Run:

```bash
mvn clean install -DskipTests
cd pos-api
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The `clean` step forces Maven to recompile and copy the updated yml file into `target/classes`.

---

### Setting JAVA_HOME permanently so you never have to set it manually

If you have multiple Java versions installed, Maven picks the one that `JAVA_HOME` points to.
Setting it permanently means you never have to run `set JAVA_HOME=...` before each build.

**Windows — permanent fix:**
1. Search "Environment Variables" in the Start menu
2. Click "Edit the system environment variables"
3. Click "Environment Variables" button
4. Under "System variables", click "New"
5. Variable name: `JAVA_HOME`
6. Variable value: `C:\Program Files\Eclipse Adoptium\jdk-21.0.7.6-hotspot`
   (adjust the version number to match what you installed)
7. Click OK on all windows
8. Open a **brand new** terminal window and verify:

```bash
mvn -version
```

You should see `Java version: 21.x.x` in the output.

**Mac — permanent fix:**
Add this to your `~/.zshrc` or `~/.bash_profile`:

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
```

Then run `source ~/.zshrc` to apply it immediately.

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

---

---

---

---

### [0.6.0] — 2026-03-19 — Phase 3: Reporting Dashboard

**What was added:**

Five manager-only reports, all scoped to the logged-in tenant and date-range filtered.

**Daily Sales Report** (`GET /reports/daily`) — the main dashboard report. Shows gross revenue, total discount amount given, tax collected, net revenue, average ticket size, total return count and refund amount, return rate as a percentage, and an hour-by-hour breakdown of transactions and revenue across the day. Defaults to today if no dates provided.

**Top Products Report** (`GET /reports/top-products`) — best-selling products ranked by either total revenue or units sold. Configurable limit (default 10). Defaults to last 30 days.

**Employee Performance Report** (`GET /reports/employee-performance`) — per-cashier stats including transaction count, total revenue, and average ticket size, sorted by revenue descending. Useful for identifying top performers and spotting gaps.

**Shrinkage Report** (`GET /reports/shrinkage`) — inventory losses broken down by type (DAMAGE, THEFT, EXPIRY, MANUAL_REMOVE) and by product. Shows total units lost and a summary map by type.

**Low Stock Report** (`GET /reports/low-stock`) — products at or below their reorder point. Each row shows current stock, reorder point, and a suggested order quantity (reorder point × 2 − current stock, minimum reorder point).

**New files:** 5 response DTOs, ReportingService, ReportController.
**Updated:** TransactionRepository (6 new queries), TransactionItemRepository (2 new queries), InventoryAdjustmentRepository (1 new query).

### [0.5.0] — 2026-03-19 — Phase 2: Returns & Exchanges

**What was added:**

Full returns and exchange system.

Return processing — a cashier looks up the original sale transaction, selects which items to return and how many (supports partial returns — you can return 1 of 3 guitars bought in the same sale), selects a reason code, and specifies whether each item should be restocked. The system validates that you cannot return more than was originally purchased, and tracks how many have already been returned across multiple return transactions. A RETURN transaction is created linked back to the original sale, inventory is updated, loyalty points are reversed proportionally, and an INVENTORY_ADJUSTMENT record is written. If all items on the original sale are fully returned, its status changes to REFUNDED.

Exchange processing — same as a return, but the customer is also taking new items. The net amount (new items total minus returned items total) is calculated: if positive the customer pays the difference, if negative they receive the difference. A single EXCHANGE transaction is created, both return and new-item stock movements happen atomically.

Return reason codes — configurable per tenant. Each reason has a code (DEFECTIVE, WRONG_ITEM, CHANGED_MIND, etc.), a description, a manager-required flag, and a display order. 7 default reasons are seeded for the demo store by V5 migration. New tenants add their own via `POST /returns/reasons`.

V5 Flyway migration adds: RETURN_REASONS table, RETURN_ITEMS table, 2 sequences, 4 indexes, 7 seeded reason codes.

**New files:**
- Domain: ReturnReason, ReturnItem
- Repository: ReturnReasonRepository, ReturnItemRepository, TransactionItemRepository
- Service: ReturnService
- Controller: ReturnController
- DTO: CreateReturnRequest (handles both return and exchange)
- Migration: V5__returns_schema.sql

### [0.4.0] — 2026-03-19 — Phase 1 Complete: All Remaining Backend Built

**What was added:**

JWT Token Helper scripts (`scripts/login.sh` for Mac/Linux, `scripts/login.bat` for Windows). Run once, saves your token to `$TOKEN` / `%TOKEN%`. Never copy-paste a token again.

EmployeeService and EmployeeController — full staff management. Admins can create employees with any role (CASHIER/MANAGER/ADMIN), update their info, deactivate accounts, and change passwords. All passwords go through BCrypt before storage.

DiscountController — manages discount rules via API. List all rules, list only currently active ones, create new rules, update existing ones, and deactivate rules. Previously discounts could only be added via seed SQL.

TenantController — `GET /tenants/current` shows your store configuration. `PUT /tenants/current` lets admins update branding (company name, logo, colors, receipt header/footer), tax rate, currency, and timezone without touching the database.

ProductService — refactored ProductController to use a proper service layer with SKU uniqueness validation, audit logging on create/update, and a clean `deactivate` endpoint.

TransactionService.getById + `GET /transactions/{id}` — look up any completed transaction by ID. Was missing from the original controller.

**Bug fixes:**
- Admin password hash in V2 seed data was incorrect (never matched ChangeMe123!) — fixed via V4 migration that runs UPDATE on startup
- Flyway checksum mismatch when inventory tables were appended to V1 — fixed by moving inventory tables to V3 migration
- MailHog health check causing `{"status":"DOWN"}` — fixed by disabling mail health check in application-local.yml
- open-in-view warning — suppressed in application-local.yml

**Phase 1 status: COMPLETE**
All originally planned Phase 1 features are built and running.

### [0.3.0] — 2026-03-18 — Phase 1.5: Full Inventory System

**What was added:**

Complete inventory management system on top of the existing POS foundation.

Supplier management — create and maintain the companies you order from. Track contact info, account numbers, payment terms (Net 30, COD, etc.), lead times, and notes. Each supplier belongs to a tenant. Products can be linked to multiple suppliers with preferred supplier designation and supplier-specific SKUs and costs.

Purchase orders — full PO lifecycle from DRAFT through SUBMITTED → CONFIRMED → PARTIAL → RECEIVED. Managers create a PO, add line items (product + qty + unit cost), submit it to the supplier, and record receipt when inventory arrives. Supports partial receipts — if a supplier ships in multiple shipments, each one is recorded separately and the PO automatically moves from PARTIAL to RECEIVED when all items are accounted for. Every receipt automatically increments product stock and writes an INVENTORY_ADJUSTMENT record.

Manual stock adjustments — managers can record stock changes outside of sales or POs. Types: MANUAL_ADD, MANUAL_REMOVE, DAMAGE, THEFT, EXPIRY, TRANSFER. Every adjustment is immutable and permanently logged with the employee who made it, the reason, and before/after quantities.

Physical stock counts — managers start a count session (FULL, PARTIAL, or CATEGORY). The system snapshots current expected stock levels for all matching products. Staff enter physical counts per product. The system calculates variances (expected minus counted). When the manager completes the count, COUNT_CORRECTION adjustments are automatically applied for every variance, updating actual stock quantities. Only one count can be in progress at a time per tenant.

Low stock alerts — `GET /inventory/low-stock` returns all products whose stock is at or below their reorder point. Used to trigger purchase orders before stock runs out.

Inventory adjustment audit trail — every stock movement (sale, return, PO receipt, manual adjustment, count correction) creates an INVENTORY_ADJUSTMENT record. This gives a complete history of how stock changed over time for any product.

**New database tables (appended to V1 migration):**
SUPPLIERS, PRODUCT_SUPPLIERS, PURCHASE_ORDERS, PURCHASE_ORDER_ITEMS,
INVENTORY_ADJUSTMENTS, STOCK_COUNTS, STOCK_COUNT_ITEMS
+ 7 new sequences + 9 new indexes

**New Java files:**
7 domain entities, 4 repositories, 1 service (InventoryService), 1 controller (InventoryController)

**Bug fixes in this update:**
- `ORA-01408` duplicate index error — removed CREATE INDEX statements for columns already covered by UNIQUE constraints (Oracle auto-indexes unique constraints)
- `maven.config` invalid threads value — emptied the file; the `-T 1` flag had a leading space that Windows parsed incorrectly
- Hibernate schema validation `wrong column type` on `tax_rate` — changed `ddl-auto` from `validate` to `none` (Flyway owns the schema; Hibernate validation caused Oracle NUMBER vs FLOAT type mismatch errors)
- Added `@JdbcTypeCode(SqlTypes.NUMERIC)` to `Tenant.taxRate` for correct Oracle type mapping

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
