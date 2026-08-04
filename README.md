# restaurant-pos-reservations

Modular monolith Spring Boot application for restaurant POS and reservations.

## Requirements

- Java 17+
- Maven Wrapper (included) or Maven 3.6.3+
- MySQL 8+ (for running the application)

## Project structure

Functional packages under `bg.martinandonov.restaurant`:

- `common`
- `security`
- `user`
- `menu`
- `inventory`
- `diningtable`
- `order`
- `kitchen`
- `reservation`
- `payment`
- `report`

Static frontend assets live under:

- `src/main/resources/static/`
- `src/main/resources/static/css/`
- `src/main/resources/static/js/`

## Configuration

Database settings are provided via environment variables (see `application.properties` and `application-example.properties`):

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/restaurant_management` | JDBC URL |
| `DB_USERNAME` | `restaurant_app` | Database username |
| `DB_PASSWORD` | _(required)_ | Database password |

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

## Notes

- Do not commit real database passwords.
- Business modules are scaffolded as empty packages; implement features incrementally.
