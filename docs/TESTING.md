# Testing

## Automated tests

Run the full suite:

```powershell
.\mvnw.cmd clean test
```

Coverage includes:

- service unit tests (menu, inventory, orders, reservations, payments, reports, users)
- controller security tests (role isolation)
- WebSocket/STOMP security tests
- static UI resource tests (Admin / Waiter / Kitchen / Client)
- login redirect / route security tests
- demo initializer tests (`@Profile("demo")` behavior, password gating, idempotency)

Tests use the `test` Spring profile and mocked persistence where appropriate. They do **not** require a live MySQL instance.

## Local MySQL smoke tests

Manual/scripted smoke against a real database validates:

- inventory rollback and stock deduction
- order workflow transitions
- AFTER_COMMIT WebSocket notifications
- reservation parallel conflict (exactly one winner)
- payment parallel protection (exactly one payment)
- sales report inclusion
- role isolation across UIs/APIs
- browser UI flows for all four roles

Typical local start before smoke:

```powershell
. .\smoke-env.ps1
.\mvnw.cmd spring-boot:run
```

`smoke-env.ps1` is gitignored and must never be committed.

## Browser smoke

Chromium/Chrome checks for `/admin`, `/waiter`, `/kitchen`, `/client` at desktop (~1440) and mobile (~390):

- navigation and forms
- dialogs / Escape / focus return
- logout via POST + CSRF
- no page-level horizontal overflow
- Waiter/Kitchen realtime status indicators
- Client availability → create → reschedule conflict → cancel

## Concurrency smoke

Repeat under load for:

- two parallel reservation creates for the same table/interval
- two parallel payments for the same order

Expected: exactly one successful business write; the other fails with a conflict/business error.

## Notes

Do not treat a fixed test count as a permanent contract. The current count is reported in release notes / PR verification for that moment in time.