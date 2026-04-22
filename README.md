# Checkout Service

A small Spring Boot service that models the checkout flow: **cart → order → payment → webhook → paid**.
It demonstrates domain-driven aggregates, an explicit order state machine, and idempotent webhook
handling against an external payment provider.

> **Stack:** Java 21 · Spring Boot 4.0.5 · Spring Data JPA · PostgreSQL (H2 in tests) · Maven

---

## Table of Contents

- [Quick Start](#quick-start)
- [Running with Docker Compose](#running-with-docker-compose)
- [Running Locally](#running-locally)
- [Configuration](#configuration)
- [API Endpoints](#api-endpoints)
- [Swagger / OpenAPI](#swagger--openapi)
- [End-to-End Example](#end-to-end-example)
- [Testing](#testing)
- [Project Layout](#project-layout)
- [Key Design Decisions](#key-design-decisions)
- [Assumptions](#assumptions)
- [Further Documentation](#further-documentation)

---

## Quick Start

```bash
# 1. Clone & enter
git clone <repo-url>
cd checkout-service

# 2. Boot Postgres + the service
docker compose up --build

# 3. Hit it
curl -X POST http://localhost:8080/carts
```

The service listens on **http://localhost:8080**.

---

## Running with Docker Compose

`docker-compose.yml` launches two containers on a private bridge network:

| Service            | Port | Notes                                  |
|--------------------|------|----------------------------------------|
| `postgres`         | 5432 | `postgres:16-alpine`, DB name `checkout` |
| `checkout-service` | 8080 | Built from the local `Dockerfile`      |

The application container waits for the database health check before starting.

```bash
docker compose up --build         # build + start
docker compose logs -f checkout-service
docker compose down               # stop (keeps volume)
docker compose down -v            # stop + drop DB volume
```

> ⚠️ The credentials currently in `docker-compose.yml` are **placeholders for local development only**.
> For any non-local environment set `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` via your
> secret store. `application.yml` already reads them from `${DB_USERNAME}` / `${DB_PASSWORD}`.

---

## Running Locally

Requires **JDK 21** and a running **PostgreSQL** reachable at `localhost:5432` with a `checkout` database.

```bash
# set DB credentials in your shell
export DB_USERNAME=checkout_user
export DB_PASSWORD=<your-password>

# run
./mvnw spring-boot:run
# or
./mvnw package && java -jar target/checkout-service-0.0.1-SNAPSHOT.jar
```

---

## Configuration

| Property                   | Source                  | Default                                           |
|----------------------------|-------------------------|---------------------------------------------------|
| `server.port`              | `application.yml`       | `8080`                                            |
| `spring.datasource.url`    | `application.yml`       | `jdbc:postgresql://localhost:5432/checkout`       |
| `spring.datasource.username` | env `DB_USERNAME`     | — (required)                                      |
| `spring.datasource.password` | env `DB_PASSWORD`     | — (required)                                      |
| `spring.jpa.hibernate.ddl-auto` | `application.yml`  | `validate` — schema drift fails fast              |
| `app.base-url`             | `application.yml`       | `http://localhost:8080`                           |

Test profile (`src/test/resources/application.yml`) uses in-memory H2 in PostgreSQL-compatibility mode
with `ddl-auto: create-drop` — no external DB needed to run tests.

---

## API Endpoints

| Method | Path                                | Purpose                                |
|--------|-------------------------------------|----------------------------------------|
| POST   | `/carts`                            | Create a new empty cart                |
| GET    | `/carts/{cartId}`                   | Fetch cart                             |
| POST   | `/carts/{cartId}/items`             | Add an item to the cart                |
| POST   | `/carts/{cartId}/checkout`          | Checkout the cart → creates an Order   |
| GET    | `/orders/{orderId}`                 | Fetch order summary                    |
| POST   | `/orders/{orderId}/payment/start`   | Start payment with the provider        |
| PUT    | `/orders/{orderId}/cancel`          | Cancel a non-terminal order            |
| POST   | `/payments/webhook`                 | Provider → service webhook (CONFIRMED / FAILED) |

**Mock provider (for local use only, guarded by `@Profile("local")` in production builds):**

| Method | Path                                     | Purpose                         |
|--------|------------------------------------------|---------------------------------|
| POST   | `/mock/payments/start`                   | Simulate the provider's "start" |
| PUT    | `/mock/payments/{externalPaymentId}/confirm` | Fire a CONFIRMED webhook    |
| PUT    | `/mock/payments/{externalPaymentId}/fail`    | Fire a FAILED webhook       |

---

## Swagger / OpenAPI

The service ships with [springdoc-openapi](https://springdoc.org) (`v2.8.5`).
Once the application is running, the following URLs are available:

| URL | Description |
|-----|-------------|
| `http://localhost:8080/swagger-ui/index.html` | Interactive Swagger UI |
| `http://localhost:8080/v3/api-docs` | Raw OpenAPI 3.0 JSON spec |
| `http://localhost:8080/v3/api-docs.yaml` | Raw OpenAPI 3.0 YAML spec |

### What you'll find in the UI

The API is grouped into three tags matching the service's module structure:

| Tag | Endpoints |
|-----|-----------|
| **Cart** | `POST /carts`, `GET /carts/{id}`, `POST /carts/{id}/items`, `POST /carts/{id}/checkout` |
| **Order** | `GET /orders/{id}`, `POST /orders/{id}/payment/start`, `PUT /orders/{id}/cancel` |
| **Payment webhook** | `POST /payments/webhook` |

Each endpoint documents:
- A plain-English summary and description of the business rule it enforces
- All possible response codes and their meaning (200, 201, 400, 404)
- Request/response schema derived from the Java `record` DTOs

### Customising springdoc properties

You can override any default via `application.yml`:

```yaml
springdoc:
  swagger-ui:
    path: /docs            # move UI to /docs instead of /swagger-ui/index.html
    operations-sorter: alpha
  api-docs:
    path: /v3/api-docs     # default
  packages-to-scan: org.codequest.checkoutservice
```

---

## End-to-End Example

```bash
# 1. Create cart
CART=$(curl -s -X POST http://localhost:8080/carts | jq -r '.id')

# 2. Add an item
curl -s -X POST http://localhost:8080/carts/$CART/items \
     -H 'Content-Type: application/json' \
     -d '{"productId":"SKU-1","quantity":2,"price":19.99}'

# 3. Checkout → creates Order (state = CREATED)
ORDER=$(curl -s -X POST http://localhost:8080/carts/$CART/checkout | jq -r '.id')

# 4. Start payment → order moves to PENDING_PAYMENT
EXT_ID=$(curl -s -X POST http://localhost:8080/orders/$ORDER/payment/start \
         | jq -r '.externalPaymentId')

# 5. Provider fires the webhook (CONFIRMED) → order moves to PAID
curl -X PUT http://localhost:8080/mock/payments/$EXT_ID/confirm

# 6. Verify
curl -s http://localhost:8080/orders/$ORDER | jq
```

---

## Testing

```bash
./mvnw test
```

The suite has **85 unit tests** (domain + service layers) and **9 integration tests** that exercise
the controllers via MockMvc against an H2 database. Integration tests cover the happy path, the
retry-after-failure flow, and duplicate-webhook idempotency.

---

## Project Layout

```
src/main/java/org/codequest/checkoutservice/
├── cart/         # active basket: Cart, CartItem aggregates
│   ├── api/ domain/ dto/ exception/ repository/ service/
├── order/        # confirmed purchases + OrderStateMachine
│   ├── api/ domain/ exception/ repository/ service/
├── payment/      # provider integration, webhooks, idempotency
│   ├── api/ application/ domain/ dto/ exception/ provider/ repository/ service/
└── shared/       # cross-module facades, DTOs, global exception handler, i18n
    └── config/ exception/ facade/ model/
```

Cross-module calls go through **facades** (`OrderFacade`, `PaymentFacade`) in the `shared` package —
no module imports another module's service or domain classes directly.

---

## Key Design Decisions

1. **Rich domain aggregates, not anemic DTOs.** Business invariants live on `Cart`, `Order`, and
   `Payment`. State transitions are methods on the aggregate (`cart.checkout()`, `payment.confirm()`),
   not service-layer `if` blocks.

2. **Explicit order state machine.** `OrderStateMachine` enumerates allowed transitions in a
   `Map<OrderState, Set<OrderState>>`. Invalid transitions raise `OrderException(INVALID_STATE_TRANSITION)` —
   return HTTP 400 via `GlobalExceptionHandler`.

3. **Facades for cross-module calls.** `CartService` calls `OrderFacade`, which accepts only shared DTOs,
   so `cart` never imports `order.domain`. The `shared` package owns the contracts.

4. **Application service for webhook orchestration.** `PaymentWebhookApplicationService` owns the
   `PaymentStatus → OrderState` mapping and the dual write (`Payment.confirm()` + `Order.transit(PAID)`)
   inside a single `@Transactional` boundary. The controller only does HTTP translation.

5. **Idempotency in depth (three layers).**
   - `PaymentService.startPayment` rejects duplicate active payments via `existsByOrderIdAndStatus(PENDING)`.
   - `PaymentService.processWebhook` returns `Optional.empty()` when the webhook repeats the current status.
   - `Payment.@Version` (optimistic lock) guards against concurrent webhook commits.

6. **Idempotent retries.** A failed payment isn't reset — a **new** `Payment` row is created for each
   attempt. The old `FAILED` row stays as an audit record. `PAYMENT_FAILED → PENDING_PAYMENT` is the
   only recoverable backward transition.

7. **Centralised, i18n-friendly exception handling.** Each module has its own `*ErrorCode` enum with
   message keys; `GlobalExceptionHandler` resolves them via Spring's `MessageSource`. A catch-all
   `@ExceptionHandler(Exception.class)` returns a generic 500 to prevent stack-trace leakage.

8. **Constructor injection + immutable DTOs.** No field injection, no Lombok. Every DTO is a Java
   `record`. Aggregates return `List.copyOf(...)` so callers cannot mutate internal state.

9. **Mock provider is environment-scoped.** `MockPaymentController` and `MockPaymentProviderService`
   are guarded by `@Profile("local")` — they are not loaded in production deployments.

---

## Assumptions

- **Single-tenant checkout.** No user/tenant isolation; `cartId` and `orderId` are globally unique
  `Long`s. A real deployment would add ownership checks driven by authenticated identity (see
  *Security Hardening* in the design doc).

- **At-least-once webhook delivery.** The provider may deliver the same webhook twice. The service is
  designed to be idempotent for any number of repeats. Ordered delivery is **not** assumed — a stale
  `PENDING` redelivery after a `CONFIRMED` is rejected by the terminal-state guard.

- **The payment provider is synchronous on `startPayment`.** It returns an `externalPaymentId`
  synchronously; the actual confirmation arrives later via webhook.

- **One Order per Cart.** `OrderService.createOrder` is idempotent on `cartId` — a re-submitted
  checkout returns the existing order.

- **Currency and localisation are out of scope.** Amounts are `BigDecimal` with no currency code;
  product pricing is supplied by the client in `AddItemRequest.price`. A real system would fetch price
  + currency server-side.

- **No authentication yet.** All endpoints are open. JWT auth and HMAC webhook signature verification
  are planned (see *Strategic Improvements* in the design doc).

- **Schema is managed manually.** `ddl-auto: validate` catches drift but does not migrate. Production
  deployment requires Flyway/Liquibase — not yet wired.

- **No message broker.** All cross-module communication is in-process. Event-driven decoupling is a
  future direction, not a current assumption.

---

## Further Documentation

Full technical design — layered architecture, UML state diagrams, sequence diagrams, webhook
idempotency decision flow, edge cases, and strategic improvements (JWT auth, event-driven design,
resilience, observability) — is in:

- [docs/checkout-service-design.docx](docs/checkout-service-design-v2.docx)

