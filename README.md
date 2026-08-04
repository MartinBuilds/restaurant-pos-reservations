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
| `menu` | Menu categories and items (catalog) |
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

## Menu catalog API

Admin-only category endpoints:

- `POST /api/admin/menu/categories`
- `GET /api/admin/menu/categories`
- `GET /api/admin/menu/categories/{id}`
- `PUT /api/admin/menu/categories/{id}`
- `PATCH /api/admin/menu/categories/{id}/status`

Admin-only menu item endpoints:

- `POST /api/admin/menu/items`
- `GET /api/admin/menu/items`
- `GET /api/admin/menu/items?categoryId={id}`
- `GET /api/admin/menu/items/{id}`
- `PUT /api/admin/menu/items/{id}`
- `PATCH /api/admin/menu/items/{id}/status`
- `PATCH /api/admin/menu/items/{id}/availability`
- `GET /api/admin/menu/items/{id}/availability`
- `POST /api/admin/menu/availability/recalculate`

Public endpoints (no login):

- `GET /api/public/menu`
- `GET /api/public/menu/categories`

### Manual vs effective availability

- `manualAvailable` is the admin on/off switch (`PATCH .../availability` request field `available` updates this flag).
- `available` is the effective, automatically computed availability stored on `MenuItem`.
- Effective availability is `true` only when:
  1. `manualAvailable` is `true`
  2. the item has a recipe
  3. every recipe ingredient is active
  4. every recipe ingredient has `stockQuantity >= quantityRequired`
- Items without a recipe are always unavailable (`availabilityReason = NO_RECIPE`).
- `minimumStockLevel` is only used for low-stock indicators; it does not decide availability.
- Availability is recalculated after stock adjustments, ingredient status changes, recipe replace/remove, manual availability updates, menu item creation, and on application startup.
- Public menu returns only items where the item is `active` and effectively `available`, and its category is `active`. Public GET uses the stored `available` flag (no per-request recalculation).
- This PR does not create orders and does not deduct stock on sale (planned for the order PR).

### Availability reasons

- `AVAILABLE`
- `MANUALLY_DISABLED`
- `NO_RECIPE`
- `INACTIVE_INGREDIENT`
- `INSUFFICIENT_STOCK`

Priority follows the list above when multiple conditions apply.

### Example requests

Create category:

```json
{
  "name": "Salads",
  "description": "Fresh salads"
}
```

Create menu item:

```json
{
  "name": "Caesar Salad",
  "description": "Romaine, parmesan, croutons",
  "price": 12.50,
  "categoryId": 1,
  "available": true
}
```

Example menu item response:

```json
{
  "id": 10,
  "name": "Caesar Salad",
  "description": "Romaine, parmesan, croutons",
  "price": 12.50,
  "active": true,
  "manualAvailable": true,
  "available": false,
  "availabilityReason": "NO_RECIPE",
  "categoryId": 1,
  "categoryName": "Salads"
}
```

Example availability response:

```json
{
  "menuItemId": 10,
  "menuItemName": "Caesar Salad",
  "manualAvailable": true,
  "available": true,
  "availabilityReason": "AVAILABLE",
  "maxPossibleServings": 4
}
```

## Inventory and recipes API

Base units (`IngredientUnit`):

- `GRAM` (store kilograms as `1000`)
- `MILLILITER` (store liters as `1000`)
- `PIECE`

Admin ingredient endpoints:

- `POST /api/admin/inventory/ingredients`
- `GET /api/admin/inventory/ingredients`
- `GET /api/admin/inventory/ingredients?activeOnly=true`
- `GET /api/admin/inventory/ingredients/{id}`
- `PUT /api/admin/inventory/ingredients/{id}`
- `PATCH /api/admin/inventory/ingredients/{id}/status`
- `PATCH /api/admin/inventory/ingredients/{id}/stock`

Admin recipe endpoints:

- `GET /api/admin/menu/items/{menuItemId}/recipe`
- `PUT /api/admin/menu/items/{menuItemId}/recipe`
- `DELETE /api/admin/menu/items/{menuItemId}/recipe`

Stock adjustment:

- `quantityChange` may be positive (delivery) or negative (write-off)
- resulting stock cannot go below zero
- concurrent stock updates are protected with `@Version` and a pessimistic write lock

Create ingredient example:

```json
{
  "name": "Tomato",
  "unit": "GRAM",
  "stockQuantity": 5000,
  "minimumStockLevel": 500
}
```

Adjust stock example:

```json
{
  "quantityChange": -250,
  "note": "Prep usage correction"
}
```

Replace recipe example:

```json
{
  "components": [
    { "ingredientId": 1, "quantityRequired": 150 },
    { "ingredientId": 2, "quantityRequired": 20 }
  ]
}
```

## Current development status

- Project foundation and shared API error handling are in place
- Users, roles, BCrypt passwords, and session-based Spring Security are implemented
- Roles are seeded idempotently on startup (`ADMIN`, `WAITER`, `COOK`, `CLIENT`)
- Optional initial ADMIN can be created from environment variables
- Menu catalog (categories and items) is implemented with admin and public APIs
- Ingredients, stock quantities, and recipes are implemented for ADMIN management
- Automatic menu availability is computed from recipes and stock (with manual override)
- Order-time stock deduction is not implemented yet
- Orders, reservations, and remaining modules are not implemented yet
