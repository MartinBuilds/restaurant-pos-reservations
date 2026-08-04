# restaurant-pos-reservations

Enterprise restaurant management system for POS operations and table reservations.
Built as a single Spring Boot modular monolith.

**Author:** Мартин Андонов Колев

## Technologies

- Java 17
- Spring Boot 4.1
- Maven (Maven Wrapper)
- Spring Web
- Spring Data JPA
- Spring Security (session-based authentication)
- WebSocket
- MySQL 8+

## Roles and access

| Role | Access |
|---|---|
| `ADMIN` | `/api/admin/**`, and all other role-restricted areas |
| `WAITER` | `/api/waiter/**` |
| `COOK` | `/api/kitchen/**` |
| `CLIENT` | `/api/client/**` |

Public (no authentication): `/`, `/login`, `/css/**`, `/js/**`, `/images/**`, `/api/public/**`.

All other `/api/**` endpoints require authentication.

Passwords are stored only as BCrypt hashes. Never commit real passwords.

## Modules

| Module | Purpose |
|---|---|
| `common` | Shared exceptions and cross-cutting API concerns |
| `security` | Authentication and authorization |
| `user` | Staff users and role assignment |
| `menu` | Menu categories and items |
| `inventory` | Stock levels and ingredients |
| `diningtable` | Restaurant floor and table layout |
| `order` | Guest orders and POS flow |
| `kitchen` | Kitchen display / order preparation |
| `reservation` | Table reservations |
| `payment` | Payments and receipts |
| `report` | Operational and sales reports |

## Requirements

- Java 17+
- MySQL 8+
- Maven Wrapper (included) or Maven 3.6.3+

## Configuration

Database credentials and optional initial admin are supplied via environment variables.

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/restaurant_management` | JDBC URL |
| `DB_USERNAME` | `restaurant_app` | Database username |
| `DB_PASSWORD` | _(required)_ | Database password |
| `INITIAL_ADMIN_EMAIL` | _(optional)_ | Creates an ADMIN user on startup when set with the other initial admin variables |
| `INITIAL_ADMIN_PASSWORD` | _(optional)_ | Initial admin password (BCrypt-hashed before storage) |
| `INITIAL_ADMIN_FULL_NAME` | _(optional)_ | Initial admin display name |

See `src/main/resources/application-example.properties` for placeholders only.
Do not put real secrets in `application.properties` or Git.

### Windows (PowerShell)

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/restaurant_management"
$env:DB_USERNAME = "restaurant_app"
$env:DB_PASSWORD = "your-password"
$env:INITIAL_ADMIN_EMAIL = "admin@example.com"
$env:INITIAL_ADMIN_PASSWORD = "replace_with_secure_password"
$env:INITIAL_ADMIN_FULL_NAME = "Restaurant Administrator"
```

### Linux / macOS

```bash
export DB_URL="jdbc:mysql://localhost:3306/restaurant_management"
export DB_USERNAME="restaurant_app"
export DB_PASSWORD="your-password"
export INITIAL_ADMIN_EMAIL="admin@example.com"
export INITIAL_ADMIN_PASSWORD="replace_with_secure_password"
export INITIAL_ADMIN_FULL_NAME="Restaurant Administrator"
```

## Build and test

```powershell
.\mvnw.cmd clean test
```

## Run the application

```powershell
.\mvnw.cmd spring-boot:run
```

The application starts on `http://localhost:8080` by default.

Use Spring Security form login at `/login`. Logout is available via the default logout endpoint.

Static assets are served from:

- `src/main/resources/static/`
- `src/main/resources/static/css/`
- `src/main/resources/static/js/`

## Admin user API

- `POST /api/admin/users`
- `GET /api/admin/users`
- `GET /api/admin/users/{id}`
- `PUT /api/admin/users/{id}/roles`
- `PATCH /api/admin/users/{id}/status`

## Current development status

- Project foundation and shared API error handling are in place
- Users, roles, BCrypt passwords, and session-based Spring Security are implemented
- Roles are seeded idempotently on startup (`ADMIN`, `WAITER`, `COOK`, `CLIENT`)
- Optional initial ADMIN can be created from environment variables
- Menu and remaining business modules are not implemented yet
