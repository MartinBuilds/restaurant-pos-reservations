# API overview

Human-readable summary by role. Authentication is session-based form login. Mutating requests require CSRF (`GET /api/csrf`).

## Auth and shared

| Method | Path | Notes |
|---|---|---|
| GET/POST | `/login` | Form login |
| POST | `/logout` | CSRF required |
| GET | `/api/csrf` | Authenticated CSRF token |
| GET | `/api/public/menu` | Public available menu |

## ADMIN (`/api/admin/**`)

- Users: create/list/get, roles, enabled status
- Menu: categories/items, availability recalculation
- Inventory: ingredients, stock, recipes
- Tables: CRUD-ish admin operations + status/active
- Reservations: create/list/update/status/schedule
- Payments: read-only history filters
- Reports: sales summary, by-item, by-payment-method

## WAITER (`/api/waiter/**`)

- Tables: list/get/status
- Orders: create, list open, add items, `READY → SERVED`
- Payments: create simulated payment for an order
- Reservations: schedule read-only

Create payment body (simulation only):

```json
{ "method": "CASH" }
```

or:

```json
{ "method": "CARD" }
```

No card number, CVV, holder, or provider fields.

## COOK (`/api/kitchen/**`)

- Orders queue (`ACCEPTED` / `COOKING` / `READY`)
- Status transitions: `COOKING`, `READY` only

## CLIENT (`/api/client/**`)

- `GET .../availability?startTime&endTime&guestCount`
- `POST /` create reservation
- `GET /` list own
- `GET /{id}` detail (foreign id → 404)
- `PUT /{id}` reschedule
- `PATCH /{id}/cancel`

LocalDateTime values are restaurant-local (no `Z` / UTC conversion).

## WebSocket

| Endpoint / topic | Access |
|---|---|
| `/ws` | Authenticated |
| `/topic/kitchen/orders` | COOK, ADMIN |
| `/topic/waiter/orders` | WAITER, ADMIN |

STOMP `CONNECT` requires CSRF. Business `SEND` to `/app/**` is denied. CLIENT has no operational subscriptions.

## Error style

API errors use a safe JSON error body (message + status). Stack traces are not returned to clients.