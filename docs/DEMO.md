# Demo / presentation script (8–12 minutes)

Use the optional `demo` profile locally. Set `DEMO_USER_PASSWORD` in your shell only (never commit it). Demo emails:

- `demo.admin@example.com`
- `demo.waiter@example.com`
- `demo.cook@example.com`
- `demo.client@example.com`

## Suggested timing

1. **~1 min — Architecture**
   Modular monolith, REST/MySQL source of truth, WebSocket notifications only, AFTER_COMMIT, roles.

2. **ADMIN login → `/admin`**
   Show menu categories/items, inventory stock, dining tables, a reservation schedule view.

3. **WAITER login → `/waiter`**
   Pick an available DEMO table, create an order with DEMO menu items.

4. **KITCHEN login → `/kitchen` (second browser/window)**
   Show realtime appearance of the new order (notification → REST refresh).

5. **COOK workflow**
   `ACCEPTED → COOKING → READY`.

6. **WAITER sees READY**
   Realtime update, then `READY → SERVED`.

7. **Payment simulation**
   CASH or CARD body with method only. Show order closed and table back to AVAILABLE. Emphasize: no card details, no gateway, not a fiscal receipt.

8. **ADMIN sales report**
   Summary / by-item / by-payment-method includes the simulated payment.

9. **CLIENT login → `/client`**
   Availability search → create reservation → show ownership list/detail → cancel (or show seeded DEMO reservation).

10. **Close with design principles**
    Security roles, conflict locking, snapshots, BigDecimal, no event replay dependency.

## Talking points

- **Why modular monolith?** One deployable with clear packages; enough structure without microservice ops cost for this scope.
- **Why MySQL is source of truth?** Durable business state; UIs and WebSocket never invent truth.
- **Why WebSocket is notification-only?** Avoid dual-write race; reconnect recovers via REST.
- **Why AFTER_COMMIT?** Never notify before the DB write is durable.
- **Why PESSIMISTIC_WRITE / conflict checks?** Prevent double booking and double payment under concurrency.
- **Why snapshots?** Historical order lines keep sold price/name even if the menu changes later.
- **Why BigDecimal?** Exact money arithmetic.
- **Why simulated CARD?** Demonstrates POS flow without handling real card data or PSP integration.

## Safety reminders

- Do not display real passwords on screen.
- Say clearly that CARD is a local enum simulation.
- Prefer DEMO-prefixed catalog data so the demo stays recognizable.
