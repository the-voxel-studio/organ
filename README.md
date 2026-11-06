# Organ: Collaborative Project & Organ Management Platform

**English** | [Français](README.fr.md)

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![PHP](https://img.shields.io/badge/PHP-8.4-777BB4.svg?logo=php&logoColor=white)](https://www.php.net/)
[![Symfony](https://img.shields.io/badge/Symfony-8.0-black.svg?logo=symfony&logoColor=white)](https://symfony.com/)
[![Angular](https://img.shields.io/badge/Angular-21-DD0031.svg?logo=angular&logoColor=white)](https://angular.dev/)
[![Android](https://img.shields.io/badge/Android-Jetpack%20Compose-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com/compose)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg?logo=docker&logoColor=white)](https://www.docker.com/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1.svg?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![MongoDB](https://img.shields.io/badge/MongoDB-7.0-47A248.svg?logo=mongodb&logoColor=white)](https://www.mongodb.com/)
[![Valkey](https://img.shields.io/badge/Valkey-7.2-CC342D.svg?logo=redis&logoColor=white)](https://valkey.io/)
[![Mercure](https://img.shields.io/badge/Mercure-Realtime-00A389.svg)](https://mercure.rocks/)

---

## 📖 Table of Contents

1. [What is Organ?](#-what-is-organ)
   * [Origins and Philosophy](#origins-and-philosophy)
   * [The Tripartite Hierarchy: Project > Organ > Task](#the-tripartite-hierarchy-project--organ--task)
   * [Storage Freedom: BYOS ("Bring Your Own Storage")](#storage-freedom-byos-bring-your-own-storage)
   * [Security & Granular Permissions (RBAC)](#security--granular-permissions-rbac)
   * [Urgency Management & Chromatic Scale](#urgency-management--chromatic-scale)
   * [Traceability, Audit Log & Analytics](#traceability-audit-log--analytics)
   * [Context-Aware Integrated AI Assistant](#context-aware-integrated-ai-assistant)
   * [Multi-Platform Ecosystem](#multi-platform-ecosystem)
2. [Technical Architecture & Microservices](#-technical-architecture--microservices)
   * [Service Mapping](#service-mapping)
   * [Architecture Diagram](#architecture-diagram)
3. [The AI Assistant: A Semi-Agentic Experiment](#-the-ai-assistant-a-semi-agentic-experiment)
   * [Why "semi-agent"?](#why-semi-agent)
   * [How It Works](#how-it-works)
   * [Safeguards in Place](#safeguards-in-place)
   * [Limitations: A One-Way Trip](#limitations-a-one-way-trip)
   * [What a Real Agent Would Need](#what-a-real-agent-would-need)
4. [Environment Configuration](#-environment-configuration)
   * [1. Project Root (`.env`)](#1-project-root-env)
   * [2. Backend API (`api/.env`)](#2-backend-api-apienv)
   * [3. Angular Frontend (`front_angular/.env`)](#3-angular-frontend-front_angularenv)
   * [4. Legacy PHP Frontend (`front_php/.env`)](#4-legacy-php-frontend-front_phpenv)
5. [Tutorial: Google Cloud Setup (OAuth 2.0 & Drive)](#-tutorial-google-cloud-setup-oauth-20--drive)
6. [Installation & Quick Start](#-installation--quick-start)
7. [Android Mobile Application](#-android-mobile-application)
8. [Demo Data (Seed) & Test Accounts](#-demo-data-seed--test-accounts)
9. [Testing & Code Quality](#-testing--code-quality)
10. [License](#-license)

---

## 💡 What is Organ?

### Origins and Philosophy

**Organ** was conceived by three computer science students with distinct organizational workflows: the creative thinker driven by visual hierarchy, the rigorous planner focused on structure, and the pragmatist seeking immediate execution.

Analyzing existing project management solutions revealed two common pitfalls:
* **The "Wall of Tasks":** Flat to-do lists that start out simple, but quickly morph into endless, anxiety-inducing scroll fields devoid of context.
* **The "Labyrinth":** Over-nested documentation systems (infinite sub-pages inside sub-pages) where teams spend more time filing and searching than executing.

Organ was designed to bridge this divide: **delivering a clean, human-centered, and deeply secure organizational structure** without compromising on visual clarity or speed of execution.

---

### The Tripartite Hierarchy: Project > Organ > Task

Organ eliminates cognitive overload through a strict 3-tier structure:

```text
📁 PROJECT (Strategic Vision & Global Team)
│
├── 🫀 ORGAN A (Autonomous business module, team, sprint, or sub-project)
│    ├── 📋 Task 1 (Priority 8/10, deadline, review/validation required)
│    ├── 📋 Task 2 (Blocked by Task 1 dependency)
│    └── 📋 Task 3
│
└── 🫀 ORGAN B (External module exposed safely to contractors or clients)
     ├── 📋 Task 4
     └── 📋 Task 5
```

1. **Project:** The high-level umbrella (title, description, visual identity color, custom icon/emoji, associated Google Drive storage, and global team members).
2. **Organ:** An autonomous functional unit within the project. An Organ represents a business area (*Design*, *Backend*, *Marketing*), a project milestone (*Phase 1*, *Sprint 3*), or a dedicated outsourced space. Each Organ possesses its own **Kanban board**, contextual quick links panel, and, crucially, its own **local role and permission system**.
3. **Task:** The atomic unit of work, enriched with comprehensive attributes:
   * Dynamic status (`TODO`, `IN_PROGRESS`, `WAITING`, `DONE`, `CANCELED`) with an explicit status message.
   * Fine-grained priority rating from 1 to 10.
   * Start and due dates with hourly time estimations.
   * Designated task actors: Creator, Technical Manager, and Official Validator (*sign-off*).
   * Intelligent dependency management: a task clearly indicates if it is blocked by prerequisite tasks.
   * Cross-cutting project tags, team discussion comments, and file attachments.

---

### Storage Freedom: BYOS ("Bring Your Own Storage")

Unlike traditional SaaS tools that leverage vendor lock-in and charge monthly markups per gigabyte, Organ embraces the **Bring Your Own Storage (BYOS)** paradigm:

* **Zero Cloud Storage Markups:** Connect your own **Google Drive** account directly to any Organ project. Project files and assets remain securely within your own cloud infrastructure, under your legal and operational control.
* **Hybrid Storage Architecture:** Task attachments can be stored either on Organ's internal infrastructure (**MongoDB GridFS/Documents**) or within your personal/enterprise **Google Drive** (`drive://<fileId>`).
* **Integrity & Versioning:** Every uploaded asset is tracked with an automatic **SHA-256** checksum, MIME-type, file size verification, and incremental version number.

---

### Security & Granular Permissions (RBAC)

Organ features an enterprise-grade Role-Based Access Control (RBAC) engine built to safely accommodate hybrid teams (internal employees, freelancers, agencies, and external clients):

* **Strict Organ-Level Isolation:** A collaborator invited to a project can be restricted exclusively to a single Organ. All other Organs within that project remain completely invisible to them.
* **Global vs. Local Roles:**
  * *Global Project Roles:* `ADMIN`, `MANAGER`, `MEMBER`.
  * *Local Organ Roles:* Fully customizable roles defined per Organ with custom names and icons.
* **Over 40 Unit Permissions:**
  * *Organ Management:* View, edit, manage local members, configure custom roles, manage external links.
  * *Task Operations:* Create tasks, edit own vs. all tasks, soft delete vs. hard delete, change status, adjust priority, schedule start/due dates, edit hourly estimates.
  * *Workflow & Assignments:* Self-assign, assign others, officially validate tasks (`TASK_VALIDATE`).
  * *Associated Resources:* Attachments, Web links, project tags, and task dependencies.
  * *Comments:* Post, edit own comments vs. moderate all comments.
* **Hardened Security Protections:** The API enforces comprehensive protection against IDOR (*Insecure Direct Object Reference*), mass-assignment exploits, and automatic cache invalidation upon privilege modification.
* **Two-Level Soft Delete & Trash:** Safely recover deleted items from either the local Organ trash or the global Project trash, backed by separate permissions for permanent data destruction.

---

### Urgency Management & Chromatic Scale

Organ combats mental fatigue through immediate visual cues:
* A calibrated urgency rating from **1 (lowest priority) to 10 (critical / urgent)**.
* A dynamic chromatic gradient transitioning from soft bubblegum pink to crimson red: dashboards and task cards highlight immediate priorities at a glance without tedious scanning.
* Clear visual alerts for blocked tasks so contributors instantly recognize blockers before joining unnecessary status syncs.

---

### Traceability, Audit Log & Analytics

All workspace mutations are permanently recorded in a dedicated **MongoDB AuditLog collection**:
* Comprehensive event logging: creation, status updates, date rescheduling, manager reassignments, and attachment uploads.
* Timeline view integrated directly within each task detail modal.
* Automated daily stats aggregation (`DailyStat`) via cron or manual dispatch, providing the frontend with:
  * Overall project activity velocity curves.
  * Task distribution doughnut charts by status.
  * Workload distribution bar charts across Organs.
  * Consultation and team engagement indicators.

---

### Context-Aware Integrated AI Assistant

Organ includes a virtual assistant backed by a *Mistral AI* LLM served through **NVIDIA NIM** (OpenAI-compatible API). It does more than answer: it can **produce a sequence of actions** (create a project, Organs, roles, tasks, navigate) that the application then executes on the user's behalf.
* **Injected context:** current page, accessible projects, and the Organs, roles and members of the open project.
* **Teaching mode (`/ask`):** explains step by step how to do something yourself (e.g. invite an external collaborator into an Organ).
* **Action modes (`/create`, `/edit`, `/role`):** generate an action plan that is executed automatically, with a "read-only" toggle to switch it off.

> [!NOTE]
> This is a **student experiment**, a first and still rudimentary take on agentic AI. How it works, and its (significant) flaws, are described in [The AI Assistant: A Semi-Agentic Experiment](#-the-ai-assistant-a-semi-agentic-experiment).

---

### Multi-Platform Ecosystem

Organ provides seamless collaboration across modern interfaces:

* **Modern Web Frontend (Angular 21 - Primary):**
  * High-performance Single Page Application powered by **Angular 21** and **Tailwind CSS 4**.
  * GSAP animations, interactive drag-and-drop Kanban boards, Chart.js analytics, role administration panels, and the integrated AI chat assistant.
* **Native Android Application:**
  * Built from scratch in **Kotlin** with declarative **Jetpack Compose** and Material Design 3.
  * One-tap login with **Google Identity Credential Manager**, secure token storage via `EncryptedSharedPreferences`, and native task/trash screens.
* **Legacy Web Frontend (Symfony Twig / PHP):**
  * Historical frontend based on Symfony Twig, Stimulus, and Tailwind CSS (maintained for backwards compatibility, currently deprecated in favor of Angular).
* **Real-Time Push (Mercure Hub):**
  * Server-Sent Events (SSE) delivering live push notifications for status shifts, task assignments, manager nominations, and comments.

---

## 🏗 Technical Architecture & Microservices

Organ uses a modular microservices architecture orchestrated via **Docker Compose**. Each service runs in an isolated container over an internal private network.

### Service Mapping

| Docker Service | Technology | Role & Responsibility | External Port |
| :--- | :--- | :--- | :--- |
| `front-angular` | **Angular 21 / Node.js** | Modern Web SPA Frontend | [http://localhost:4200](http://localhost:4200) |
| `api-nginx` | **Nginx Alpine** | HTTP reverse proxy and web termination for API | [http://localhost:8001](http://localhost:8001) |
| `api-php` | **PHP 8.4-FPM / Symfony 8** | Core API backend, REST controllers, JWT & RBAC engine | Internal (FastCGI) |
| `database` | **MySQL 8.0** | Relational DB (Users, Projects, Organs, Roles, Tasks) | `3306` |
| `mongodb` | **MongoDB 7.0** | NoSQL DB (Audit logs, daily stats, GridFS files) | `27017` |
| `valkey` | **Valkey 7.2** | High-performance in-memory cache (Redis fork) | `6379` |
| `mercure` | **Mercure Hub** | Real-time Server-Sent Events (SSE) hub | [http://localhost:9090](http://localhost:9090) |
| `phpmyadmin` | **phpMyAdmin** | Web management UI for MySQL database | [http://localhost:8080](http://localhost:8080) |
| `front-nginx` | **Nginx Alpine** | Web server for the legacy PHP frontend | [http://localhost:8000](http://localhost:8000) |
| `front-php` | **PHP 8.4-FPM** | Legacy Symfony Twig frontend (deprecated) | Internal (FastCGI) |
| `android` | **Kotlin / Jetpack Compose** | Native Android application (in `android/`) | Standalone |

---

### Architecture Diagram

```mermaid
flowchart TB
    subgraph Clients["User Clients"]
        ANG["Angular 21 Frontend<br/>:4200"]
        AND["Android Native App<br/>(Jetpack Compose)"]
        PHP_F["Legacy PHP Twig<br/>(Deprecated :8000)"]
    end

    subgraph Ingress["Web Ingress & Proxy"]
        API_NGX["api-nginx<br/>:8001"]
    end

    subgraph CoreBackend["Backend Core"]
        API_PHP["api-php (Symfony 8 / PHP 8.4)<br/>- Lexik JWT & Refresh Tokens<br/>- RBAC Permission Engine<br/>- Doctrine ORM & ODM<br/>- Mistral AI Integration"]
    end

    subgraph DataStorage["Persistence & Cache"]
        MYSQL[("MySQL 8.0<br/>Relational Data<br/>Projects, Organs, Tasks, Roles")]
        MONGO[("MongoDB 7.0<br/>AuditLog, DailyStat,<br/>GridFS Files")]
        VALKEY[("Valkey 7.2<br/>Application Cache & Sessions")]
    end

    subgraph Realtime["Realtime Engine"]
        MERCURE["Mercure Hub :9090<br/>Server-Sent Events (SSE)"]
    end

    subgraph ExternalServices["External Cloud Services"]
        GOOGLE["Google Cloud APIs<br/>- OAuth 2.0 Identity<br/>- Google Drive API"]
        NVIDIA["NVIDIA AI Endpoints<br/>Mistral AI LLM"]
    end

    ANG -->|HTTP REST / JWT| API_NGX
    AND -->|HTTP REST / JWT| API_NGX
    PHP_F -->|HTTP REST| API_NGX
    ANG -.->|SSE Subscription| MERCURE

    API_NGX -->|FastCGI| API_PHP

    API_PHP -->|SQL / Doctrine ORM| MYSQL
    API_PHP -->|BSON / Doctrine ODM| MONGO
    API_PHP -->|Redis Protocol| VALKEY
    API_PHP -->|Publish SSE| MERCURE
    API_PHP -->|OAuth & BYOS Files| GOOGLE
    API_PHP -->|AI Inference Requests| NVIDIA
```

#### Detailed Backend Schema
![Backend Schema](docs/Backend.png)

---

## 🤖 The AI Assistant: A Semi-Agentic Experiment

> [!IMPORTANT]
> **Student work, and a first attempt.** Organ is a student project, and the specification did not call for any AI. The assistant is a **voluntary addition** on top of the required scope. It is also the **very first time** we integrated an LLM into an application, starting **without in-depth knowledge of the subject**. So this section does not advertise a finished feature: it documents **a first experiment**, what it does, how it does it, and above all **what its flaws taught us**.

### Why "semi-agent"?

An AI assistant can be placed on one of three levels:

| Level | What the model does | Example |
| :--- | :--- | :--- |
| **Chatbot** | It replies with text, and that's it. | Conversational FAQ |
| **Semi-agent** *(Organ)* | It **produces an action plan** that the application executes, but **in a single pass and without ever seeing the outcome**. | "Create a Marketing project with 3 tasks" |
| **Agent** | It acts, **observes the result**, corrects itself and repeats **in a loop** until the goal is reached. | Coding agents, tool-using assistants |

Organ's assistant does more than talk: it really changes data. But it acts **blindly**. It writes its whole plan at once, before anything is executed, and then has no way of knowing what happened. It is a **one-way trip** (*one pass*), a **preliminary** stage of agentic AI, missing the one thing that makes an agent: **the observation loop**.

### How It Works

```mermaid
sequenceDiagram
    autonumber
    actor U as User
    participant F as Angular<br/>(ChatbotService)
    participant A as Symfony API<br/>(ChatbotController)
    participant DB as MySQL
    participant L as Mistral LLM<br/>(NVIDIA NIM)
    participant E as Angular<br/>(ActionExecutor)

    U->>F: "/create a Marketing project with 3 tasks"
    F->>A: POST /api/chatbot/message<br/>{history, context: route, command, actionsEnabled}
    A->>DB: User's projects + Organs, roles, members of the route's project
    A->>L: Huge system prompt + JSON context + last 10 messages
    L-->>A: {"message": "...", "actions": [ ... ]}
    A->>A: JSON extraction / repair
    A-->>F: message + action plan
    F->>E: executeActions(actions)
    loop For each action, in order
        E->>A: Regular REST call (user's JWT)
        A-->>E: Created resource (UUID)
    end
    E-->>U: Summary toast + page refresh
    Note over L,E: The LLM is never told how the actions turned out
```

**1. The frontend prepares the request** (`front_angular/src/app/services/chatbot/chatbot.service.ts`)
The user picks a command: `/ask` (default, explanation), `/create`, `/edit` or `/role`. The service sends the conversation history plus a small context: the current route, the command, and the state of the "actions allowed" toggle.

**2. The backend builds the context and the prompt** (`api/src/Controller/ChatbotController.php`)
* It loads the user's **accessible projects** from the database and, if the route contains a project UUID, that project's **Organs, their roles, and its members**.
* It assembles a **system prompt** of more than a hundred lines: functional overview, sitemap, business rules, branding rules, the list of 30+ permissions, **the JSON schema of every possible action**, and rules on brevity and token budget. A directive specific to the chosen command is appended.
* It sends everything, with the **last 10 messages**, to the NVIDIA NIM API (`temperature: 0.2`, `max_tokens: 1024`, `response_format: json_object`). The model is configurable through `NVIDIA_MODEL`.

**3. The model answers with a plan**
The LLM must return a strict JSON object:

```json
{
  "message": "I'm creating the **Marketing** project with a *Campaigns* Organ and 2 tasks.",
  "actions": [
    { "type": "create_project", "id": "proj1", "payload": { "title": "Marketing", "color": "#FF7DD4", "iconType": "EMOJI", "iconData": "📣" } },
    { "type": "create_organ", "id": "org1", "projectUuid": "proj1", "payload": { "title": "Campaigns", "highlightColor": "#355EE4", "iconType": "EMOJI", "iconData": "🎯" } },
    { "type": "create_task", "id": "task1", "projectUuid": "proj1", "organUuid": "org1", "payload": { "title": "Define the audience", "priority": 7 } },
    { "type": "create_task", "projectUuid": "proj1", "organUuid": "org1", "payload": { "title": "Design the visuals", "priority": 5, "dependencyUuids": ["task1"] } },
    { "type": "navigate", "path": "/organ/proj1/org1" }
  ]
}
```

`proj1`, `org1` and `task1` are **temporary variables**: the model cannot know the real UUIDs yet, since nothing has been created. Available vocabulary: `navigate`, `create_project`, `create_organ`, `create_role`, `copy_role`, `paste_role`, `create_task`, `update_task`.

**4. The backend "repairs" the answer when needed**
LLMs don't always respect the format. The controller extracts the text between the first `{` and the last `}`, escapes illegal line breaks, and if the response was **truncated** by the token limit, `repairTruncatedJson()` scans the text character by character to **salvage only the complete actions**.

**5. The frontend executes the plan** (`front_angular/src/app/services/chatbot/chatbot-action-executor.service.ts`)
Actions run **sequentially**, through **the same REST services** as the regular UI. After each creation, the real UUID returned by the API is mapped to its temporary variable and substituted into the following actions. `copy_role` / `paste_role` go through `localStorage`. At the end, a toast summarizes what was created and the page refreshes.

### Safeguards in Place

* **The AI has no more rights than the user.** Actions go through the standard REST API with the user's JWT, so **the whole RBAC engine applies**. An action the user is not allowed to perform fails for the AI too.
* **No deletion possible.** It is forbidden in the prompt, and, more importantly, **the action type does not exist in the executor**. The protection is structural; it does not depend only on the model obeying.
* **Read-only mode.** A toggle in the panel disables execution. The instruction is sent to the model **and** enforced client-side: even if the model returns actions, they are ignored.
* **Project list restricted** to projects the user is a member of (but see limitation #11 for the rest of the context).
* **Constrained output:** enforced JSON, low temperature.

### Limitations: A One-Way Trip

This is the most instructive part of the experiment. In real use, the system shows **major** flaws, and almost all of them come from the same root cause: **there is no feedback loop**.

**1. A single pass, with the plan fixed in advance.**
One message triggers **one** LLM call. The whole plan is written **before** any execution, against a world that does not exist yet (hence `proj1`, `task1`…). The model cannot adapt step 3 to the outcome of step 2.

**2. Errors are never observed.**
If an action fails (permission denied, invalid field, non-existent UUID), the executor shows an error toast and stops. The model **is never told**: the history keeps only the *text* of its messages, not the actions it emitted nor their results. On the next turn, it "believes" it succeeded, since its own message said "I'm creating…". It cannot correct itself, retry, or even explain the failure.

**3. No rollback.**
Actions are not a transaction. If the 4th action fails, the first 3 remain. The user is left with a **partial** state to clean up by hand.

**4. Success is announced before it happens.**
The displayed `message` is written by the model **before** execution: it describes an *intention*, not a result. The final "operation completed successfully" toast only counts HTTP 2xx responses. Nothing checks that what was created matches the request.

**5. A partly blind context, with no way to ask for more.**
The model only sees what is injected into the prompt, and **the task list is not included**: for `/edit` (`update_task`) it needs a `taskUuid` it doesn't know, which tempts it to invent one. Without read tools, it cannot fetch missing information. Everything must be guessed or preloaded.

**6. Truncation and silent partial execution.**
With 1024 output tokens, an ambitious plan gets cut off mid-way. The repair mechanism salvages complete actions, which means **part of the plan runs without warning**, sometimes without the final navigation. Prompt rules like "3 or 4 tasks max" or "one-sentence descriptions" are **workarounds** for this, not a solution.

**7. Control through the prompt only.**
The system prompt is full of "CRITIQUE", "ABSOLUMENT", "IMPÉRATIVEMENT", "Ne dis JAMAIS" (CRITICAL, ABSOLUTELY, NEVER). That is the symptom of a system that can only **hope** the model complies: actions are not schema-validated before execution, and an unknown type is just skipped with a `console.warn`.

**8. Fragile mechanisms.**
* Temporary variable substitution is a `replaceAll` over **every** string: `task1` also gets replaced inside `task10`, or inside a title containing that text.
* JSON extraction relies on heuristics (first and last brace, repair regexes).

**9. Cost, latency and privacy.**
Every message resends the full prompt, the project list, and **members' names and emails** to a third-party provider. There is no streaming: the user waits for the full response, then for execution.

**10. Strong dependency on the provider.**
The model we started with (*Mixtral 8x22B*) was removed from NVIDIA's API catalog, which forced us to switch to *Mistral Medium 3.5*. But the prompt had been developed against the first model, and nothing guarantees another model behaves the same way. Without a test or evaluation set for the responses, there is no way to check that the assistant still works as well after the switch.

**11. Context that bypasses isolation.**
The project whose Organs, roles and members get loaded is derived from the route **sent by the client**, and the controller does not check that the user is a member of that project. It also injects **every** Organ of the project, including those the user has no access to. That information, members' emails included, ends up in the prompt and can surface in the model's answer. This contradicts the Organ-level isolation the RBAC engine enforces everywhere else, and it is exactly the kind of hole an AI introduces when it is wired to the data without going through the same access controls as the rest of the API.

### What a Real Agent Would Need

| Organ today | An agentic approach |
| :--- | :--- |
| Free-form JSON inside text, repaired by hand | **Native tool calling** (*function calling*) with validated schemas |
| Whole plan written upfront | A **loop**: *reason → call a tool → observe → continue*, with a max iteration count |
| Client-side execution, results never reported back | **Server-side** execution, with each result (UUID, error) **fed back to the model** |
| All context stuffed into the prompt, with no access checks | **Read tools** (`list_tasks`, `get_organ`…) going through the same RBAC checks as the API, so the model fetches what it needs, and only what it is allowed to see |
| Temporary `proj1` variables | Real UUIDs, since each creation is observed before the next one |
| Error = stop + toast | Error = **observation**: the model can fix, retry, or explain |
| No undo | **Plan preview and confirmation** before writes, transactions or compensating actions |
| Text-only history | History that includes **tool calls and their results** |

### What We Take Away

This module was not required, and it remains imperfect. Still, it made concrete for us the difference between **a model that generates instructions** and **an agent that pursues a goal**, and that difference is **the loop**. Almost everything we had to patch together (JSON repair, task caps, all-caps rules, temporary variables) compensates for the missing feedback. The assistant's own help panel admits as much: it advises users to phrase requests "step by step" and to "check the result" before going further. In other words, **the human plays the role of the loop** that the system cannot perform itself. To us, that is the main lesson of this experiment, and the starting point for a next version.

---

## ⚙️ Environment Configuration

Running Organ locally requires four `.env` configuration files.

> [!IMPORTANT]
> **Secret Key Policy:** For `APP_SECRET`, `JWT_PASSPHRASE`, and specifically `MERCURE_JWT_SECRET`, you **must** provide random strings of **at least 32 characters**. Using shorter keys will cause the API container to fail immediately without an explicit error trace.

---

### 1. Project Root (`.env`)
Configures the Docker Compose orchestration layer:
```dotenv
# ==========================================
# MYSQL DATABASE CREDENTIALS
# ==========================================
MYSQL_ROOT_PASSWORD=root_password
MYSQL_DATABASE=app_database
MYSQL_USER=app_user
MYSQL_PASSWORD=app_password

# ==========================================
# PHPMYADMIN
# ==========================================
PMA_USER=root
PMA_PASSWORD=root_password

# ==========================================
# MERCURE REALTIME HUB (Min 32 chars)
# ==========================================
MERCURE_JWT_SECRET=!ChangeThisMercureHubJWTSecretKey!

# ==========================================
# MONGODB NOSQL DATABASE
# ==========================================
MONGODB_ROOT_USER=root
MONGODB_ROOT_PASSWORD=mongodb_root_password
MONGODB_DATABASE=organ

# ==========================================
# GOOGLE CLOUD CREDENTIALS (OAuth & Drive)
# ==========================================
GOOGLE_CLIENT_ID=<YOUR_CLIENT_ID>.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=<YOUR_CLIENT_SECRET>
```

---

### 2. Backend API (`api/.env`)
Configures the Symfony backend core:
```dotenv
###> SYMFONY CORE ###
APP_ENV=dev
APP_SECRET=a_very_long_random_string_of_at_least_32_chars
APP_SHARE_DIR=var/share
DEFAULT_URI=http://localhost
###< SYMFONY CORE ###

###> INFRASTRUCTURE & NETWORK ###
TRUSTED_PROXIES=127.0.0.1,REMOTE_ADDR
TRUSTED_HEADERS=x-forwarded-for,x-forwarded-host,x-forwarded-proto,x-forwarded-port
CORS_ALLOW_ORIGIN='^https?://(localhost|127\.0\.0\.1):(8000|8001|4200)$'
###< INFRASTRUCTURE & NETWORK ###

###> DATABASE & CACHE ###
DATABASE_URL="mysql://app_user:app_password@database:3306/app_database?serverVersion=8.0.32&charset=utf8mb4"
REDIS_URL=redis://organ_valkey:6379
MONGODB_URI="mongodb://root:mongodb_root_password@mongodb:27017/?authSource=admin"
MONGODB_DB="organ_dev"
###< DATABASE & CACHE ###

###> AUTHENTICATION & SECURITY ###
JWT_SECRET_KEY=%kernel.project_dir%/config/jwt/private.pem
JWT_PUBLIC_KEY=%kernel.project_dir%/config/jwt/public.pem
JWT_PASSPHRASE=another_long_random_passphrase_for_jwt
GOOGLE_CLIENT_ID=<YOUR_CLIENT_ID>.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=<YOUR_CLIENT_SECRET>
COOKIE_SECURE=false
###< AUTHENTICATION & SECURITY ###

###> REALTIME (MERCURE) ###
MERCURE_URL=http://mercure/.well-known/mercure
MERCURE_PUBLIC_URL=http://localhost:8000/hub
MERCURE_JWT_SECRET=!ChangeThisMercureHubJWTSecretKey!
NOTIFICATION_BASE_URL=http://localhost:8000
###< REALTIME ###

###> NVIDIA AI ASSISTANT ###
NVIDIA_API_KEY=<nvapi-api_key>
NVIDIA_MODEL=mistralai/mistral-medium-3.5-128b
###< NVIDIA AI ASSISTANT ###
```

---

### 3. Angular Frontend (`front_angular/.env`)
Configures the modern Angular Web client:
```dotenv
NG_APP_GOOGLE_CLIENT_ID=<YOUR_CLIENT_ID>.apps.googleusercontent.com
```

---

### 4. Legacy PHP Frontend (`front_php/.env`)
Configures the deprecated Twig web interface:
```dotenv
###> SYMFONY CORE ###
APP_ENV=dev
APP_SECRET=another_secret_for_the_front_app
###< SYMFONY CORE ###

###> SYMFONY ROUTING ###
DEFAULT_URI=http://localhost:8000
###< SYMFONY ROUTING ###

###> API CONFIGURATION ###
API_URL=http://api-nginx
###< API CONFIGURATION ###

###> AUTHENTICATION ###
GOOGLE_CLIENT_ID=<YOUR_CLIENT_ID>.apps.googleusercontent.com
###< AUTHENTICATION ###
```

---

## 🛠 Tutorial: Google Cloud Setup (OAuth 2.0 & Drive)

To enable Google Sign-In and decentralized project storage with Google Drive:

1. **Open Google Cloud Console:** Navigate to [console.cloud.google.com](https://console.cloud.google.com).
2. **Create a Project:** Create a new project (e.g., `Organ-Dev`).
3. **Enable Google Drive API:** In *APIs & Services* > *Library*, search for **Google Drive API** and click **Enable**.
4. **Configure OAuth Consent Screen:**
   * User Type: **External**.
   * Fill in basic app information (`Organ`) and your developer email.
   * **Scopes:** Add the required scope:
     ```text
     https://www.googleapis.com/auth/drive.file
     ```
     *(This scope strictly restricts Organ to accessing only files created or explicitly opened by the application, ensuring user privacy).*
   * **Test Users:** While in testing status, add the Gmail addresses of testing team members.
5. **Generate Credentials:**
   * Go to *Credentials* > *Create Credentials* > *OAuth Client ID*.
   * Application Type: **Web application**.
   * **Authorized JavaScript Origins:**
     * `http://localhost:4200`
     * `http://localhost:8000`
     * `http://localhost:8001`
   * **Authorized Redirect URIs:**
     * `http://localhost:8000/login/google/check`
     * `http://localhost:4200/login`
6. **Save Secrets:** Copy the generated **Client ID** and **Client Secret** into your `.env` configuration files.

---

## 🚀 Installation & Quick Start

> [!NOTE]
> The `docker-compose.yml` file enforces `linux/amd64` platform compatibility to ensure seamless parity across macOS (Apple Silicon), Windows (WSL2), and Linux.

### 1. Clone the repository
```bash
git clone https://github.com/the-voxel-studio/organ.git
cd organ
```

### 2. Configure Environment Files
Create `.env`, `api/.env`, `front_angular/.env`, and `front_php/.env` following the templates provided above.

### 3. Build and launch Docker containers
```bash
docker compose build
docker compose up -d
```

### 4. Initialize Dependencies & Security Keys
Install backend Composer dependencies and generate asymmetric RSA keys for Lexik JWT authentication:
```bash
# Symfony API Dependencies
docker compose exec api-php composer install

# Legacy PHP Frontend Dependencies
docker compose exec front-php composer install

# Generate JWT cryptographic keypair
docker compose exec api-php php bin/console lexik:jwt:generate-keypair
```

### 5. Build Tailwind Assets (Legacy PHP Frontend only)
*The Angular frontend handles styling automatically.* For the legacy PHP frontend:
```bash
docker compose exec front-php php bin/console tailwind:build
```

> [!TIP]
> **Handling the `Directory not empty` message during cache:clear:**
> When manually clearing Symfony cache (`php bin/console cache:clear`), Symfony may output a non-blocking `Directory not empty` notice while simultaneously reading and writing cache artifacts. Simply re-run the command once to confirm completion.

---

### 🌐 Service Access URLs

| Component | URL | Description |
| :--- | :--- | :--- |
| **Angular Web App** | [http://localhost:4200](http://localhost:4200) | Primary modern web frontend (recommended) |
| **API Swagger Docs** | [http://localhost:8001/api/docs](http://localhost:8001/api/docs) | Interactive OpenAPI / Swagger UI documentation |
| **phpMyAdmin Database Manager** | [http://localhost:8080](http://localhost:8080) | Web management interface for MySQL |
| **Mercure Realtime Hub** | [http://localhost:9090](http://localhost:9090) | Server-Sent Events hub |
| **Legacy PHP App** | [http://localhost:8000](http://localhost:8000) | Legacy Twig interface (deprecated) |

---

## 📱 Android Mobile Application

The repository includes a modern native Android application located in the `android/` directory.

### Mobile Technical Highlights
* **Language & Architecture:** 100% Kotlin with **Jetpack Compose** and **Material 3**.
* **Modern Auth:** Native **Google Identity Credential Manager** (`androidx.credentials`) integration for streamlined one-tap authentication.
* **Networking:** Retrofit 2, OkHttp 3, Coroutines, and Flow for asynchronous communication with the Organ REST API.
* **Local Security:** Session tokens and refresh tokens stored in hardware-backed keystores via `EncryptedSharedPreferences`.
* **Visuals:** Lottie vector animations, dynamic theme selectors, and `colorpicker-compose`.

### Build & Run
1. Open the `android/` folder in **Android Studio** (Koala or newer, JDK 17+).
2. Configure your target API endpoint (use `http://10.0.2.2:8001` if using the standard Android emulator to target your host machine).
3. Run the application directly from Android Studio or build the debug APK via Gradle:
   ```bash
   cd android
   ./gradlew assembleDebug
   ```

---

## 🧪 Demo Data (Seed) & Test Accounts

To immediately test all features (populated Kanban boards, analytical KPI charts, Organ structures), a comprehensive dataset is provided.

### 1. Import MySQL Seed Data
* **Option A (phpMyAdmin):**
  1. Open [http://localhost:8080](http://localhost:8080).
  2. Log in with `root` / `root_password`.
  3. Select `app_database` in the left sidebar.
  4. Navigate to the **Import** tab, choose the `seed.sql` file from the project root, and execute.
* **Option B (Docker CLI):**
  ```bash
  docker compose exec -T database mysql -uapp_user -papp_password app_database < seed.sql
  ```

### 2. Seed MongoDB Analytics Data
To populate activity timelines and daily analytics graphs:
```bash
docker compose exec -T api-php php bin/console app:seed:mock-analytics
```

### 3. Preconfigured Test Accounts
All demonstration accounts use the password: `password`

| Role | Name | Email | Password |
| :--- | :--- | :--- | :--- |
| **Administrator** | Jean Dupont | `admin@organ.com` | `password` |
| **Manager** | Marie Curie | `manager@organ.com` | `password` |
| **Developer** | Albert Einstein | `dev@organ.com` | `password` |
| **User** | Nikola Tesla | `user@organ.com` | `password` |
| **Member** | Grace Hopper | `grace@organ.com` | `password` |

---

## 🧪 Testing & Code Quality

Organ prioritizes data security and permission isolation with comprehensive automated test suites.

### Backend Tests (Symfony & PHPUnit)
Covers unit, functional, and security scenarios (IDOR prevention, mass assignment, cache pollution, granular task and organ permissions):
```bash
docker compose exec api-php bin/phpunit
```

### Frontend Tests (Angular & Vitest)
```bash
cd front_angular
npm test
```

### Mobile Android Tests
```bash
cd android
./gradlew test
```

---

## 📄 License

This project is licensed under the **GNU General Public License v3 (GPLv3)**.  
See the [LICENSE](LICENSE) file for complete terms and conditions.
