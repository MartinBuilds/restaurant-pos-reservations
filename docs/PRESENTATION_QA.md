# Presentation Q&A

Short answers for a defense / demo.

1. **Why Java 17 and Spring Boot?**
   Mature enterprise stack, strong typing, security/data/web ecosystem, LTS Java.

2. **Why modular monolith, not microservices?**
   One cohesive domain and team scope; modules keep boundaries without network complexity.

3. **Why MySQL?**
   Reliable relational persistence, constraints, transactions, and clear reporting queries.

4. **How is the system secured?**
   Session login, BCrypt passwords, CSRF, role-based HTTP and STOMP authorization.

5. **How is double reservation prevented?**
   Interval overlap checks, ownership rules, locking/conflict responses (`409`).

6. **How is double payment prevented?**
   One-payment-per-order rule with locking; second attempt fails.

7. **How is negative stock prevented?**
   Stock checked/deducted inside the order transaction; insufficient stock fails the order.

8. **Why is WebSocket not source of truth?**
   Messages can be missed; REST/MySQL remain authoritative after every notification.

9. **What happens on WebSocket disconnect?**
   UI reconnects, re-subscribes, and reloads state from REST (no event replay store).

10. **Why AFTER_COMMIT?**
    Listeners must not announce changes that can still roll back.

11. **Why snapshot prices/names?**
    Orders remain historically accurate after menu edits.

12. **Why BigDecimal?**
    Avoid binary floating-point money errors.

13. **How are roles managed?**
    Roles in MySQL; ADMIN assigns roles; URLs/APIs enforce `hasRole` / `hasAnyRole`.

14. **Why no JWT?**
    Browser UIs use first-party sessions + CSRF; simpler and appropriate for this app.

15. **What does simulated CARD mean?**
    Local enum + DB payment record only; no PAN/CVV, no bank, no money movement.

16. **What would you add in production?**
    Real PSP, fiscal device, migrations, observability, rate limits, email/SMS, IdP, stronger ops packaging.

17. **How is concurrency tested?**
    Parallel smoke for reservation create and payment; expect exactly one success.

18. **How are credentials protected?**
    Env vars / gitignored local files; BCrypt at rest; never in Git or API responses.

19. **How do reservation intervals work?**
    Half-open style conflict on overlapping `[start,end)` windows in restaurant-local time.

20. **How do sales reports work?**
    Aggregations over closed paid orders/payments for a date range — operational, not tax accounting.
