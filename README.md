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
- Spring Security
- WebSocket
- MySQL 8+

## Roles (planned)

- Admin
- Manager
- Waiter
- Kitchen staff
- Cashier
- Host / reservation staff

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

Database credentials are supplied via environment variables. Do not hardcode secrets.

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/restaurant_management` | JDBC URL |
| `DB_USERNAME` | `restaurant_app` | Database username |
| `DB_PASSWORD` | _(required)_ | Database password |

See `src/main/resources/application-example.properties` for a copy-friendly template.

### Windows (PowerShell)

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/restaurant_management"
$env:DB_USERNAME = "restaurant_app"
$env:DB_PASSWORD = "your-password"
```

### Linux / macOS

```bash
export DB_URL="jdbc:mysql://localhost:3306/restaurant_management"
export DB_USERNAME="restaurant_app"
export DB_PASSWORD="your-password"
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

Static assets are served from:

- `src/main/resources/static/`
- `src/main/resources/static/css/`
- `src/main/resources/static/js/`

## Current development status

- Project scaffold and modular package layout are in place
- Shared API error handling foundation is available under `common.exception`
- Business modules are prepared as empty packages (no entities/services/controllers yet)
- Database is configured for MySQL via environment variables
- Next steps: security foundation, domain entities, and module-by-module features
