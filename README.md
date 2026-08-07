# restaurant-pos-reservations

Enterprise restaurant management system for POS operations and table reservations.
Built as a single Spring Boot **modular monolith**.

**Author:** Martin Andonov Kolev

## Overview

The application covers day-to-day restaurant operations:

- staff authentication and roles
- menu and inventory with recipe-based availability
- waiter order taking with stock deduction
- kitchen workflow with realtime notifications
- table reservations for clients and staff views
- simulated CASH/CARD payments and operational sales reports
- four role-based browser UIs (Admin, Waiter, Kitchen, Client)

REST + MySQL are the source of truth. WebSocket/STOMP messages are notifications only.

## Features

- Session-based Spring Security with CSRF
- Role-isolated Admin / Waiter / Kitchen / Client interfaces
- Menu catalog + automatic availability from recipes/stock
- Order workflow: ACCEPTED → COOKING → READY → SERVED
- Reservation availability, create, reschedule, cancel with conflict checks
- Simulated payments and receipts (not fiscal / not a real PSP)
- Operational sales reports for ADMIN
- Optional `demo` profile for presentation seed data

## Technology stack

- Java 17
- Spring Boot 4.1 (Web MVC, Data JPA, Security, WebSocket)
- Spring Security Messaging (STOMP authorization)
- MySQL 8+
- Maven Wrapper
- HTML5 / CSS3 / vanilla JavaScript (ES modules, Fetch API)

No Node.js, npm, React/Angular/Vue, Docker, H2, Lombok, Flyway, or external payment SDKs in this project.

## Architecture

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

Summary: Browser UIs → REST controllers → services → repositories → MySQL. Order commits publish AFTER_COMMIT events to STOMP topics; UIs refresh via REST.

## Roles

| Role | UI | API |
|---|---|---|
| ADMIN | `/admin` | `/api/admin/**` (+ other operational areas as configured) |
| WAITER | `/waiter` | `/api/waiter/**`, `/operations/**` |
| COOK | `/kitchen` | `/api/kitchen/**`, `/operations/**` |
| CLIENT | `/client` | `/api/client/**` |

Login redirect precedence: ADMIN → WAITER → COOK → CLIENT.

## Main business workflows

1. **Order:** Waiter creates order → stock deducted → table OCCUPIED → kitchen notified → COOKING/READY → Waiter SERVED → payment → order closed → table AVAILABLE.
2. **Reservation:** Client (or admin) books a free table interval → CONFIRMED → optional reschedule → cancel frees the slot.
3. **Availability:** Menu item effective availability = manual flag AND recipe AND active ingredients AND enough stock.

## Database

MySQL 8+ with JPA `ddl-auto=update`. Details and ER diagram: [docs/DATABASE.md](docs/DATABASE.md).

## Security

- BCrypt password hashes
- Session cookies + CSRF for mutating HTTP and STOMP CONNECT
- Role-based HTTP authorization
- STOMP topic authorization; inbound business SEND denied
- Password hashes never returned in API JSON
- Safe API error bodies (no stack traces)

## WebSocket / STOMP

- Endpoint: `/ws` (authenticated)
- Kitchen topic: `/topic/kitchen/orders` (COOK/ADMIN)
- Waiter topic: `/topic/waiter/orders` (WAITER/ADMIN)
- CLIENT has no operational subscriptions
- Notifications are not durable; reconnect recovers with REST

## Simulated payments disclaimer

Payment methods `CASH` and `CARD` are **local simulations**.

`CARD` means:

- a local enum value
- a local MySQL payment row
- **no** card number / CVV / holder
- **no** payment provider, bank authorization, or real money movement

Receipts are operational simulation documents only — **not** a fiscal bon, tax invoice, or bank document.

## Sales reports

ADMIN operational aggregates (summary, by item, by payment method) over paid/closed activity. They are not accounting or tax filings.

## UI routes

| UI | URL | Stack notes |
|---|---|---|
| Admin | `/admin` | Vanilla JS modules; REST only |
| Waiter | `/waiter` | Shared `/operations/**` + STOMP |
| Kitchen | `/kitchen` | Shared `/operations/**` + STOMP |
| Client | `/client` | REST only; no WebSocket |

## Local setup

### Requirements

- Java 17+
- MySQL 8+
- Maven Wrapper (included)

### Environment variables

| Variable | Required | Description |
|---|---|---|
| `DB_URL` | no (has default) | JDBC URL |
| `DB_USERNAME` | no (default `restaurant_app`) | DB user |
| `DB_PASSWORD` | **yes** | DB password |
| `RESTAURANT_TIME_ZONE` | no (default `Europe/Sofia`) | Reservation clock zone |
| `INITIAL_ADMIN_EMAIL` | optional | Seed first ADMIN |
| `INITIAL_ADMIN_PASSWORD` | optional | Seed admin password (BCrypt stored) |
| `INITIAL_ADMIN_FULL_NAME` | optional | Seed admin display name |
| `DEMO_USER_PASSWORD` | demo only | Shared password for demo.* users |

See `src/main/resources/application-example.properties`. Never commit real secrets. Prefer a gitignored `smoke-env.ps1` for local values.

## Running the application

### Windows PowerShell (normal)

```powershell
. .\smoke-env.ps1
.\mvnw.cmd spring-boot:run
```

### Demo profile (presentation)

```powershell
. .\smoke-env.ps1
$env:DEMO_USER_PASSWORD = "<set-locally>"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```

Demo users (created only when `DEMO_USER_PASSWORD` is set):

- `demo.admin@example.com`
- `demo.waiter@example.com`
- `demo.cook@example.com`
- `demo.client@example.com`

Demo catalog/tables use a `DEMO ` prefix and are idempotent. Demo does **not** seed payments or large history. If `DEMO_USER_PASSWORD` is missing, demo users are skipped (safe warning only).

## Running tests

```powershell
.\mvnw.cmd clean test
```

More detail: [docs/TESTING.md](docs/TESTING.md).

## API overview

See [docs/API.md](docs/API.md).

## Known scope limitations / future production work

This academic/demo-scope system does **not** include:

- real payment gateway or fiscal device
- VAT/tax compliance modules
- email/SMS notifications
- password reset / anonymous public booking
- distributed message broker / outbox / WebSocket event replay
- Flyway/Liquibase migrations
- Docker deployment packaging
- production observability / rate limiting
- external identity provider

Do not treat simulated CARD payments or receipts as real financial or fiscal documents.

## Project structure

```text
src/main/java/bg/martinandonov/restaurant/
  common/ security/ user/ menu/ inventory/ diningtable/
  order/ kitchen/ reservation/ payment/ report/ demo/ client/
src/main/resources/static/
  admin/ waiter/ kitchen/ client/ operations/
docs/
  ARCHITECTURE.md DATABASE.md API.md TESTING.md DEMO.md PRESENTATION_QA.md
```

## Presentation / demo instructions

Follow [docs/DEMO.md](docs/DEMO.md) and prepare answers with [docs/PRESENTATION_QA.md](docs/PRESENTATION_QA.md).

## Further documentation

| Doc | Content |
|---|---|
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | Modular monolith, WebSocket flow, concurrency |
| [DATABASE.md](docs/DATABASE.md) | Entities, ER diagram, constraints |
| [API.md](docs/API.md) | Role-oriented API map |
| [TESTING.md](docs/TESTING.md) | Automated + smoke testing |
| [DEMO.md](docs/DEMO.md) | 8–12 minute presentation script |
| [PRESENTATION_QA.md](docs/PRESENTATION_QA.md) | Defense Q&A |