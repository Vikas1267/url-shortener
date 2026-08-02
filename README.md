# Snipr URL Shortener

Snipr is a small Spring Boot + PostgreSQL URL shortener API built to show the boring-but-important engineering choices behind a reliable redirect service: deterministic Base62 codes from database IDs, Flyway-owned schema history, centralized error handling, and click analytics that never break the redirect path.

## Architecture

```text
Client
  |
  v
[Spring Boot App]
  |-- Controller layer   (HTTP in/out, status codes, validation triggers)
  |-- Service layer      (business logic: code generation, click logging, stats aggregation)
  |-- Repository layer   (Spring Data JPA, talks to Postgres)
  `-- (v2) Cache layer   (Redis, cache-aside in front of the urls table)
          |
          v
     [PostgreSQL]
     |-- urls table
     `-- clicks table
```

## Setup

Prerequisites: Java 17+, Docker, and Docker Compose.

```bash
git clone <repo-url>
cd url-shortener
cp .env.example .env
docker compose up -d postgres
./mvnw spring-boot:run
```

On Windows PowerShell, use:

```powershell
Copy-Item .env.example .env
docker compose up -d postgres
.\mvnw.cmd spring-boot:run
```

Swagger UI is available at `http://localhost:8080/swagger-ui.html`.

## Curl Examples

Create a short URL:

```bash
curl -i -X POST http://localhost:8080/api/shorten \
  -H "Content-Type: application/json" \
  -d '{"longUrl":"https://example.com/some/very/long/path?query=1"}'
```

Redirect with the `shortCode` returned by the previous response:

```bash
curl -i http://localhost:8080/q0V
```

Read stats for the same code:

```bash
curl -s http://localhost:8080/api/stats/q0V
```

On a fresh database with the default `app.short-code.offset=100000`, the first generated code is usually `q0V`; otherwise use the `shortCode` from your POST response.

## Engineering Decisions

The main tradeoffs are documented in [docs/design.md](docs/design.md): Base62 over database IDs instead of random generation, `422` for semantic URL validation failures versus `400` for structurally invalid requests, Flyway migrations instead of `ddl-auto: update`, and why Redis is explicitly deferred for v1.

## Tests

```bash
./mvnw test
```

The test suite includes mocked service tests, `@WebMvcTest` controller coverage, and Testcontainers integration tests against real PostgreSQL.
