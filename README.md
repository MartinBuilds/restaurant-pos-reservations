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
| `ADMIN` | `/admin/**`, `/api/admin/**`, and all other role-restricted areas |
| `WAITER` | `/api/waiter/**` |
| `COOK` | `/api/kitchen/**` |
| `CLIENT` | `/api/client/**` |

Public (no authentication): `/`, `/login`, `/css/**`, `/js/**`, `/images/**`, `/api/public/**`. Admin UI at `/admin/**` requires `ADMIN`.

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
| `RESTAURANT_TIME_ZONE` | `Europe/Sofia` | Zone for reservation `LocalDateTime.now(clock)` |
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

## Dining tables API

Statuses (`DiningTableStatus`):

- `AVAILABLE` — active and ready for use
- `OCCUPIED` — currently in use
- `RESERVED` — marked as reserved
- `OUT_OF_SERVICE` — temporarily unusable

### Active vs status

- `active` is the catalog/soft-delete flag. Inactive tables remain in the database.
- Deactivating a table sets `active=false` and forces `status=OUT_OF_SERVICE`.
- Reactivating a table sets `active=true` and `status=AVAILABLE`.
- Inactive tables cannot be moved to `AVAILABLE`, `OCCUPIED`, or `RESERVED`.
- Tables with an open order must stay `OCCUPIED` and cannot be set to `AVAILABLE`/`RESERVED` or deactivated.
- Order creation automatically sets a table to `OCCUPIED`. Future reservation and payment modules will drive additional status changes.

Admin endpoints (`ADMIN` only):

- `POST /api/admin/tables`
- `GET /api/admin/tables`
- `GET /api/admin/tables/{id}`
- `PUT /api/admin/tables/{id}`
- `PATCH /api/admin/tables/{id}/status`
- `PATCH /api/admin/tables/{id}/active`

Waiter endpoints (`WAITER` or `ADMIN`):

- `GET /api/waiter/tables`
- `GET /api/waiter/tables?status=AVAILABLE`
- `GET /api/waiter/tables/{id}`
- `PATCH /api/waiter/tables/{id}/status`

Waiter rules:

- lists only active tables
- may set `AVAILABLE`, `OCCUPIED`, or `RESERVED`
- may not set `OUT_OF_SERVICE`
- inactive tables are not visible (404 by id)

Create table example:

```json
{
  "tableNumber": 5,
  "displayName": "Window",
  "capacity": 4
}
```

Example response:

```json
{
  "id": 1,
  "tableNumber": 5,
  "displayName": "Window",
  "capacity": 4,
  "status": "AVAILABLE",
  "active": true,
  "version": 0
}
```

Update status example:

```json
{
  "status": "OCCUPIED"
}
```

## Orders API

Entities:

- `RestaurantOrder` (`restaurant_orders`) — table, waiter, status, closed flag, total, timestamps
- `OrderItem` (`order_items`) — snapshot `menuItemName` / `unitPrice`, quantity, line total

Order statuses (`OrderStatus`):

- `ACCEPTED` (new orders always start here)
- `COOKING`, `READY`, `SERVED`, `CANCELLED` (kitchen/waiter workflow; cancellation not exposed yet)

### Order status workflow

Allowed transitions:

- `ACCEPTED` → `COOKING` (COOK / ADMIN via kitchen API)
- `COOKING` → `READY` (COOK / ADMIN via kitchen API)
- `READY` → `SERVED` (WAITER / ADMIN via waiter API)

Invalid transitions return `409 Conflict`. Requesting an unsupported status for the endpoint returns `400 Bad Request`. Setting the current status again is idempotent (`200 OK`).

After `SERVED`:

- `order.closed` remains `false`
- table remains `OCCUPIED`
- stock, totals, and snapshots are unchanged

`SERVED` does **not** mean paid. A later simulated payment PR will record `CASH` or `CARD` in MySQL, set `closed=true`, and free the table — without Stripe, PayPal, bank APIs, or real card data.

Kitchen REST endpoints (`COOK` or `ADMIN`):

- `GET /api/kitchen/orders`
- `GET /api/kitchen/orders?status=ACCEPTED|COOKING|READY`
- `GET /api/kitchen/orders/{id}`
- `PATCH /api/kitchen/orders/{id}/status` with `COOKING` or `READY`

Kitchen queue includes only open orders in `ACCEPTED`, `COOKING`, or `READY`. Served/cancelled/closed orders are omitted (404 by id).

Waiter status endpoint (`WAITER` or `ADMIN`):

- `PATCH /api/waiter/orders/{id}/status` with `SERVED` only

There is no SockJS, HTML kitchen screen, or WebSocket message persistence in this PR. See **Kitchen WebSocket / STOMP** below.

### Creation flow

1. Lock dining table; require `active` and `AVAILABLE`; reject if another open order exists
2. Validate items (non-empty, no duplicate menu item ids, quantity 1–100)
3. Validate each menu item: active, category active, manual/effective available, has recipe
4. Aggregate ingredient requirements across all ordered items (`recipe.quantityRequired × quantity`)
5. Lock ingredients by id ascending; verify stock; deduct only after all checks pass
6. Create order (`ACCEPTED`, `closed=false`) and snapshot order items
7. Set table to `OCCUPIED`; recalculate menu availability in the same transaction

Any failure rolls back stock, table status, order, and availability changes.

### Snapshot pricing

- `unitPrice` and `menuItemName` are copied at order time
- Later menu price changes do not alter existing order lines
- Adding quantity to an existing line keeps the original snapshot unit price

### Waiter order endpoints (`WAITER` or `ADMIN`)

- `POST /api/waiter/orders` → 201
- `GET /api/waiter/orders` — open orders (`closed=false`)
- `GET /api/waiter/orders?tableId={id}` — open orders for a table
- `GET /api/waiter/orders/{id}`
- `POST /api/waiter/orders/{id}/items` — add items to an `ACCEPTED` open order
- `PATCH /api/waiter/orders/{id}/status` — set `SERVED` when current status is `READY`

Create order example:

```json
{
  "diningTableId": 1,
  "items": [
    { "menuItemId": 10, "quantity": 2 },
    { "menuItemId": 11, "quantity": 1 }
  ]
}
```

Example order response:

```json
{
  "id": 1,
  "orderNumber": "3f1c9e2a-....",
  "diningTableId": 1,
  "tableNumber": 5,
  "waiterId": 2,
  "waiterName": "Ada Waiter",
  "status": "ACCEPTED",
  "closed": false,
  "totalAmount": 28.50,
  "createdAt": "2026-08-05T01:00:00",
  "updatedAt": "2026-08-05T01:00:00",
  "items": [
    {
      "id": 1,
      "menuItemId": 10,
      "menuItemName": "Caesar Salad",
      "unitPrice": 12.50,
      "quantity": 2,
      "lineTotal": 25.00
    }
  ]
}
```

### Payments (future, simulation only)

This project does not include payment functionality yet. A later PR will add simulated `CASH` / `CARD` payment records that set `closed=true`. There will be no Stripe, PayPal, bank API, real card numbers, IBAN, or live money movement.

## Kitchen WebSocket / STOMP

Real-time notifications only. REST and MySQL remain the source of truth.

### Architecture

```text
REST operation
    ↓
@Transactional business logic
    ↓
MySQL commit
    ↓
AFTER_COMMIT application event
    ↓
STOMP notification
```

Notifications are never sent for rolled-back transactions. WebSocket messages are best-effort; they are not stored.

### Endpoint and broker

| Setting | Value |
|---|---|
| Handshake endpoint | `/ws` (authenticated HTTP session; no SockJS) |
| Application destination prefix | `/app` (client business SEND denied) |
| Broker destination prefix | `/topic` (in-memory simple broker) |
| Heartbeats | server outgoing 10000 ms, expected incoming 10000 ms |

Allowed origin patterns for local development only: `http(s)://localhost:*` and `http(s)://127.0.0.1:*`. No wildcard `*`.

### Topics

| Topic | Events |
|---|---|
| `/topic/kitchen/orders` | `ORDER_CREATED`, `ORDER_STATUS_CHANGED` |
| `/topic/waiter/orders` | `ORDER_STATUS_CHANGED` only |

`ORDER_CREATED` is not sent to the waiter topic (the waiter already has the REST create response). Status changes (`ACCEPTED→COOKING`, `COOKING→READY`, `READY→SERVED`) go to both topics so kitchen clients can remove served orders from the active queue.

No event is published for `addItemsToOrder` or for idempotent same-status updates.

### Event payload

`OrderRealtimeMessage`:

- `eventId` (UUID, unique per real event; usable for client deduplication)
- `eventType` (`ORDER_CREATED` \| `ORDER_STATUS_CHANGED`)
- `occurredAt` (UTC `Instant`)
- `previousStatus` (`OrderStatus`, null for create)
- `currentStatus` (`OrderStatus`)
- `order` (`KitchenOrderResponse` — no unit prices, line totals, order total, or payment fields)

Example `ORDER_CREATED`:

```json
{
  "eventId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "eventType": "ORDER_CREATED",
  "occurredAt": "2026-08-05T01:05:00Z",
  "previousStatus": null,
  "currentStatus": "ACCEPTED",
  "order": {
    "id": 1,
    "orderNumber": "3f1c9e2a-....",
    "diningTableId": 1,
    "tableNumber": 5,
    "status": "ACCEPTED",
    "createdAt": "2026-08-05T01:05:00",
    "updatedAt": "2026-08-05T01:05:00",
    "items": [
      {
        "orderItemId": 1,
        "menuItemId": 10,
        "menuItemName": "Caesar Salad",
        "quantity": 2
      }
    ]
  }
}
```

Example `ORDER_STATUS_CHANGED`:

```json
{
  "eventId": "bbbbbbbb-cccc-dddd-eeee-ffffffffffff",
  "eventType": "ORDER_STATUS_CHANGED",
  "occurredAt": "2026-08-05T01:10:00Z",
  "previousStatus": "ACCEPTED",
  "currentStatus": "COOKING",
  "order": {
    "id": 1,
    "orderNumber": "3f1c9e2a-....",
    "diningTableId": 1,
    "tableNumber": 5,
    "status": "COOKING",
    "createdAt": "2026-08-05T01:05:00",
    "updatedAt": "2026-08-05T01:10:00",
    "items": [
      {
        "orderItemId": 1,
        "menuItemId": 10,
        "menuItemName": "Caesar Salad",
        "quantity": 2
      }
    ]
  }
}
```

### Authentication and subscription authorization

Handshake `/ws` requires the existing session-based Spring Security authentication (same cookie as REST). No JWT and no separate WebSocket login.

STOMP `CONNECT` also requires the session CSRF token (Spring Security messaging default). Authenticated clients read it from:

- `GET /api/csrf`

and send the token in STOMP CONNECT headers (`headerName` / `token` from that response). HTTP CSRF for REST is unchanged.

| Destination | Allowed roles |
|---|---|
| `SUBSCRIBE /topic/kitchen/**` | `COOK`, `ADMIN` |
| `SUBSCRIBE /topic/waiter/**` | `WAITER`, `ADMIN` |

`CLIENT` and anonymous users cannot subscribe. Client `SEND` to `/app/**` is denied. There are no `@MessageMapping` business controllers.

If the in-memory broker fails to deliver a notification after commit, the failure is logged (eventId + orderId only) and the REST response remains successful.

### Reconnect

On connect or reconnect, clients should:

1. Load current state via REST (`GET /api/kitchen/orders` or the waiter order endpoints)
2. Then subscribe to STOMP updates

Missed WebSocket events are not replayed. There is no `websocket_messages` table and no outbox in this PR.

### Out of scope here

- HTML kitchen screen / frontend frameworks
- SockJS
- External brokers (Kafka, RabbitMQ, Redis, ActiveMQ)
- Payments, reports

## Table reservations

Online table bookings with conflict checks and occupancy schedule. There is no anonymous/public booking, no email/SMS, and no automatic change to `DiningTable.status`.

### Time zone

Reservation request times are `LocalDateTime` in the restaurant zone:

```properties
app.restaurant.time-zone=${RESTAURANT_TIME_ZONE:Europe/Sofia}
```

API expects ISO local date-time, for example `2026-08-05T19:00:00`. The service uses `LocalDateTime.now(clock)` with a configured `Clock` bean (not raw `LocalDateTime.now()`).

### Entity and statuses

Table `reservations` stores:

- unique `reservationNumber` (server-generated UUID)
- dining table, client user
- `startTime` / `endTime`
- `guestCount`, optional `notes`
- `status`, timestamps, `@Version`

Statuses (`ReservationStatus`):

| Status | Blocks interval? |
|---|---|
| `CONFIRMED` | yes |
| `CANCELLED` | no |
| `COMPLETED` | no |
| `NO_SHOW` | no |

New reservations start as `CONFIRMED`.

### Conflict rule

Two `CONFIRMED` reservations on the same table conflict when:

```text
existing.startTime < requested.endTime
AND
existing.endTime > requested.startTime
```

Adjacent intervals are allowed (`18:00–20:00` and `20:00–22:00`). Overlap or identical intervals return `409 Conflict`.

Concurrency: create/update locks the dining-table row with `PESSIMISTIC_WRITE`, then checks conflicts in the same transaction. Lock conflicts map to `409`.

Reservations never automatically set `DiningTable.status` to `RESERVED` / `OCCUPIED` / etc. Current `AVAILABLE` / `OCCUPIED` / `RESERVED` does not block a future booking; `OUT_OF_SERVICE` and inactive tables do.

### Capacity and availability

- `guestCount >= 1` and `guestCount <= table.capacity` (capacity overflow → `400`)
- Availability search returns active tables that are not `OUT_OF_SERVICE`, have enough capacity, and have no conflicting `CONFIRMED` reservation
- Ordered by capacity ascending, then table number ascending

### Ownership and status workflow

- Client endpoints use the authenticated user (must have `CLIENT` role). Foreign reservations return `404`.
- Client may update/cancel only own future `CONFIRMED` reservations (`CONFIRMED → CANCELLED` only; cancel is idempotent).
- Admin may set `CANCELLED` (before start), `COMPLETED` (after end), `NO_SHOW` (after start). Terminal statuses cannot change further. Same-status updates are idempotent.

### Dining-table guard

A future `CONFIRMED` reservation blocks table deactivation and `OUT_OF_SERVICE`. Cancel or reschedule first. Open-order guards remain unchanged.

### Endpoints

Client (`CLIENT`; must have CLIENT role at service layer):

- `GET /api/client/reservations/availability?startTime=&endTime=&guestCount=`
- `POST /api/client/reservations`
- `GET /api/client/reservations`
- `GET /api/client/reservations/{id}`
- `PUT /api/client/reservations/{id}`
- `PATCH /api/client/reservations/{id}/cancel`

Admin (`ADMIN`):

- `POST /api/admin/reservations` (includes `clientId`)
- `GET /api/admin/reservations?from=&to=&status=&tableId=&clientId=`
- `GET /api/admin/reservations/{id}`
- `PUT /api/admin/reservations/{id}`
- `PATCH /api/admin/reservations/{id}/status`
- `GET /api/admin/reservations/schedule?from=&to=&tableId=&status=`

Waiter (`WAITER` / `ADMIN`), read-only:

- `GET /api/waiter/reservations/schedule?from=&to=&tableId=&status=`

Waiter schedule defaults to `CONFIRMED`, `COMPLETED`, `NO_SHOW` (excludes `CANCELLED` unless `status=CANCELLED` is requested). Admin schedule includes all statuses when `status` is omitted. Schedule overlap uses the same interval formula against `from`/`to`.

Create example:

```json
{
  "diningTableId": 1,
  "startTime": "2026-08-05T19:00:00",
  "endTime": "2026-08-05T21:00:00",
  "guestCount": 2,
  "notes": "Anniversary"
}
```

Example response:

```json
{
  "id": 1,
  "reservationNumber": "3f1c9e2a-....",
  "diningTableId": 1,
  "tableNumber": 5,
  "tableDisplayName": "Window",
  "clientId": 10,
  "clientName": "Ada Client",
  "clientEmail": "client@example.com",
  "startTime": "2026-08-05T19:00:00",
  "endTime": "2026-08-05T21:00:00",
  "guestCount": 2,
  "status": "CONFIRMED",
  "notes": "Anniversary",
  "createdAt": "2026-08-05T12:00:00",
  "updatedAt": "2026-08-05T12:00:00"
}
```

## Simulated payments and receipt

Local-only payment simulation for `SERVED` orders. There is **no** Stripe, PayPal, bank, POS terminal, gateway, webhook, card number, CVV, IBAN, or payment token. `CARD` is a text enum stored in MySQL — never enter real card data.

### Entity

Table `payments`:

- unique `receiptNumber` (server-generated `SIM-…` UUID)
- unique `order_id` (one payment per order)
- `method` (`CASH` | `CARD`, STRING)
- `amount` `DECIMAL(12,2)` snapshot of `RestaurantOrder.totalAmount`
- `processedBy` (authenticated WAITER/ADMIN)
- `paidAt` via configured `Clock`
- `@Version`

Indexes: `paid_at`, `method+paid_at`, `processed_by_id+paid_at`.

### Process flow

1. Resolve `diningTableId` for the order (no write lock)
2. `PESSIMISTIC_WRITE` dining table, then order (same lock order as other table flows)
3. Require `status=SERVED`, `closed=false`, no existing payment, `totalAmount > 0`, table `active` and `OCCUPIED`
4. Insert `Payment` with server amount (request cannot set amount)
5. Set `order.closed=true` (status stays `SERVED` — SERVED is not PAID until a Payment exists)
6. Set `table.status=AVAILABLE`
7. Return simulated receipt (`simulated=true`) with OrderItem snapshot lines

Duplicate / concurrency: table+order locks, `closed` check, `existsByOrderId`, unique `order_id`, `@Version`. Parallel requests → one `201`, one `409`, exactly one payment.

Does **not** change stock, menu availability, recipes, order items, order status, reservations, or publish WebSocket events. No partial/split/refund/tips/tax. Receipt is not a legal tax invoice or fiscal bon. Aggregated admin sales reports are available under `/api/admin/reports/sales`.

Future confirmed reservations do not block freeing the table after payment.

### Endpoints

Waiter / Admin:

- `POST /api/waiter/orders/{orderId}/payment` → `201`
- `GET /api/waiter/orders/{orderId}/payment` → `200` / `404`

Admin only:

- `GET /api/admin/payments?method=&from=&to=&processedById=`
- `GET /api/admin/payments/{id}`

### Example requests

```json
{ "method": "CASH" }
```

```json
{ "method": "CARD" }
```

## Sales reports

Admin-only read-only sales reports over simulated payments. Included rows require:

- a `Payment` exists
- `order.closed = true`
- `order.status = SERVED`
- `Payment.paidAt` in the selected period

Period is half-open `[from, to)` in the restaurant zone (`app.restaurant.time-zone`, default `Europe/Sofia`):

- `paidAt >= from`
- `paidAt < to`

ISO local date-time example: `2026-08-01T00:00:00`.

### Endpoints

- `GET /api/admin/reports/sales/summary?from=&to=`
- `GET /api/admin/reports/sales/by-item?from=&to=`
- `GET /api/admin/reports/sales/by-payment-method?from=&to=`

### Metrics

Summary: `totalRevenue` (sum of payment amounts), `paidOrdersCount`, `soldItemsCount` (sum of order-item quantities), `averageOrderValue` (`HALF_UP`, scale 2). Empty period returns zeros.

Sales by item: groups by `menuItemId` + historical `OrderItem.menuItemName` snapshot; revenue from `lineTotal`. Renames/price changes after payment do not rewrite history. Ordered by revenue desc, quantity desc, name asc, id asc.

Sales by payment method: always returns `CASH` then `CARD` with counts, amounts, and `percentageOfRevenue`. Missing methods are zero-filled.

Reports are not legal accounting/tax documents. There is no frontend charting and no PDF/CSV/Excel export in this PR. Payments remain simulated CASH/CARD only.

Example summary response:

```json
{
  "period": { "from": "2026-08-01T00:00:00", "to": "2026-08-02T00:00:00", "timeZone": "Europe/Sofia" },
  "totalRevenue": 100.00,
  "paidOrdersCount": 2,
  "soldItemsCount": 5,
  "averageOrderValue": 50.00
}
```

## Admin UI

Vanilla HTML/CSS/JavaScript administrative panel served as static resources.

- URL: `/admin` (forwards to `/admin/index.html`)
- Role: `ADMIN` only (WAITER/COOK/CLIENT denied; anonymous redirected to login)
- Auth: existing session-based Spring Security login
- CSRF: `GET /api/csrf`, then send token header on POST/PUT/PATCH/DELETE
- Routing: hash routes `#/dashboard`, `#/users`, `#/menu`, `#/inventory`, `#/tables`, `#/reservations`, `#/payments`, `#/reports`
- Stack: HTML5, CSS3, ES modules, Fetch API — no Node.js, npm, React/Angular/Vue, jQuery, Bootstrap/Tailwind, CDN, or external fonts
- REST is the source of truth; no new business endpoints or JPA changes in this UI
- LocalDateTime fields are sent without `Z`/offset (restaurant zone, default `Europe/Sofia`)
- Payments are simulated CASH/CARD history only — no card data, no fiscal invoice UI
- Sales reports are operational, not tax/accounting documents
- Waiter, kitchen, and client UIs are not included (future PRs)
- Browsers: current evergreen desktop/mobile browsers; responsive from ~390px to 1440px+

### Admin sections

| Section | Uses |
|---|---|
| Users | `/api/admin/users` |
| Menu | `/api/admin/menu/categories`, `/api/admin/menu/items`, availability |
| Inventory | `/api/admin/inventory/ingredients`, recipes |
| Tables | `/api/admin/tables` |
| Reservations | `/api/admin/reservations`, `/schedule` |
| Payments | `/api/admin/payments` (read-only) |
| Reports | `/api/admin/reports/sales/*` |

## Current development status

- Project foundation and shared API error handling are in place
- Users, roles, BCrypt passwords, and session-based Spring Security are implemented
- Roles are seeded idempotently on startup (`ADMIN`, `WAITER`, `COOK`, `CLIENT`)
- Optional initial ADMIN can be created from environment variables
- Menu catalog (categories and items) is implemented with admin and public APIs
- Ingredients, stock quantities, and recipes are implemented for ADMIN management
- Automatic menu availability is computed from recipes and stock (with manual override)
- Dining tables are managed by ADMIN and operable by WAITER
- Order creation with transactional stock deduction is implemented for WAITER/ADMIN
- Order status workflow ACCEPTED → COOKING → READY → SERVED is implemented over REST
- Kitchen/waiter STOMP notifications are implemented (AFTER_COMMIT, no message persistence)
- Table reservations with conflict checks and schedule are implemented
- Simulated CASH/CARD payments and receipts are implemented
- Admin sales reports (summary, by item, by payment method) are implemented
- Admin UI (vanilla HTML/CSS/JS) is implemented for ADMIN session users
