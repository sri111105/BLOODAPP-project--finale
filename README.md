# 🩸 Blood Donor Management System

## AI-Powered · Bilingual · Location-Aware Donor–Hospital Coordination Platform

**Real-Time · Prompt-Engineered · Deployable**



> **Blood Donor Management System** is a full-stack, AI-enhanced platform that connects blood donors and hospitals in real time.
> It combines a bilingual (Tamil + English) prompt-engineered LLM assistant, structured emergency-request extraction,
> automated donor matching with live map visualization, and gamified donor engagement — all built on
> **Spring Boot, MySQL, and Google Gemini.**

[📖 Documentation](#-table-of-contents) · [🚀 Quick Start](#-quick-start) · [🎯 Features](#-key-features) · [🏗️ Architecture](#️-system-architecture) · [👥 Team](#-meet-the-team)

---

## 📑 Table of Contents

| Core Sections | Technical Deep Dives | Resources |
|---|---|---|
| [🎯 Key Features](#-key-features) | [🏛️ Prompt Engineering Layer](#️-prompt-engineering-layer) | [⚙️ Configuration](#️-configuration) |
| [🆚 Why This Project?](#-why-this-project) | [🧠 Structured Extraction](#-structured-extraction-engine) | [📊 Database Schema](#-database-schema) |
| [🏗️ System Architecture](#️-system-architecture) | [🗺️ Location & Matching](#️-location--matching-engine) | [🗺️ Roadmap](#️-roadmap) |
| [💬 Request Walkthrough](#-request-walkthrough) | [🏅 Badge & Eligibility Logic](#-badge--eligibility-logic) | [👥 Meet the Team](#-meet-the-team) |
| [🚀 Quick Start](#-quick-start) | [🛡️ Security & Reliability Notes](#️-security--reliability-notes) | [📁 Project Structure](#-project-structure) |

---

## 🎯 Key Features

| Feature | Description |
|---|---|
| 🤖 **Bilingual AI Chatbot** | Tamil + English donor eligibility assistant, powered by Gemini 2.5 Flash with a dedicated prompt-engineering layer |
| 🎙️ **Voice Interaction** | Manual mic input + "Listen" playback, both Tamil (`ta-IN`) and English (`en-IN`) |
| 🧠 **Structured Emergency Extraction** | Converts free-text hospital requests into structured JSON (blood group, units, urgency, location) — plus a manual entry form as a fallback |
| 🩺 **Eligibility Engine** | Every donor is automatically labeled **Eligible** / **Not Eligible** based on a 90-day donation cycle |
| 🏅 **Time-Based Recognition Badges** | Bronze → Silver → Gold badge auto-assigned based on days since last donation |
| 🗺️ **Live Donor Map** | Leaflet.js + OpenStreetMap + Nominatim geocoding — no API key required, shows real donor/hospital pins and distance in km |
| 🩸 **Per-Group Inventory Tracker** | Auto-updating stock levels (Good/Medium/Low) for all 8 blood groups, per hospital |
| 📄 **Auto-Generated PDF Confirmations** | Matched-donor lists with eligibility status, generated via OpenPDF |
| 📱 **SMS Alerts** | One-click urgent-need SMS to matched donors via Twilio |
| 🔄 **Idempotent DB Reset** | Clean auto-increment reset on startup for consistent demo state |

---

## 🆚 Why This Project?

### 📊 Typical Student Blood-Donor Projects vs. This System

| Capability | Typical Donor App | **This System** |
|---|---|---|
| **Input Method** | Static dropdowns & forms only | ✨ Free-text NLP extraction **+** manual form fallback |
| **Language Support** | English only | 🗣️ Tamil + English, chatbot and voice |
| **Eligibility Logic** | Manual staff judgment | 🩺 Automatic 90-day rule-based Eligible/Not Eligible engine |
| **Matching** | Simple SQL filter | 🧠 Blood group + eligibility + real geocoded distance |
| **Map** | None | 🗺️ Live Leaflet map with donor/hospital pins and km distance |
| **Engagement** | None | 🏅 Time-based Bronze/Silver/Gold recognition badges |
| **Inventory** | Manual only | 📊 Auto-incrementing, auto-graded (Good/Medium/Low) per blood group |
| **Documentation Output** | Screenshots only | 📄 Auto-generated confidential PDF confirmations |
| **Deployment** | Local only | 🐳 Docker + docker-compose, cloud-ready |

### 🎯 Key Differentiators

- **Dual-Mode AI** — the same Gemini model is prompt-engineered for two distinct jobs: open conversational Q&A (chatbot) and strict structured JSON extraction (hospital requests) — demonstrating range in prompt design, not just a single wrapper.
- **Grounded, Not Guessed** — eligibility, badges, and inventory status are all deterministic, rule-based computations (not LLM guesses), while the LLM is reserved for language understanding and generation tasks it's actually good at.
- **Real-World Emergency Use Case** — unlike generic to-do or chatbot demos, this addresses actual blood-shortage coordination failures: donor awareness gaps, language barriers, and slow manual matching during time-critical emergencies.

---

## 🏗️ System Architecture

### High-Level Component Diagram

```
graph TB
    A[🌐 Donor Portal - index.html] --> B[Spring Boot REST API]
    C[🏥 Hospital Dashboard - hospital.html] --> B
    B --> D[🧠 Prompt Engineering Layer]
    D --> E[Google Gemini 2.5 Flash]
    B --> F[(MySQL - blood_db)]
    B --> G[🗺️ Leaflet.js + Nominatim]
    B --> H[📱 Twilio SMS]
    B --> I[📄 OpenPDF Generator]

    style A fill:#0088CC
    style C fill:#0088CC
    style B fill:#4A90E2
    style D fill:#7C3AED
    style E fill:#16A34A
    style F fill:#DC2626
```

*Two-portal architecture: **Donor Portal ⇄ Spring Boot API ⇄ (Prompt Layer → Gemini) ⇄ MySQL**, with map, SMS, and PDF as supporting services.*

### 🔍 Layer Responsibilities

| Layer | Purpose | Key Technologies | Output |
|---|---|---|---|
| **🌐 Presentation** | Donor & hospital-facing UI | HTML, CSS, JavaScript | User interactions |
| **🧠 Prompt Engineering** | Builds system prompts, few-shot examples, language rules | `PromptBuilder.java` | Structured request to LLM |
| **🤖 LLM** | Conversational reasoning + structured extraction | Google Gemini 2.5 Flash | Chat replies / JSON |
| **⚖️ Matching & Rules** | Eligibility, badges, inventory status | `DonorMatchingService`, `BadgeService`, `InventoryService` | Deterministic business logic |
| **🗺️ Geospatial** | Donor/hospital distance and mapping | Leaflet.js, Nominatim, Haversine formula | Map pins, km distance |
| **💾 Persistence** | Donor, hospital, inventory, chat, request data | MySQL 8 + Spring Data JPA | Durable state |

---

## 💬 Request Walkthrough

**Donor Chatbot:**
```
Donor  →  "naan tattoo pannitu 2 matham aagudhu, blood kudukalama?"
Bot    →  🗣️ (Tamil detected — responds in Tamil)
           "இல்லை, tattoo pண்ணின பிறகு குறைந்தது 6 மாதங்கள் காத்திருக்க வேண்டும்.
            தற்போது தகுதி இல்லை. மருத்துவரிடம் ஆலோசனை பெறவும்."
```

**Hospital Urgent Request (AI Extraction):**
```
Staff  →  "O-negative venum, 2 units, City Hospital-ku urgent-a"
System →  🧠 Gemini structured extraction:
           { "bloodGroup": "O-", "units": 2, "urgency": "high", "location": "City Hospital" }
Staff  →  Confirm & Match
System →  🗺️ Matches eligible O- donors, shows distance in km, generates PDF
```

---

## 🏛️ Prompt Engineering Layer

Two distinct, purpose-built prompts drive the same underlying Gemini model:

**1. Conversational Eligibility Assistant** — bilingual, rule-grounded, tone-controlled, JSON-wrapped output with a `language` field so the frontend knows which voice to use.

**2. Structured Data Extractor** — zero-explanation, schema-locked JSON output for hospital free-text requests, with sensible defaults when a field is missing (e.g., `units: 1`, `urgency: "medium"`).

This dual-prompt design is the core "prompt engineering" demonstration of the project — the same model, two very different behavioral contracts, each enforced entirely through prompt design rather than separate fine-tuned models.

---

## 🧠 Structured Extraction Engine

| Slot | Raw Staff Input | Extracted Value | Confidence Handling |
|---|---|---|---|
| `bloodGroup` | "O-negative venum" | `O-` | Directly mapped from natural language |
| `units` | "2 units" | `2` | Parsed numeric value, defaults to 1 if absent |
| `urgency` | "urgent-a" | `high` | Mapped from colloquial phrasing |
| `location` | "City Hospital-ku" | `City Hospital` | Extracted entity |

A **manual entry form** (Name, Blood Group, City, Level) sits alongside the AI path as a fallback — both converge on the same internal `BloodRequest` object, so hospital staff are never blocked if the AI path is unavailable (e.g., during a rate limit).

---

## 🗺️ Location & Matching Engine

- **Geocoding:** Nominatim (OpenStreetMap) — free, no API key, converts donor/hospital addresses to lat/lng
- **Map Rendering:** Leaflet.js + OpenStreetMap tiles
- **Distance Calculation:** Haversine formula computed server-side in Java — no external distance API dependency
- **Matching Criteria:** blood group compatibility + Eligible status (90+ days since last donation, or never donated) + proximity

---

## 🏅 Badge & Eligibility Logic

| Days Since Last Donation | Eligibility Status | Badge |
|---|---|---|
| 0–59 days | Not Eligible | 🥉 Bronze |
| 60–89 days | Not Eligible | 🥈 Silver |
| 90+ days (or never donated) | **Eligible** | 🥇 Gold |

Both indicators are computed deterministically from `last_donation_date` — no LLM involvement, ensuring consistent, auditable status at all times.

---

## 🚀 Quick Start

**1 · Prerequisites**
```
Java 17+ (JDK), MySQL 8.0, Maven (bundled via mvnw)
```

**2 · Clone & configure**
```
git clone <your-repo-url>
cd bloodapp
```
Create the database:
```sql
CREATE DATABASE blood_db;
```
Set your credentials and API keys as environment variables (recommended) rather than hardcoding them in `application.properties`:
```
LLM_API_KEY=your_gemini_api_key
TWILIO_ACCOUNT_SID=your_twilio_sid
TWILIO_AUTH_TOKEN=your_twilio_token
TWILIO_FROM_NUMBER=your_twilio_number
```

**3 · Run locally**
```
.\mvnw.cmd clean spring-boot:run
```
App starts at `http://localhost:8081`

**4 · Access the portals**
```
Donor Portal:      http://localhost:8081/index.html
People Portal:     http://localhost:8081/people.html
Hospital Dashboard: http://localhost:8081/hospital.html
```

**5 · Docker (optional)**
```
docker build -t bloodapp .
docker run -d -p 8081:8081 --env-file .env bloodapp
```

---

## ⚙️ Configuration

| Variable | Purpose |
|---|---|
| `LLM_API_KEY` | Google Gemini API key (get from [aistudio.google.com](https://aistudio.google.com/app/apikey)) |
| `llm.provider` | Set to `gemini` |
| `TWILIO_ACCOUNT_SID` / `TWILIO_AUTH_TOKEN` / `TWILIO_FROM_NUMBER` | Twilio SMS credentials |
| `spring.datasource.url` | MySQL JDBC URL (`blood_db`) |
| `server.port` | Defaults to `8081` |

> ⚠️ **Security note:** never commit real API keys or Twilio credentials into `application.properties` or version control. Use environment variables or a `.env` file (excluded via `.gitignore`), and rotate any key that may have been previously exposed.

---

## 📊 Database Schema (Key Tables)

| Table | Purpose |
|---|---|
| `donor` | Donor profile, location, blood group, eligibility fields, badge level |
| `hospital` | Hospital identity and geocoded location |
| `donation_history` | Historical donation records per donor |
| `blood_request` | Hospital emergency requests (AI-extracted or manual) |
| `blood_inventory` | Per-blood-group stock levels per hospital |
| `chat_log` | Chatbot conversation history (message, reply, language) |

---

## 🛡️ Security & Reliability Notes

- Gemini API calls include rate-limit handling — on a `429 Too Many Requests`, the system falls back to a local response rather than crashing.
- Twilio SMS failures surface a clear on-screen error rather than failing silently.
- PDF confirmations are intended for hospital-staff access only, not public download.

---

## 🗺️ Roadmap

| Stage | Status | Description |
|---|---|---|
| 1 | ✅ Done | Core donor registration, hospital dashboard, city search |
| 2 | ✅ Done | Bilingual chatbot + prompt engineering layer |
| 3 | ✅ Done | Structured hospital-request extraction + manual form fallback |
| 4 | ✅ Done | Donor matching, eligibility engine, badge system |
| 5 | ✅ Done | Leaflet map, inventory tracker, PDF generation, SMS alerts |
| 6 | ✅ Done | Docker containerization (`Dockerfile` + `docker-compose.yml`) — satisfies the "Deploy via AWS/Azure/Docker" requirement |
| 7 | 🔲 Planned (optional stretch, not required) | RAG-grounded chatbot answers over official donation guidelines |
| 8 | 🔲 Planned (optional stretch, not required) | Cloud hosting (Azure/AWS) with a managed MySQL instance, for a public live URL instead of localhost |

---

## 📁 Project Structure

```
bloodapp/
├── src/main/java/com/hospital/bloodapp/
│   ├── BloodappApplication.java
│   ├── controller/
│   │   ├── ChatController.java
│   │   ├── DonorController.java
│   │   └── HospitalController.java
│   ├── model/
│   │   ├── Donor.java · Hospital.java · BloodRequest.java
│   │   ├── BloodInventory.java · DonationHistory.java · ChatLog.java
│   ├── repository/            Spring Data JPA repositories
│   ├── runner/
│   │   └── DatabaseSeeder.java
│   └── service/
│       ├── LlmService.java            Gemini API integration
│       ├── PromptBuilder.java         Prompt engineering layer
│       ├── RequestParserService.java  Structured extraction
│       ├── DonorMatchingService.java  Eligibility + matching
│       ├── GeocodingService.java      Nominatim integration
│       ├── InventoryService.java      Stock auto-update logic
│       ├── BadgeService.java          Badge tier computation
│       ├── BloodExpiryScheduler.java  Shelf-life expiry job
│       ├── NotificationService.java   Twilio SMS
│       ├── PdfGeneratorService.java   OpenPDF confirmations
│       ├── ReminderService.java       Eligibility reminders
│       └── RagService.java            (optional) guideline retrieval
├── src/main/resources/
│   ├── application.properties
│   ├── guidelines.txt
│   └── static/
│       ├── index.html      Donor portal
│       ├── people.html     People registration
│       └── hospital.html   Hospital admin dashboard
├── Dockerfile
└── pom.xml
```

---

## 👥 Meet the Team

*Add your team members' names and roles here, following the same format as the reference project.*

---

## 🛠️ Built With

[Spring Boot](https://spring.io/projects/spring-boot) · [Google Gemini](https://ai.google.dev) · [MySQL](https://www.mysql.com) · [Leaflet.js](https://leafletjs.com) · [OpenStreetMap](https://www.openstreetmap.org) · [Twilio](https://www.twilio.com) · [OpenPDF](https://github.com/LibrePDF/OpenPDF)

---

Made with ❤️ for a better blood donation ecosystem.# 🩸 Blood Donor Management System

## AI-Powered · Bilingual · Location-Aware Donor–Hospital Coordination Platform

**Real-Time · Prompt-Engineered · Deployable**

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com)
[![Gemini](https://img.shields.io/badge/Gemini_2.5_Flash-4285F4?style=for-the-badge&logo=googlegemini&logoColor=white)](https://ai.google.dev)
[![Leaflet](https://img.shields.io/badge/Leaflet.js-199900?style=for-the-badge&logo=leaflet&logoColor=white)](https://leafletjs.com)
[![Twilio](https://img.shields.io/badge/Twilio-F22F46?style=for-the-badge&logo=twilio&logoColor=white)](https://www.twilio.com)

> **Blood Donor Management System** is a full-stack, AI-enhanced platform that connects blood donors and hospitals in real time.
> It combines a bilingual (Tamil + English) prompt-engineered LLM assistant, structured emergency-request extraction,
> automated donor matching with live map visualization, and gamified donor engagement — all built on
> **Spring Boot, MySQL, and Google Gemini.**

[📖 Documentation](#-table-of-contents) · [🚀 Quick Start](#-quick-start) · [🎯 Features](#-key-features) · [🏗️ Architecture](#️-system-architecture) · [👥 Team](#-meet-the-team)

---

## 📑 Table of Contents

| Core Sections | Technical Deep Dives | Resources |
|---|---|---|
| [🎯 Key Features](#-key-features) | [🏛️ Prompt Engineering Layer](#️-prompt-engineering-layer) | [⚙️ Configuration](#️-configuration) |
| [🆚 Why This Project?](#-why-this-project) | [🧠 Structured Extraction](#-structured-extraction-engine) | [📊 Database Schema](#-database-schema) |
| [🏗️ System Architecture](#️-system-architecture) | [🗺️ Location & Matching](#️-location--matching-engine) | [🗺️ Roadmap](#️-roadmap) |
| [💬 Request Walkthrough](#-request-walkthrough) | [🏅 Badge & Eligibility Logic](#-badge--eligibility-logic) | [👥 Meet the Team](#-meet-the-team) |
| [🚀 Quick Start](#-quick-start) | [🛡️ Security & Reliability Notes](#️-security--reliability-notes) | [📁 Project Structure](#-project-structure) |

---

## 🎯 Key Features

| Feature | Description |
|---|---|
| 🤖 **Bilingual AI Chatbot** | Tamil + English donor eligibility assistant, powered by Gemini 2.5 Flash with a dedicated prompt-engineering layer |
| 🎙️ **Voice Interaction** | Manual mic input + "Listen" playback, both Tamil (`ta-IN`) and English (`en-IN`) |
| 🧠 **Structured Emergency Extraction** | Converts free-text hospital requests into structured JSON (blood group, units, urgency, location) — plus a manual entry form as a fallback |
| 🩺 **Eligibility Engine** | Every donor is automatically labeled **Eligible** / **Not Eligible** based on a 90-day donation cycle |
| 🏅 **Time-Based Recognition Badges** | Bronze → Silver → Gold badge auto-assigned based on days since last donation |
| 🗺️ **Live Donor Map** | Leaflet.js + OpenStreetMap + Nominatim geocoding — no API key required, shows real donor/hospital pins and distance in km |
| 🩸 **Per-Group Inventory Tracker** | Auto-updating stock levels (Good/Medium/Low) for all 8 blood groups, per hospital |
| 📄 **Auto-Generated PDF Confirmations** | Matched-donor lists with eligibility status, generated via OpenPDF |
| 📱 **SMS Alerts** | One-click urgent-need SMS to matched donors via Twilio |
| 🔄 **Idempotent DB Reset** | Clean auto-increment reset on startup for consistent demo state |

---

## 🆚 Why This Project?

### 📊 Typical Student Blood-Donor Projects vs. This System

| Capability | Typical Donor App | **This System** |
|---|---|---|
| **Input Method** | Static dropdowns & forms only | ✨ Free-text NLP extraction **+** manual form fallback |
| **Language Support** | English only | 🗣️ Tamil + English, chatbot and voice |
| **Eligibility Logic** | Manual staff judgment | 🩺 Automatic 90-day rule-based Eligible/Not Eligible engine |
| **Matching** | Simple SQL filter | 🧠 Blood group + eligibility + real geocoded distance |
| **Map** | None | 🗺️ Live Leaflet map with donor/hospital pins and km distance |
| **Engagement** | None | 🏅 Time-based Bronze/Silver/Gold recognition badges |
| **Inventory** | Manual only | 📊 Auto-incrementing, auto-graded (Good/Medium/Low) per blood group |
| **Documentation Output** | Screenshots only | 📄 Auto-generated confidential PDF confirmations |
| **Deployment** | Local only | 🐳 Docker + docker-compose, cloud-ready |

### 🎯 Key Differentiators

- **Dual-Mode AI** — the same Gemini model is prompt-engineered for two distinct jobs: open conversational Q&A (chatbot) and strict structured JSON extraction (hospital requests) — demonstrating range in prompt design, not just a single wrapper.
- **Grounded, Not Guessed** — eligibility, badges, and inventory status are all deterministic, rule-based computations (not LLM guesses), while the LLM is reserved for language understanding and generation tasks it's actually good at.
- **Real-World Emergency Use Case** — unlike generic to-do or chatbot demos, this addresses actual blood-shortage coordination failures: donor awareness gaps, language barriers, and slow manual matching during time-critical emergencies.

---

## 🏗️ System Architecture

### High-Level Component Diagram

```
graph TB
    A[🌐 Donor Portal - index.html] --> B[Spring Boot REST API]
    C[🏥 Hospital Dashboard - hospital.html] --> B
    B --> D[🧠 Prompt Engineering Layer]
    D --> E[Google Gemini 2.5 Flash]
    B --> F[(MySQL - blood_db)]
    B --> G[🗺️ Leaflet.js + Nominatim]
    B --> H[📱 Twilio SMS]
    B --> I[📄 OpenPDF Generator]

    style A fill:#0088CC
    style C fill:#0088CC
    style B fill:#4A90E2
    style D fill:#7C3AED
    style E fill:#16A34A
    style F fill:#DC2626
```

*Two-portal architecture: **Donor Portal ⇄ Spring Boot API ⇄ (Prompt Layer → Gemini) ⇄ MySQL**, with map, SMS, and PDF as supporting services.*

### 🔍 Layer Responsibilities

| Layer | Purpose | Key Technologies | Output |
|---|---|---|---|
| **🌐 Presentation** | Donor & hospital-facing UI | HTML, CSS, JavaScript | User interactions |
| **🧠 Prompt Engineering** | Builds system prompts, few-shot examples, language rules | `PromptBuilder.java` | Structured request to LLM |
| **🤖 LLM** | Conversational reasoning + structured extraction | Google Gemini 2.5 Flash | Chat replies / JSON |
| **⚖️ Matching & Rules** | Eligibility, badges, inventory status | `DonorMatchingService`, `BadgeService`, `InventoryService` | Deterministic business logic |
| **🗺️ Geospatial** | Donor/hospital distance and mapping | Leaflet.js, Nominatim, Haversine formula | Map pins, km distance |
| **💾 Persistence** | Donor, hospital, inventory, chat, request data | MySQL 8 + Spring Data JPA | Durable state |

---

## 💬 Request Walkthrough

**Donor Chatbot:**
```
Donor  →  "naan tattoo pannitu 2 matham aagudhu, blood kudukalama?"
Bot    →  🗣️ (Tamil detected — responds in Tamil)
           "இல்லை, tattoo pண்ணின பிறகு குறைந்தது 6 மாதங்கள் காத்திருக்க வேண்டும்.
            தற்போது தகுதி இல்லை. மருத்துவரிடம் ஆலோசனை பெறவும்."
```

**Hospital Urgent Request (AI Extraction):**
```
Staff  →  "O-negative venum, 2 units, City Hospital-ku urgent-a"
System →  🧠 Gemini structured extraction:
           { "bloodGroup": "O-", "units": 2, "urgency": "high", "location": "City Hospital" }
Staff  →  Confirm & Match
System →  🗺️ Matches eligible O- donors, shows distance in km, generates PDF
```

---

## 🏛️ Prompt Engineering Layer

Two distinct, purpose-built prompts drive the same underlying Gemini model:

**1. Conversational Eligibility Assistant** — bilingual, rule-grounded, tone-controlled, JSON-wrapped output with a `language` field so the frontend knows which voice to use.

**2. Structured Data Extractor** — zero-explanation, schema-locked JSON output for hospital free-text requests, with sensible defaults when a field is missing (e.g., `units: 1`, `urgency: "medium"`).

This dual-prompt design is the core "prompt engineering" demonstration of the project — the same model, two very different behavioral contracts, each enforced entirely through prompt design rather than separate fine-tuned models.

---

## 🧠 Structured Extraction Engine

| Slot | Raw Staff Input | Extracted Value | Confidence Handling |
|---|---|---|---|
| `bloodGroup` | "O-negative venum" | `O-` | Directly mapped from natural language |
| `units` | "2 units" | `2` | Parsed numeric value, defaults to 1 if absent |
| `urgency` | "urgent-a" | `high` | Mapped from colloquial phrasing |
| `location` | "City Hospital-ku" | `City Hospital` | Extracted entity |

A **manual entry form** (Name, Blood Group, City, Level) sits alongside the AI path as a fallback — both converge on the same internal `BloodRequest` object, so hospital staff are never blocked if the AI path is unavailable (e.g., during a rate limit).

---

## 🗺️ Location & Matching Engine

- **Geocoding:** Nominatim (OpenStreetMap) — free, no API key, converts donor/hospital addresses to lat/lng
- **Map Rendering:** Leaflet.js + OpenStreetMap tiles
- **Distance Calculation:** Haversine formula computed server-side in Java — no external distance API dependency
- **Matching Criteria:** blood group compatibility + Eligible status (90+ days since last donation, or never donated) + proximity

---

## 🏅 Badge & Eligibility Logic

| Days Since Last Donation | Eligibility Status | Badge |
|---|---|---|
| 0–59 days | Not Eligible | 🥉 Bronze |
| 60–89 days | Not Eligible | 🥈 Silver |
| 90+ days (or never donated) | **Eligible** | 🥇 Gold |

Both indicators are computed deterministically from `last_donation_date` — no LLM involvement, ensuring consistent, auditable status at all times.

---

## 🚀 Quick Start

**1 · Prerequisites**
```
Java 17+ (JDK), MySQL 8.0, Maven (bundled via mvnw)
```

**2 · Clone & configure**
```
git clone <your-repo-url>
cd bloodapp
```
Create the database:
```sql
CREATE DATABASE blood_db;
```
Set your credentials and API keys as environment variables (recommended) rather than hardcoding them in `application.properties`:
```
LLM_API_KEY=your_gemini_api_key
TWILIO_ACCOUNT_SID=your_twilio_sid
TWILIO_AUTH_TOKEN=your_twilio_token
TWILIO_FROM_NUMBER=your_twilio_number
```

**3 · Run locally**
```
.\mvnw.cmd clean spring-boot:run
```
App starts at `http://localhost:8081`

**4 · Access the portals**
```
Donor Portal:      http://localhost:8081/index.html
People Portal:     http://localhost:8081/people.html
Hospital Dashboard: http://localhost:8081/hospital.html
```

**5 · Docker (optional)**
```
docker build -t bloodapp .
docker run -d -p 8081:8081 --env-file .env bloodapp
```

---

## ⚙️ Configuration

| Variable | Purpose |
|---|---|
| `LLM_API_KEY` | Google Gemini API key (get from [aistudio.google.com](https://aistudio.google.com/app/apikey)) |
| `llm.provider` | Set to `gemini` |
| `TWILIO_ACCOUNT_SID` / `TWILIO_AUTH_TOKEN` / `TWILIO_FROM_NUMBER` | Twilio SMS credentials |
| `spring.datasource.url` | MySQL JDBC URL (`blood_db`) |
| `server.port` | Defaults to `8081` |

> ⚠️ **Security note:** never commit real API keys or Twilio credentials into `application.properties` or version control. Use environment variables or a `.env` file (excluded via `.gitignore`), and rotate any key that may have been previously exposed.

---

## 📊 Database Schema (Key Tables)

| Table | Purpose |
|---|---|
| `donor` | Donor profile, location, blood group, eligibility fields, badge level |
| `hospital` | Hospital identity and geocoded location |
| `donation_history` | Historical donation records per donor |
| `blood_request` | Hospital emergency requests (AI-extracted or manual) |
| `blood_inventory` | Per-blood-group stock levels per hospital |
| `chat_log` | Chatbot conversation history (message, reply, language) |

---

## 🛡️ Security & Reliability Notes

- Gemini API calls include rate-limit handling — on a `429 Too Many Requests`, the system falls back to a local response rather than crashing.
- Twilio SMS failures surface a clear on-screen error rather than failing silently.
- PDF confirmations are intended for hospital-staff access only, not public download.

---

## 🗺️ Roadmap

| Stage | Status | Description |
|---|---|---|
| 1 | ✅ Done | Core donor registration, hospital dashboard, city search |
| 2 | ✅ Done | Bilingual chatbot + prompt engineering layer |
| 3 | ✅ Done | Structured hospital-request extraction + manual form fallback |
| 4 | ✅ Done | Donor matching, eligibility engine, badge system |
| 5 | ✅ Done | Leaflet map, inventory tracker, PDF generation, SMS alerts |
| 6 | ✅ Done | Docker containerization (`Dockerfile` + `docker-compose.yml`) — satisfies the "Deploy via AWS/Azure/Docker" requirement |
| 7 | 🔲 Planned (optional stretch, not required) | RAG-grounded chatbot answers over official donation guidelines |
| 8 | 🔲 Planned (optional stretch, not required) | Cloud hosting (Azure/AWS) with a managed MySQL instance, for a public live URL instead of localhost |

---

## 📁 Project Structure

```
bloodapp/
├── src/main/java/com/hospital/bloodapp/
│   ├── BloodappApplication.java
│   ├── controller/
│   │   ├── ChatController.java
│   │   ├── DonorController.java
│   │   └── HospitalController.java
│   ├── model/
│   │   ├── Donor.java · Hospital.java · BloodRequest.java
│   │   ├── BloodInventory.java · DonationHistory.java · ChatLog.java
│   ├── repository/            Spring Data JPA repositories
│   ├── runner/
│   │   └── DatabaseSeeder.java
│   └── service/
│       ├── LlmService.java            Gemini API integration
│       ├── PromptBuilder.java         Prompt engineering layer
│       ├── RequestParserService.java  Structured extraction
│       ├── DonorMatchingService.java  Eligibility + matching
│       ├── GeocodingService.java      Nominatim integration
│       ├── InventoryService.java      Stock auto-update logic
│       ├── BadgeService.java          Badge tier computation
│       ├── BloodExpiryScheduler.java  Shelf-life expiry job
│       ├── NotificationService.java   Twilio SMS
│       ├── PdfGeneratorService.java   OpenPDF confirmations
│       ├── ReminderService.java       Eligibility reminders
│       └── RagService.java            (optional) guideline retrieval
├── src/main/resources/
│   ├── application.properties
│   ├── guidelines.txt
│   └── static/
│       ├── index.html      Donor portal
│       ├── people.html     People registration
│       └── hospital.html   Hospital admin dashboard
├── Dockerfile
└── pom.xml
```

---

## 👥 Meet the Team

*Add your team members' names and roles here, following the same format as the reference project.*

---

## 🛠️ Built With

[Spring Boot](https://spring.io/projects/spring-boot) · [Google Gemini](https://ai.google.dev) · [MySQL](https://www.mysql.com) · [Leaflet.js](https://leafletjs.com) · [OpenStreetMap](https://www.openstreetmap.org) · [Twilio](https://www.twilio.com) · [OpenPDF](https://github.com/LibrePDF/OpenPDF)

---

Made with ❤️ for a better blood donation ecosystem.