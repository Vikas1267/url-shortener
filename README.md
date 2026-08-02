# ⚡ Snipr — High-Performance URL Shortener & Analytics API

[![Java 17](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15%2B-blue.svg)](https://www.postgresql.org/)
[![Flyway](https://img.shields.io/badge/Flyway-Migrations-red.svg)](https://flywaydb.org/)
[![Swagger](https://img.shields.io/badge/OpenAPI-3.0-green.svg)](http://localhost:8080/swagger-ui.html)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**Snipr** is a production-grade Spring Boot 3 & PostgreSQL URL Shortener REST API engineered for speed, mathematical reliability, and deep click analytics. Built with deterministic **Base62 primary key encoding** for 0-collision short code generation, Flyway versioned schema migrations, non-blocking asynchronous click analytics logging, and interactive OpenAPI / Swagger UI documentation.

---

## 📸 System Architecture

```text
                                  Client (Browser / Postman / App)
                                                 │
                   ┌─────────────────────────────┼─────────────────────────────┐
                   ▼                             ▼                             ▼
         POST /api/shorten               GET /{shortCode}              GET /api/stats/{code}
                   │                             │                             │
                   ▼                             ▼                             ▼
          [ShortenController]           [RedirectController]           [StatsController]
                   │                             │                             │
                   ▼                             │                             ▼
            [UrlService]                         │                      [StatsService]
      (Base62 id+offset encoding)                │                     (SQL Aggregations)
                   │                             │                             │
                   ▼                             ├────────────────────────┐    │
           [UrlRepository]                       │                        │    │
                   │                             ▼                        ▼    │
                   │                    HTTP 302 Redirect        [Async Click Task Executor]
                   │                    (Location: longUrl)       (Non-blocking Thread Pool)
                   │                             │                        │
                   ▼                             │                        ▼
            ┌──────────────┐                     │               ┌──────────────────┐
            │  urls Table  │◄────────────────────┴───────────────┤   clicks Table   │
            └──────────────┘                                     └──────────────────┘
                                        PostgreSQL
```

---

## ✨ Key Features

- 🔒 **Deterministic Base62 Encoding**: $O(1)$ mathematical bijection (`id + offset` $\rightarrow$ `shortCode`). Zero database retry loops or collision race conditions under heavy write load.
- ⚡ **Non-Blocking Asynchronous Click Logging**: Click events (IP address, User-Agent, Referrer, timestamp) are dispatched to a dedicated `ThreadPoolTaskExecutor` pool (`click-log-`), ensuring redirects execute instantly.
- 🛠️ **Dual-Mode Resolution**:
  - `GET /{shortCode}` $\rightarrow$ Returns HTTP `302 Found` for web browser redirects.
  - `GET /api/expand/{shortCode}` or `GET /{shortCode}?redirect=false` $\rightarrow$ Returns JSON payload without redirecting (ideal for API clients & Swagger UI testing).
- 📜 **Flyway Database Migrations**: Version-controlled SQL schema history (`V1__create_urls_table.sql`, `V2__create_clicks_table.sql`) with optimized unique and composite index strategies.
- 🎯 **Strict HTTP Status Semantics**: Centralized `@RestControllerAdvice` mapping exceptions to standard status codes (`201`, `302`, `400`, `404`, `410`, `422`, `500`).
- 🧪 **Enterprise Test Suite**: 100% test coverage including unit tests, `@WebMvcTest` web layer mocks, and integration tests using **Testcontainers** with real PostgreSQL containers.

---

## 🚀 Quick Start

### Prerequisites
- **Java 17+** installed (`java -version`)
- **Docker & Docker Compose** installed (`docker compose version`)

### 1. Clone & Setup Environment

```bash
git clone https://github.com/YOUR_USERNAME/url-shortener.git
cd url-shortener
```

Create `.env` file from template:
- **Linux / macOS**:
  ```bash
  cp .env.example .env
  ```
- **Windows Command Prompt (`cmd.exe`)**:
  ```cmd
  copy .env.example .env
  ```
- **Windows PowerShell**:
  ```powershell
  Copy-Item .env.example .env
  ```

### 2. Start PostgreSQL Container

```bash
docker compose up -d postgres
```

### 3. Run the Application

- **Linux / macOS**:
  ```bash
  ./mvnw spring-boot:run
  ```
- **Windows (`cmd.exe` or PowerShell)**:
  ```cmd
  .\mvnw.cmd spring-boot:run
  ```

The API will start at **`http://localhost:8080`**.  
Interactive **Swagger UI** will be available at **`http://localhost:8080/swagger-ui.html`**.

---

## 📡 API Reference & Examples

### 1. Shorten a Long URL
**`POST /api/shorten`**

```bash
curl -i -X POST http://localhost:8080/api/shorten \
  -H "Content-Type: application/json" \
  -d '{"longUrl":"https://example.com/some/very/long/path?query=1"}'
```

**Response (`201 Created`)**:
```json
{
  "shortCode": "q0V",
  "shortUrl": "http://localhost:8080/q0V"
}
```

---

### 2. Redirect to Target URL
**`GET /{shortCode}`**

```bash
curl -i http://localhost:8080/q0V
```

**Response (`302 Found`)**:
```http
HTTP/1.1 302 Found
Location: https://example.com/some/very/long/path?query=1
Content-Length: 0
```

---

### 3. Expand a Short Code (JSON Response)
**`GET /api/expand/{shortCode}`** or **`GET /{shortCode}?redirect=false`**

```bash
curl -s http://localhost:8080/api/expand/q0V
```

**Response (`200 OK`)**:
```json
{
  "shortCode": "q0V",
  "longUrl": "https://example.com/some/very/long/path?query=1"
}
```

---

### 4. Get Click Analytics
**`GET /api/stats/{shortCode}`**

```bash
curl -s http://localhost:8080/api/stats/q0V
```

**Response (`200 OK`)**:
```json
{
  "shortCode": "q0V",
  "totalClicks": 42,
  "clicksPerDay": [
    {
      "date": "2026-08-02",
      "count": 42
    }
  ],
  "referrers": [
    {
      "referrer": "direct",
      "count": 30
    },
    {
      "referrer": "https://twitter.com",
      "count": 12
    }
  ]
}
```

---

## 📑 Testing in Postman & Swagger UI

### Testing in Postman
1. Open Postman $\rightarrow$ Click **Import** $\rightarrow$ paste `http://localhost:8080/v3/api-docs` to auto-import all endpoints.
2. To test `302 Found` redirects in Postman:
   - Go to request **Settings** tab $\rightarrow$ Turn OFF **Automatically follow redirects**.
   - Send `GET http://localhost:8080/q0V`. Postman will display `302 Found` and the `Location` header in the bottom `Headers` panel!

### Testing in Swagger UI
Visit `http://localhost:8080/swagger-ui.html`:
- Use `POST /api/shorten` to create short URLs.
- Use `GET /api/expand/{shortCode}` or `GET /{shortCode}?redirect=false` to test resolution directly inside Swagger UI without browser CORS redirect blocks.

---

## 🗄️ Database Schema & Indexing

### `urls` Table
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Auto-increment ID used for Base62 encoding |
| `short_code` | `VARCHAR(10)` | `NOT NULL, UNIQUE` | Base62 code representation |
| `long_url` | `TEXT` | `NOT NULL` | Destination long URL |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL, DEFAULT now()` | UTC creation timestamp |
| `expires_at` | `TIMESTAMPTZ` | `NULLABLE` | Expiration date |
| `is_active` | `BOOLEAN` | `NOT NULL, DEFAULT true` | Active flag |

*Index*: `idx_urls_short_code` (Unique index backing $O(1)$ redirect lookups).

### `clicks` Table
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Auto-increment click event ID |
| `url_id` | `BIGINT` | `FOREIGN KEY (urls.id)` | FK linking to parent URL |
| `clicked_at` | `TIMESTAMPTZ` | `NOT NULL, DEFAULT now()` | UTC timestamp of click |
| `referrer` | `TEXT` | `NULLABLE` | HTTP `Referer` header |
| `ip_address` | `INET` | `NULLABLE` | Client IP address (`X-Forwarded-For` / `RemoteAddr`) |
| `user_agent` | `TEXT` | `NULLABLE` | HTTP `User-Agent` header |

*Indices*: `idx_clicks_url_id` and `idx_clicks_url_id_clicked_at` (Composite index for rapid date-ranged analytics queries).

---

## 🧪 Running Unit & Integration Tests

Run the full test suite (includes **Testcontainers** for isolated PostgreSQL container integration testing):

- **Linux / macOS**:
  ```bash
  ./mvnw test
  ```
- **Windows**:
  ```cmd
  .\mvnw.cmd test
  ```

---

## 📁 Project Directory Structure

```text
url-shortener/
├── docker-compose.yml                        # Docker Postgres database service
├── pom.xml                                   # Maven dependencies & build plugins
├── README.md                                 # Project documentation
├── requests.http                             # JetBrains HTTP Client test requests
├── docs/                                     # System Specifications
│   ├── PRD.md                                # Product Requirements Document
│   ├── TRS.md                                # Technical Requirements Specification
│   └── design.md                             # Architectural tradeoffs & decisions
└── src/
    ├── main/
    │   ├── java/com/om/urlshortener/
    │   │   ├── config/AppConfig.java         # CORS, ThreadPoolTaskExecutor & OpenAPI Info
    │   │   ├── controller/                   # REST Controllers (Shorten, Redirect, Stats)
    │   │   ├── dto/                          # DTO Records (Shorten, Stats, Resolve)
    │   │   ├── entity/                       # JPA Entities (Url, Click)
    │   │   ├── exception/                    # GlobalExceptionHandler & Custom Exceptions
    │   │   ├── repository/                   # Spring Data Repositories
    │   │   ├── service/                      # Business Logic (UrlService, ClickService, StatsService)
    │   │   └── util/Base62.java              # Base62 Encoder / Decoder
    │   └── resources/
    │       ├── application.yml               # App configuration
    │       └── db/migration/                 # Flyway SQL migrations (V1, V2)
    └── test/java/com/om/urlshortener/        # Unit & Testcontainers Integration Tests
```

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
