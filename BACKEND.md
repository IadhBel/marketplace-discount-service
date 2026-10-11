# BACKEND — Functional and Technical Specification

Working specification for the backend of the Carrefour Java Kata (see `instruction.md`). It contains the functional specification (sections 1–2, business rules summarized in 2.8, and acceptance criteria in section 11), the technical specification (sections 3–8), the work items by feature (section 11), the coding rules (section 12), the best practices (section 13), the Java 25 notes (section 14) and the CI/CD pipeline (section 15). The implementation status of each item is tracked in section 11: nothing is described as done unless it is coded and tested.

The repository is a monorepo: `backend/` (Spring Boot), `docker-compose.yml` and `README.md` at the root. A `frontend/` (Vue.js) folder will be added later; it is out of scope of this document.

**Language convention:** documentation, code comments, log messages, API error messages and commit messages are all written in English.

## 1. Scope

| Requirement from the brief | Translation |
|---|---|
| A customer can add a discount code before checkout and get a discount | `POST /api/discounts/apply` computes a discount on a cart |
| Discount codes have restrictions on certain products | Each code references the list of eligible products |

The **MVP** covers these two points and nothing more. Extensions are listed in section 2.7 and are only built once the MVP works.

The brief has no user account, order or payment: checkout itself is out of scope. The backend only computes a discount on a cart.

### Traceability to the brief (`instruction.md`)

| Requirement of the brief | Where it is covered |
|---|---|
| Story 1: add a discount code before checkout, it applies a discount | Rules 2.1–2.5, ticket FEAT-1 (AC-1.x), endpoint 4.1 |
| Story 2: discount codes have restrictions on certain products | Rule 2.3, ticket FEAT-1 (AC-2.x), table `discount_code_product` (section 5) |
| Recommended time box of 2 hours | Priorities of section 11.0: the MVP (P0) comes first, everything else is cut if time is short |
| Stories have no acceptance criteria: define them | Business rules RG-01 to RG-17 (section 2.8) and acceptance criteria (section 11) |
| The MVP must contain business logic, not a handoff | Domain model and `DiscountPolicy` (section 3.1), computation rules 2.4 |
| Demonstrate that the solution meets the MVP | Tests mapped to acceptance criteria (section 7 and section 11), demo requests in the README (FEAT-1) |
| Missing information: decide and stay consistent | Assumptions 2.6 and decisions to confirm (section 10) |
| Bonus features are optional, MVP is mandatory | Priorities of section 11.0: bonus tickets (P3) start only once FEAT-1 is done |
| Java 25, Spring Boot | Section 14 and `backend/build.gradle` (toolchain 25, Spring Boot 4.1) |
| Java Clean Architecture | Section 3 (hexagonal, dependency rules 3.2) and coding rules (section 12) |
| README.md with intricacies and launch instructions | Root `README.md`, kept in sync with what is really implemented |
| Small commits with meaningful messages | Commit rules (section 13.5) |
| Vue.js | Ticket FEAT-2 (P1), specified separately once the backend contract is stable |
| (Not in the brief) Secured API | Decision for the MVP: JWT authentication and `CUSTOMER` role (section 6, RG-17, AC-S.x) |

## 2. Business rules

### 2.1 Discount code

A discount code (`DiscountCode`) contains:

| Field | Type | Rule |
|---|---|---|
| `code` | text | Unique. Normalized on input: `trim` then upper case (`summer10` = `SUMMER10`) |
| `percentage` | integer | Discount percentage, from 1 to 100 |
| `active` | boolean | An inactive code is rejected |
| `startsAt` | instant (UTC), optional | If present, the code is not valid before this date |
| `expiresAt` | instant (UTC), optional | If present, the code is not valid from this date on |
| `eligibleProductIds` | set of product identifiers | Products the code applies to (see 2.3) |

The only discount type is **percentage** (no fixed amount in the MVP).

### 2.2 Cart

The client sends lines `(productId, quantity)`. The server looks up the name and unit price of each product in the PostgreSQL catalog. **The client never sends a price.**

Validations:
- the cart contains at least one line;
- `quantity` is an integer between 1 and 99;
- a `productId` appears only once (duplicate = 400 error, lines are not silently merged);
- every `productId` exists in the catalog, otherwise `PRODUCT_NOT_FOUND`;
- the catalog unit price is strictly positive (database constraint and domain check).

### 2.3 Eligible products and restrictions

- A product is eligible if its identifier is in the code's `eligibleProductIds`.
- **Mixed cart**: the discount applies to eligible lines only.
- **No eligible product in the cart**: the code is rejected (`NO_ELIGIBLE_PRODUCT`); there is no silent 0 discount.
- Assumption: a code with an empty product list is valid for **all** products (to be confirmed, see section 10).

### 2.4 Computation

All monetary values are `BigDecimal` (never `double`/`float`), 2 decimals, single currency EUR.

```
lineTotal      = unitPrice × quantity
subtotal       = Σ lineTotal
eligibleAmount = Σ lineTotal of eligible lines
discountAmount = round(eligibleAmount × percentage / 100)   // HALF_UP, 2 decimals
total          = subtotal − discountAmount
```

Rounding is applied **once**, on the total eligible amount, not line by line, to avoid rounding drift. Since `percentage ≤ 100`, `discountAmount ≤ eligibleAmount ≤ subtotal`, so the total is never negative.

Example: P-001 (3.99 € × 2) + P-003 (2.50 € × 1), code SUMMER10 (10 %) eligible on P-001 only:
- `subtotal` = 7.98 + 2.50 = 10.48
- `eligibleAmount` = 7.98
- `discountAmount` = 0.798 → 0.80
- `total` = 9.68

### 2.5 Invalid codes

Check order (the first failing check gives the error): existence of the code, validity of the cart (products exist, cart invariants of 2.2), activation, validity dates, eligibility.

| Case | Error |
|---|---|
| Unknown code | `DISCOUNT_CODE_NOT_FOUND` |
| `active = false` | `DISCOUNT_CODE_INACTIVE` |
| Before `startsAt` or from `expiresAt` on | `DISCOUNT_CODE_EXPIRED` |
| No eligible line | `NO_ELIGIBLE_PRODUCT` |

The reference date comes from an injected `Clock` (testable), never from `Instant.now()` inside the domain.

### 2.6 Assumptions

- **No stacking**: one code per cart and per call.
- **No minimum cart amount** in the MVP.
- **No usage limit** (number of uses per code or per customer) in the MVP: the call is a computation with no side effect, nothing is consumed. It is therefore not a "checkout".
- The discount is **computed on demand** and not persisted.
- The computation is stateless: no cart is stored on the server.

### 2.7 Optional extensions (out of MVP)

- Minimum cart amount.
- Fixed-amount discount.
- Usage limit per code.
- Stackable codes.
- ArchUnit test enforcing the dependency rules of section 3.2.
- Business metrics (section 13.2).

### 2.8 Business rules (RG) — summary and traceability

Each rule is enforced in exactly one place, and is proven by the acceptance criteria of section 11.

| Id | Rule | Proven by | Enforced in |
|---|---|---|---|
| RG-01 | A code is matched after `trim` and upper-casing | AC-1.2 | `DiscountCode.normalize` |
| RG-02 | An unknown code is rejected | AC-1.4 | `ApplyDiscountService` |
| RG-03 | An inactive code is rejected | AC-1.5 | `DiscountCode.assertUsableAt` |
| RG-04 | A code is valid from `startsAt` (inclusive) to `expiresAt` (exclusive); both bounds are optional | AC-1.6 | `DiscountCode.assertUsableAt` |
| RG-05 | The percentage is an integer from 1 to 100 | Data integrity (invalid data → 500) | `DiscountCode` |
| RG-06 | A cart has at least one line | AC-1.7 | `Cart` |
| RG-07 | A quantity is an integer from 1 to 99 | AC-1.7 | `CartItem` |
| RG-08 | A product appears at most once in a cart | AC-1.7 | `Cart` |
| RG-09 | Every product of the cart exists; name and price come from the server catalog, never from the client | AC-1.8, AC-1.9 | `ApplyDiscountService`, `LoadProductsPort` |
| RG-10 | A catalog unit price is strictly positive | Data integrity (invalid data → 500) | `Product`, database constraint |
| RG-11 | A product is eligible if the code lists it; a code listing no product applies to every product | AC-2.1, AC-2.3 | `DiscountCode.isEligibleFor` |
| RG-12 | The discount applies to eligible lines only | AC-2.1 | `DiscountPolicy` |
| RG-13 | A code with no eligible line in the cart is rejected | AC-2.2 | `DiscountPolicy` |
| RG-14 | `discountAmount` = `eligibleAmount × percentage / 100`, rounded HALF_UP to 2 decimals, once | AC-1.3 | `Money.percentage`, `DiscountPolicy` |
| RG-15 | `total` = `subtotal − discountAmount`, never negative | AC-1.1, AC-2.4 | `DiscountPolicy` |
| RG-16 | One code per cart (no stacking) | Contract: the request carries a single `code` | REST contract (4.1) |
| RG-17 | Only an authenticated user with the `CUSTOMER` role can apply a discount code | AC-S.1 to AC-S.4 | `SecurityConfiguration` |

## 3. Architecture

Clean Architecture with a hexagonal approach (Ports & Adapters) and pragmatic DDD, built around one use case. Modular Spring Boot monolith: no microservices, no separate Gradle modules from day one. Boundaries are carried by packages.

Flow: `REST controller → ApplyDiscountUseCase (inbound port) → ApplyDiscountService → Domain (DiscountPolicy) + outbound ports → JPA adapters → PostgreSQL`. Compile-time dependencies always point towards the domain and the ports, never towards the adapters.

### 3.1 Layers

**Domain** — plain Java, no Spring, JPA or REST DTO.
- Models: `Cart`, `CartItem`, `Product`, `DiscountCode`, `DiscountResult`, `Money`.
  - `Cart` enforces its invariants (at least one line, no duplicate product, quantity 1..99).
  - `Money` wraps a 2-decimal `BigDecimal` and the HALF_UP rounding. It is kept only if it really simplifies the computations.
  - `DiscountCode` owns its validity rules (activation, dates) and product eligibility.
- `DiscountPolicy`: pure domain service that computes the discount of a `Cart` for a `DiscountCode` (rules of section 2.4). It receives a `Clock` through its constructor, never a hard-coded `now()`.
- Exceptions: `DiscountCodeNotFoundException`, `DiscountCodeInactiveException`, `DiscountCodeExpiredException`, `NoEligibleProductException`, `ProductNotFoundException`, `InvalidCartException` (cart invariants: empty cart, duplicated product, invalid quantity). They are mapped to HTTP status codes by the web adapter only.

**Application** — orchestration, one main use case.
- Inbound port: `ApplyDiscountUseCase` (`ApplyDiscountResult apply(ApplyDiscountCommand)`).
- `ApplyDiscountCommand(String code, List<CartItemCommand> items)` and `CartItemCommand(String productId, int quantity)`: immutable records, independent from REST DTOs.
- `ApplyDiscountResult`: carries everything the REST contract exposes (lines, `subtotal`, `eligibleAmount`, `discountAmount`, `total`, percentage, currency).
- Outbound ports: `LoadDiscountCodePort` (`Optional<DiscountCode> loadByCode(String)`) and `LoadProductsPort` (`List<Product> loadByIds(Collection<String>)`, batch loading: one query, not one per item).
- Application services are plain classes (no Spring annotation, constructor injection through Lombok `@RequiredArgsConstructor`). They are declared as beans in `shared/config`, which keeps the application layer independent from the framework.
- `ApplyDiscountService`: normalizes the code, loads it (otherwise `DiscountCodeNotFoundException`), loads the products in one batch (otherwise `ProductNotFoundException` for missing identifiers), builds the `Cart`, calls `DiscountPolicy`, returns the result. No computation rule lives in this service.

**Adapter** — framework and technical code.
- `adapter/in/web`: REST controller (`DiscountController`), request/response DTOs, MapStruct mapper, `@RestControllerAdvice`. HTTP validation (Bean Validation) and mapping only.
- `adapter/out/persistence`: JPA entities, Spring Data repositories, adapters implementing the outbound ports, MapStruct mapper JPA ↔ domain.
- MapStruct is used at the REST and persistence boundaries, never to implement a business rule.
- Lombok is limited to boilerplate (`@RequiredArgsConstructor` and `@Slf4j` on services and adapters). Domain models stay explicit (records or classes), without Lombok annotations that would hide invariants.

**Shared / configuration**: bean declarations (`Clock`, `DiscountPolicy`, `ApplyDiscountService`), security (`SecurityConfiguration`), OpenAPI (TECH-1), in `shared/config`.

### 3.2 Dependency rules

| Component | Responsibility | Depends on |
|---|---|---|
| REST controller | HTTP validation, DTO mapping, response codes | `ApplyDiscountUseCase` |
| `ApplyDiscountService` | Use case orchestration | domain, outbound ports |
| `DiscountPolicy` | Computation and eligibility check | domain only |
| `LoadDiscountCodePort`, `LoadProductsPort` | Data access contract | domain |
| Persistence adapters | JPA/PostgreSQL implementation of the ports | ports, domain, Spring Data |

The domain knows nothing about Spring, JPA, PostgreSQL or REST DTOs. The application service knows nothing about persistence implementations.

### 3.3 Project structure

Target structure of the backend once FEAT-1 (and the tickets marked in brackets) are done. Package-by-feature: one `discount` business context, split hexagonally inside; `shared` only holds technical configuration. Each class belongs to exactly one layer and the dependency rules of 3.2 apply. Commands and results live in their own packages: they are the light CQRS of section 3.4.

**Production code**

```
backend/src/main/java/com/carrefour/discountkata/
├── DiscountKataApplication.java
├── discount/
│   ├── domain/                              plain Java, no framework
│   │   ├── model/
│   │   │   ├── Money.java                   2-decimal amount, HALF_UP rounding (RG-14)
│   │   │   ├── Product.java                 id, name, unit price > 0 (RG-10)
│   │   │   ├── CartItem.java                product + quantity 1..99 (RG-07)
│   │   │   ├── Cart.java                    ≥ 1 line, no duplicate product (RG-06, RG-08)
│   │   │   ├── DiscountCode.java            normalization, validity, eligibility (RG-01, 03, 04, 05, 11)
│   │   │   └── DiscountResult.java          lines with eligibility, amounts
│   │   ├── policy/
│   │   │   └── DiscountPolicy.java          computation (RG-12 to RG-15)
│   │   └── exception/
│   │       ├── DiscountException.java       sealed root of the business errors
│   │       ├── DiscountCodeNotFoundException.java
│   │       ├── DiscountCodeInactiveException.java
│   │       ├── DiscountCodeExpiredException.java
│   │       ├── NoEligibleProductException.java
│   │       ├── ProductNotFoundException.java
│   │       └── InvalidCartException.java
│   ├── application/                         orchestration, no Spring annotation
│   │   ├── command/
│   │   │   ├── ApplyDiscountCommand.java    write side of the use case (CQRS)
│   │   │   └── CartItemCommand.java
│   │   ├── result/
│   │   │   └── ApplyDiscountResult.java     outcome returned by the use case
│   │   ├── port/
│   │   │   ├── in/
│   │   │   │   └── ApplyDiscountUseCase.java
│   │   │   └── out/
│   │   │       ├── LoadDiscountCodePort.java
│   │   │       └── LoadProductsPort.java
│   │   └── service/
│   │       └── ApplyDiscountService.java    implements ApplyDiscountUseCase (RG-02, RG-09)
│   └── adapter/
│       ├── in/
│       │   └── web/
│       │       ├── DiscountController.java  POST /api/discounts/apply
│       │       ├── dto/
│       │       │   ├── ApplyDiscountRequest.java
│       │       │   ├── CartItemRequest.java
│       │       │   ├── ApplyDiscountResponse.java
│       │       │   └── ErrorResponse.java   single error format (4.2)
│       │       ├── mapper/
│       │       │   └── DiscountWebMapper.java          MapStruct: DTO ↔ command / result
│       │       ├── error/
│       │       │   └── ApiExceptionHandler.java        exceptions → HTTP status + error code
│       │       └── metrics/
│       │           └── DiscountMetrics.java            [BONUS-1] business metrics
│       └── out/
│           └── persistence/
│               ├── entity/
│               │   ├── ProductEntity.java
│               │   └── DiscountCodeEntity.java         with its eligible product ids
│               ├── repository/
│               │   ├── ProductJpaRepository.java
│               │   └── DiscountCodeJpaRepository.java
│               ├── mapper/
│               │   └── DiscountPersistenceMapper.java  MapStruct: entity ↔ domain
│               └── adapter/
│                   ├── DiscountCodePersistenceAdapter.java   implements LoadDiscountCodePort
│                   └── ProductPersistenceAdapter.java        implements LoadProductsPort
└── shared/
    └── config/
        ├── BeanConfiguration.java           Clock, DiscountPolicy, ApplyDiscountService
        ├── SecurityConfiguration.java       JWT resource server, CUSTOMER rule (RG-17)
        ├── SecurityErrorHandler.java        401 / 403 in the error format
        └── OpenApiConfiguration.java        [TECH-1] bearer security scheme

backend/src/main/resources/
├── application.yml
└── db/
    └── migration/
        ├── V1__create_schema.sql            product, discount_code, discount_code_product
        └── V2__insert_demo_data.sql         demo products and codes (section 5)
```

**Tests** (same packages as the production code)

```
backend/src/test/java/com/carrefour/discountkata/
├── discount/
│   ├── domain/
│   │   ├── model/
│   │   │   ├── MoneyTest.java
│   │   │   ├── CartTest.java
│   │   │   ├── CartItemTest.java
│   │   │   └── DiscountCodeTest.java
│   │   └── policy/
│   │       └── DiscountPolicyTest.java
│   ├── application/
│   │   └── service/
│   │       └── ApplyDiscountServiceTest.java          Mockito, ports mocked
│   └── adapter/
│       ├── in/
│       │   └── web/
│       │       ├── DiscountControllerSecurityTest.java   @WebMvcTest + Spring Security Test
│       │       ├── DiscountApiIT.java                    REST Assured, end to end
│       │       └── OpenApiDocumentationIT.java           [TECH-1] /v3/api-docs
│       └── out/
│           └── persistence/
│               └── DiscountPersistenceIT.java         Flyway + JPA on PostgreSQL
└── support/
    ├── PostgresTestConfiguration.java       Testcontainers PostgreSQL (@ServiceConnection)
    └── TestJwtFactory.java                  signs test tokens with a generated RSA key
```

Naming rules:
- `*Test`: unit and web-layer tests, no infrastructure, run by `test`.
- `*IT`: integration tests that start Spring and PostgreSQL (Docker), run by `integrationTest`.
- Classes are named by their role in the architecture: `*UseCase` (inbound port), `*Port` (outbound port), `*Command` / `*Result` (CQRS input and output of a use case), `*Service` (application service), `*Adapter` (outbound adapter), `*Entity` and `*JpaRepository` (persistence), `*Request` / `*Response` (REST DTOs), `*Mapper` (MapStruct), `*Configuration` (Spring).

No interface without a concrete purpose: `DiscountPolicy` is a concrete domain class, not a port. One outbound adapter per port (interface segregation).

**Repository**

```
.
├── backend/                     Spring Boot application (Gradle subproject)
│   ├── src/                     see above
│   ├── build.gradle
│   ├── Dockerfile               image of the backend
│   └── .dockerignore
├── frontend/                    [FEAT-2] Vue.js application, not started
├── keycloak/
│   └── discount-realm.json      realm imported by the local Keycloak
├── gradle/wrapper/              Gradle wrapper
├── docker-compose.yml           local PostgreSQL and Keycloak
├── .gitlab-ci.yml               CI/CD pipeline (section 15)
├── settings.gradle              Gradle multi-project (includes backend)
├── gradlew, gradlew.bat
├── README.md
├── BACKEND.md
└── instruction.md
```

### 3.4 CQRS

Light CQRS: the application layer separates what a client asks the system to do (commands) from what it reads (queries), with dedicated input and output types, but without the infrastructure of a full CQRS.

| Element | In this project |
|---|---|
| Command | `ApplyDiscountCommand` (+ `CartItemCommand`): the intent of the customer, immutable, validated by the domain |
| Command handler | `ApplyDiscountService`, behind the `ApplyDiscountUseCase` inbound port |
| Result | `ApplyDiscountResult`: what the use case returns, independent from the REST DTOs |
| Query side | None: the MVP has no read use case (the product listing was removed, as not required by the brief) |

Choices to be able to defend:
- **Why "command" for a computation without side effect?** Strictly speaking, applying a code today only computes a price: it changes nothing. It is modelled as a command because it expresses the customer's intent on their cart, and because it becomes a real write as soon as a usage limit per code exists (extension 2.7): the same use case will then record the redemption, without changing its contract.
- **What is not done, on purpose:** no command or query bus, no generic handler, no separate read database, no event sourcing. With one use case, they would add indirection and no value (YAGNI).
- **How it would grow:** a query use case (for example, listing the active codes) would get its own inbound port, query type and read model, possibly read directly from optimized SQL views, without going through the domain model. A redemption would publish a `DiscountApplied` domain event, which other systems (loyalty, CRM) could consume in real time: that is the step towards an event-driven architecture.

## 4. REST API

Base path: `/api`. Format: JSON, UTF-8. Amounts are JSON decimal numbers with 2 decimals.

### 4.1 `POST /api/discounts/apply`

Requires a bearer token with the `CUSTOMER` role (section 6): `Authorization: Bearer <JWT>`.

Request:

```json
{
  "code": "SUMMER10",
  "items": [
    { "productId": "P-001", "quantity": 2 },
    { "productId": "P-003", "quantity": 1 }
  ]
}
```

Response 200:

```json
{
  "code": "SUMMER10",
  "discountPercentage": 10,
  "currency": "EUR",
  "lines": [
    { "productId": "P-001", "name": "Ground coffee 250 g", "unitPrice": 3.99, "quantity": 2, "lineTotal": 7.98, "eligible": true },
    { "productId": "P-003", "name": "Traditional baguette", "unitPrice": 2.50, "quantity": 1, "lineTotal": 2.50, "eligible": false }
  ],
  "subtotal": 10.48,
  "eligibleAmount": 7.98,
  "discountAmount": 0.80,
  "total": 9.68
}
```

`code` in the response is the normalized code.

### 4.2 Errors

Single format:

```json
{
  "status": 422,
  "code": "NO_ELIGIBLE_PRODUCT",
  "message": "No product in the cart is eligible for discount code 'SUMMER10'.",
  "details": []
}
```

`details` is optional (list of messages, used for validation errors: field + message). Messages are in English.

| HTTP | `code` | Case |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Empty cart, quantity outside 1..99, blank code, duplicated `productId`, invalid JSON (Bean Validation at the edge, or `InvalidCartException` from the domain) |
| 401 | `UNAUTHORIZED` | Missing, malformed, expired or wrongly signed token |
| 403 | `FORBIDDEN` | Valid token without the `CUSTOMER` role |
| 404 | `DISCOUNT_CODE_NOT_FOUND` | Unknown code |
| 404 | `PRODUCT_NOT_FOUND` | `productId` not in the catalog |
| 422 | `DISCOUNT_CODE_INACTIVE` | Deactivated code |
| 422 | `DISCOUNT_CODE_EXPIRED` | Outside the validity period |
| 422 | `NO_ELIGIBLE_PRODUCT` | No eligible line |
| 500 | `INTERNAL_ERROR` | Unexpected error, no technical detail exposed |

### 4.3 Client data vs server data

| Provided by the client | Controlled by the server |
|---|---|
| `code`, `productId`, `quantity` | Unit prices, names, percentage, eligibility, validity dates, every computed amount, currency |

## 5. Persistence (PostgreSQL)

Minimal model:

| Table | Columns |
|---|---|
| `product` | `id` (PK, text), `name`, `unit_price` (`NUMERIC(10,2)`, `> 0`) |
| `discount_code` | `code` (PK, text, upper case), `percentage` (`1..100`), `active`, `starts_at` (nullable), `expires_at` (nullable) |
| `discount_code_product` | `code` (FK), `product_id` (FK), composite PK |

Migrations with **Flyway** (`db/migration`), to be confirmed (section 10). One migration creates the schema, another inserts **demo data** so the MVP can be demonstrated:

- `SUMMER10`: 10 %, active, restricted to some products;
- `WELCOME5`: 5 %, active, no restriction (if the assumption of 2.3 is confirmed);
- `EXPIRED20`: expired;
- `DISABLED15`: inactive.

JPA/Hibernate runs with `ddl-auto=validate`: the schema is managed by Flyway only.

## 6. Security

**MVP: the API is protected by JWT, with roles.** The brief does not ask for it; it is a decision for this project. The role follows the actor of the user stories: the customer who applies a discount code to their cart (`CUSTOMER`). Another actor, such as a cashier applying a code at the till, would only add a role and a rule in `SecurityConfiguration`, without touching the business code.

| Topic | Decision |
|---|---|
| Model | The backend is an OAuth2 **Resource Server**: it only validates tokens. Spring Security does not issue tokens; an authorization server does |
| Authorization server | Keycloak 26.8, realm `discount`. Locally it runs in `docker-compose` (port 8180, development mode) and imports `keycloak/discount-realm.json` at startup: role `CUSTOMER`, public client `discount-cli`, users `customer` (with the role) and `viewer` (without it) |
| Token validation | Signature checked with the public keys (JWKS) of the issuer `JWT_ISSUER_URI`, plus expiration and issuer. The backend holds no secret |
| Roles | Keycloak exposes the realm roles in a top-level `roles` claim (protocol mapper of the realm); Spring maps them to `ROLE_*` authorities through configuration (`authorities-claim-name`, `authority-prefix`), with no custom code |
| Rules | `POST /api/discounts/apply` requires `ROLE_CUSTOMER`. `/actuator/health`, `/v3/api-docs/**` and `/swagger-ui/**` are public. Every other endpoint requires authentication |
| Where | All authorization rules live in `SecurityConfiguration` (one place, by URL); controllers and services contain no role check |
| Session | Stateless (no HTTP session). CSRF protection disabled, since no cookie carries the authentication |
| Errors | 401 `UNAUTHORIZED` and 403 `FORBIDDEN` use the error format of section 4.2 |

Measures kept from the start:
- strict input validation (Bean Validation + domain rules);
- price and eligibility are never read from the request;
- no stack trace in error responses;
- CORS: not needed in development (Vite proxy); if the frontend is served from another origin, allow only that origin, configurable.

## 7. Tests

| Level | Tools | Content |
|---|---|---|
| Domain | JUnit 5 | Computation (nominal case, HALF_UP rounding, mixed cart, 100 %), validity (inactive, before/after dates, boundaries), no eligible product, empty restriction. No Mockito, no Spring. |
| Application | JUnit 5 + Mockito | `ApplyDiscountService` with mocked ports: unknown code, unknown product, orchestration, fixed `Clock` |
| Persistence integration | Testcontainers (PostgreSQL) + `@DataJpaTest`/`@SpringBootTest` | Flyway migrations, adapters, MapStruct mappings, demo data |
| Security | `@WebMvcTest` + Spring Security Test (`jwt()` request post-processor) | 401 without or with an invalid token, 403 without `CUSTOMER`, 200 with `CUSTOMER`, public endpoints reachable. Use case mocked, no infrastructure |
| HTTP | REST Assured + `@SpringBootTest(webEnvironment = RANDOM_PORT)` + Testcontainers | Contract of `POST /api/discounts/apply`: 200, 400, 401, 403, 404, 422, error format. Tokens are signed in the test with a generated RSA key and validated by a test `JwtDecoder`, so no Keycloak is needed |
| API documentation | REST Assured on `/v3/api-docs` | The OpenAPI document exposes the endpoint, its request and response schemas, its error codes and the bearer security scheme |

Priority goes to fast business tests (domain, application), completed by integration tests against real infrastructure. Each tool answers a precise need:
- **JUnit 5**: domain tests, without Spring; the most numerous.
- **Mockito**: isolates `ApplyDiscountService` from persistence; used at this level only.
- **Testcontainers**: checks the Flyway migrations and queries against a real PostgreSQL, which an in-memory database does not guarantee.
- **REST Assured**: checks the HTTP contract (status codes, error format), which is the one consumed by the frontend, and the published OpenAPI document.
- **Spring Security Test**: simulates authenticated users and roles in web-layer tests without an authorization server.

Unit tests and integration tests are separate Gradle tasks: classes named `*IT` (Testcontainers, Docker required) run with `integrationTest`, all other tests with `test`. JaCoCo merges the coverage of both for Sonar.

**WireMock**: the backend calls no external service, so WireMock is **not used** (dependency not added until an external integration exists).

## 8. Documentation and running

Prerequisites: JDK 25, Docker (PostgreSQL, Keycloak and Testcontainers).

Configuration (environment variables, with development defaults in `application.yml`):

| Variable | Purpose | Dev default |
|---|---|---|
| `DB_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/discount` |
| `DB_USER` | User | `discount` |
| `DB_PASSWORD` | Password | `discount` |
| `SERVER_PORT` | HTTP port | `8080` |
| `JWT_ISSUER_URI` | Issuer of the tokens (Keycloak realm) | `http://localhost:8180/realms/discount` |

Commands (Gradle Groovy multi-project, all run from the repository root):

```
docker compose up -d              # PostgreSQL and Keycloak
./gradlew :backend:bootRun
./gradlew :backend:test
./gradlew :backend:build              # compile, unit and integration tests, format check
./gradlew :backend:integrationTest    # integration tests only (Docker required)
./gradlew :backend:spotlessApply      # format the code
./gradlew :backend:dependencyCheckAnalyze   # OWASP scan (NVD_API_KEY recommended)
```

Getting a token for the demo (local Keycloak, password grant of the development client):

```
curl -s -X POST http://localhost:8180/realms/discount/protocol/openid-connect/token \
  -d grant_type=password -d client_id=discount-cli -d username=customer -d password=customer \
  | jq -r .access_token
```

The user `viewer` gets a valid token without the `CUSTOMER` role (expected 403). The password grant and the credentials of the realm file are for local development only; Keycloak admin console: `http://localhost:8180` (`admin` / `admin`).

API documentation: Springdoc OpenAPI — Swagger UI at `http://localhost:8080/swagger-ui.html`, specification at `/v3/api-docs`.

The root `README.md` documents the full startup (PostgreSQL, backend, frontend) and the implementation choices. Only features that are actually coded and tested are documented as done.

## 9. Recommended implementation order

Work is organized by feature (vertical slice), and each feature ends with its tests and one commit (see section 11). Inside a feature:

1. Domain and application layers (rules, use case, ports).
2. Persistence adapter and Flyway migration.
3. REST adapter, validation, error handling.
4. Tests (domain, application, persistence, HTTP).
5. README update (launch instructions and demo requests).

Order of the features: by priority, as defined in section 11.0. The REST contract of section 4 must be stable before starting the frontend (FEAT-2).

## 10. Decisions to confirm before implementation

1. **Empty restriction**: is a code with no associated product valid for all products? (proposed: yes; already implemented this way, RG-11)
2. **Flyway** rather than a plain `schema.sql`. (proposed: Flyway)
3. **Single currency EUR**. (proposed: yes)
4. **Demo codes** seeded by migration (section 5). (proposed: yes)
5. **Price origin**: catalog in the database, controlled by the server. (proposed: yes)
6. **Lombok** limited to boilerplate of services and adapters (not on the domain). (decided)
7. **Language**: everything in English — docs, comments, logs, API messages, commits. (decided)
8. **Security in the MVP**: JWT Resource Server, Keycloak as issuer, role `CUSTOMER` (section 6). (decided)

## 11. Work items by feature

Status legend: **Coded** means the code exists and compiles; **Tested** means automated tests exist and pass. Nothing is "Done" until both are true.

### 11.0 Prioritization

Priorities follow the brief: the MVP is mandatory and comes first; the other requirements of the brief come next; constraints added for this project after that; bonus features last. A ticket starts only when every ticket of a higher priority is done.

| Priority | Ticket | Source | Why this rank |
|---|---|---|---|
| **P0** | FEAT-1 — Apply a discount code to a cart (US-1, US-2) | Brief, MVP | Both user stories; mandatory and must be demonstrated |
| **P1** | FEAT-2 — Vue.js frontend | Brief, requirements | Vue.js is a requirement of the brief; depends on FEAT-1 |
| **P2** | TECH-1 — API documentation (Springdoc OpenAPI) | Project constraints | Cheap and helps the reviewer try the API, but not asked by the brief |
| **P3** | BONUS-1 — Business metrics (section 13.2) | Bonus | Technical bonus |
| **P3** | BONUS-2+ — Extensions of section 2.7 (minimum amount, fixed amount, usage limit, ArchUnit…) | Bonus | Only if time remains; each one gets its own ticket and acceptance criteria before being started |

The brief recommends spending no more than two hours. This plan already exceeds that budget: if time is short, P0 is finished and demonstrated, and lower priorities are stopped rather than half-done. The README states clearly what was not done.

### FEAT-1 — Apply a discount code to a cart (`POST /api/discounts/apply`)

Covers story 1 and story 2 of the brief (they share the same endpoint and the same domain model; the restriction logic is part of the model).

**US-1** — As a customer, I apply a discount code before checkout and the discount is applied to my cart.

| Id | Acceptance criterion |
|---|---|
| AC-1.1 | A valid code applies its percentage to the eligible amount: `total = subtotal − discountAmount` |
| AC-1.2 | The code is matched ignoring case and surrounding spaces (`" summer10 "` = `SUMMER10`) |
| AC-1.3 | The discount is rounded HALF_UP to 2 decimals, once, on the total eligible amount (e.g. 10 % of 7.98 → 0.80) |
| AC-1.4 | Unknown code → 404 `DISCOUNT_CODE_NOT_FOUND` |
| AC-1.5 | Inactive code → 422 `DISCOUNT_CODE_INACTIVE` |
| AC-1.6 | Code before `startsAt` or from `expiresAt` on → 422 `DISCOUNT_CODE_EXPIRED` (`startsAt` inclusive, `expiresAt` exclusive) |
| AC-1.7 | Empty cart, quantity outside 1..99, duplicated product or blank code → 400 `VALIDATION_ERROR` |
| AC-1.8 | Unknown product → 404 `PRODUCT_NOT_FOUND` |
| AC-1.9 | Prices come from the server catalog; the request carries no price |

**US-2** — As a customer, a discount code only applies to the products it targets.

| Id | Acceptance criterion |
|---|---|
| AC-2.1 | In a mixed cart, only eligible lines count in `eligibleAmount`; each line is flagged `eligible` |
| AC-2.2 | No eligible product in the cart → 422 `NO_ELIGIBLE_PRODUCT` (no silent 0 discount) |
| AC-2.3 | A code without restriction applies to every product (assumption 2.3) |
| AC-2.4 | A 100 % code on eligible lines gives `total = subtotal − eligibleAmount`, never negative |

**Security** — As a customer, I must be authenticated with the `CUSTOMER` role to apply a discount code.

| Id | Acceptance criterion |
|---|---|
| AC-S.1 | No token → 401 `UNAUTHORIZED` |
| AC-S.2 | Malformed, expired or wrongly signed token → 401 `UNAUTHORIZED` |
| AC-S.3 | Valid token without the `CUSTOMER` role → 403 `FORBIDDEN` |
| AC-S.4 | Valid token with the `CUSTOMER` role → the request is processed (AC-1.x, AC-2.x apply) |
| AC-S.5 | `/actuator/health` and the OpenAPI documentation are reachable without a token |

**Technical tasks**

| Task | Layer | Status |
|---|---|---|
| Business exceptions | domain | Not started |
| Model (`Money`, `Product`, `Cart`, `CartItem`, `DiscountCode`, `DiscountResult`) and `DiscountPolicy` | domain | Not started |
| `ApplyDiscountCommand`, `CartItemCommand`, `ApplyDiscountResult` | application | Not started |
| Ports `ApplyDiscountUseCase`, `LoadDiscountCodePort`, `LoadProductsPort` | application | Not started |
| `ApplyDiscountService` | application | Not started |
| JPA entities, repositories, MapStruct mapper, adapters for code and products | adapter/out | Not started |
| Flyway migrations (schema and demo data) | resources | Not started |
| `DiscountController`, DTOs, MapStruct mapper, exception handler | adapter/in | Not started |
| Bean configuration (`Clock`, `DiscountPolicy`, services) | shared/config | Not started |
| `SecurityConfiguration` (rules of section 6, 401/403 in the error format) | shared/config | Not started |
| Keycloak in `docker-compose` with an imported `discount` realm (role, users, `roles` mapper) | infra | Coded, not run (no Docker on the development machine) |
| README: launch instructions and demo requests for each acceptance criterion | docs | Not started |
| Tests (see table below) | all | Not started |

**Tests proving the acceptance criteria**

| Level | Covers |
|---|---|
| Domain (JUnit 5) | AC-1.1, 1.2 (normalize), 1.3, 1.5, 1.6 (boundaries), 2.1, 2.2, 2.3, 2.4, cart invariants (1.7) |
| Application (Mockito) | AC-1.4, 1.8, orchestration of ports |
| Security (`@WebMvcTest` + Spring Security Test) | AC-S.1 to AC-S.5 |
| Persistence (Testcontainers, if a container runtime is available) | Flyway migrations, loading of code with its eligible products |
| HTTP (REST Assured) | AC-1.1, 1.4–1.9, 2.1–2.2, AC-S.1, S.3, S.4 end to end, error format |

**Commit**: `[feature] apply a discount code to a cart`

**Priority**: P0.

### FEAT-2 — Vue.js frontend

Required by the brief. Specified in a dedicated document once FEAT-1 is done and its contract is stable. Not started.

**Priority**: P1.

### TECH-1 — API documentation

| Id | Acceptance criterion |
|---|---|
| AC-T1.1 | Swagger UI is reachable at `/swagger-ui.html` and lists the endpoint with its request, response and error schemas |
| AC-T1.2 | The OpenAPI document declares the bearer JWT security scheme on the endpoint |
| AC-T1.3 | A REST Assured test on `/v3/api-docs` checks AC-T1.1 and AC-T1.2 |

**Commit**: `[feature] document the REST API with OpenAPI`. **Priority**: P2. Status: Not started (dependency already declared).

### BONUS-1 — Business metrics

| Id | Acceptance criterion |
|---|---|
| AC-B1.1 | Each applied code increments `discount.applications{outcome=applied}` and records its amount in `discount.amount{code}` |
| AC-B1.2 | Each business rejection increments `discount.applications{outcome=rejected, reason}`; validation errors are not counted |
| AC-B1.3 | Metrics are readable at `/actuator/metrics/discount.applications` |

**Commit**: `[feature] add business metrics on discount applications`. **Priority**: P3. Status: Not started.

## 12. Coding rules

**Architecture**
- Dependencies point inwards: `adapter` → `application` → `domain`. The domain imports nothing from Spring, JPA, Jackson or the REST DTOs.
- No business rule in controllers, entities or mappers. Controllers validate the HTTP format and delegate to a use case.
- Authorization rules are declared only in `SecurityConfiguration`; no role check in controllers or services.
- One use case per inbound port. Ports are named by intent (`ApplyDiscountUseCase`, `LoadDiscountCodePort`); adapters end with `Adapter`.
- No interface without a second implementation or a boundary to protect (ports are boundaries; `DiscountPolicy` is not).

**SOLID in practice**
- Single responsibility: one reason to change per class (`Money` does arithmetic, `Cart` keeps its invariants, `DiscountPolicy` computes, services orchestrate).
- Open/closed: new error cases extend the sealed `DiscountException`; new discount kinds extend the policy without changing the service.
- Liskov: exception subclasses and adapters are interchangeable with their abstractions.
- Interface segregation: one port per need (`LoadDiscountCodePort`, `LoadProductsPort`), each with a single method.
- Dependency inversion: services depend on ports and `Clock`, never on JPA repositories or `Instant.now()`.

**Code style**
- Immutable objects: `record`, `final` fields, defensive copies (`List.copyOf`). No setters.
- Invariants are checked in constructors: an invalid object cannot exist.
- Constructor injection only. No field injection, no `@Autowired` on fields.
- Money is only handled through `Money` in the domain and application layers (never `double`/`float`). Raw `BigDecimal` only appears at the boundaries: REST DTOs (JSON) and JPA entities (`NUMERIC` columns).
- Time comes from an injected `Clock`.
- Absence is expressed with `Optional`; methods never return `null` collections.
- Business errors are unchecked exceptions from the sealed `DiscountException`; the HTTP mapping lives in the web adapter only. Data corruption (invalid price, percentage out of range) raises `IllegalArgumentException` and ends as a 500.
- MapStruct only maps between boundary types; Lombok is limited to `@RequiredArgsConstructor` and `@Slf4j` (never on domain models).
- Formatting is enforced by Spotless with the Eclipse formatter (tabs, as generated by Spring Initializr): `spotlessCheck` in CI, `spotlessApply` locally; package-by-feature (`discount`), hexagonal inside.
- English everywhere: identifiers, comments, logs, API messages, commit messages.
- Comments explain a *why* or a non-obvious rule, never what the code already says.

## 13. Best practices

### 13.1 Logs (SLF4J)

Rules:
- `@Slf4j`, parameterized messages (`{}`), never string concatenation, never `System.out`.
- The domain does not log. Logs live in application services and adapters, and each event is logged **once**.
- A log line must be useful to someone operating the service. No "entering method" logs, no logs in mappers or persistence adapters (Hibernate SQL logging can be enabled through configuration when needed).
- No stack trace for expected business outcomes; no personal data (the cart content is not logged, only its size).

Levels:

| Level | When | Example |
|---|---|---|
| `DEBUG` | Flow detail, only useful while investigating | `Applying discount code 'SUMMER10' to a cart of 2 line(s)` |
| `INFO` | A completed business operation, or an expected business rejection | `Discount code 'SUMMER10' applied: subtotal=10.48, discount=0.80, total=9.68` / `Discount code rejected: reason=DISCOUNT_CODE_EXPIRED, code='EXPIRED20'` |
| `WARN` | Something abnormal that does not break the request and needs attention | `Access denied: subject=8f3c… lacks role CUSTOMER on POST /api/discounts/apply` (token subject id, never a name) |
| `ERROR` | Unexpected failure (500), always with the stack trace | `Unexpected error while handling POST /api/discounts/apply` |

A rejected code (unknown, inactive, expired, no eligible product) is a normal business outcome, not an incident: it is logged at `INFO` and counted by a metric, not raised as a `WARN`. A malformed request (400) is logged at `DEBUG` only, to avoid flooding the logs with client mistakes.

Where each log is written:

| Class | Level | Event |
|---|---|---|
| `ApplyDiscountService` | `DEBUG` | Start of an application (normalized code, number of lines) |
| `ApplyDiscountService` | `INFO` | Discount applied (code, subtotal, discount, total) |
| `ApiExceptionHandler` | `INFO` | Business rejection (error code, code typed by the customer) |
| `ApiExceptionHandler` | `DEBUG` | Validation error (field errors) |
| `ApiExceptionHandler` | `ERROR` | Unexpected exception, with stack trace |
| `SecurityErrorHandler` | `DEBUG` | 401: missing or invalid token (frequent, not an incident) |
| `SecurityErrorHandler` | `WARN` | 403: an authenticated user without the required role (worth a look: wrong role assignment or misuse) |

### 13.2 Business metrics (Micrometer)

Only metrics that answer a business question are recorded. Technical metrics (HTTP latency, JVM, connection pool) come for free with Spring Boot Actuator and are not re-implemented.

| Metric | Type | Tags | Business question |
|---|---|---|---|
| `discount.applications` | Counter | `outcome` = `applied` or `rejected`; `reason` = error code when rejected (`DISCOUNT_CODE_NOT_FOUND`, `DISCOUNT_CODE_INACTIVE`, `DISCOUNT_CODE_EXPIRED`, `NO_ELIGIBLE_PRODUCT`) | How often do customers use a code successfully, and why are codes refused? A rise of `NO_ELIGIBLE_PRODUCT` points to a badly targeted promotion; a rise of `DISCOUNT_CODE_EXPIRED` to a code still advertised after its end date. |
| `discount.amount` | Distribution summary (base unit EUR) | `code` | How much discount is granted, in total and per code? |

Rules:
- Tags have a bounded cardinality. The `code` tag is only set on `discount.amount`, i.e. for codes that exist in the database; a code typed by the customer is **never** a tag on a rejection (unknown codes would create an unbounded number of series).
- Validation errors (400) are not counted as rejections: they are client mistakes, already visible in the HTTP metrics.
- This is a bonus (BONUS-1, priority P3), built only after the MVP.
- Metrics are recorded in the web adapter (successful response and business exception handler), so that the domain and application layers stay free of Micrometer.
- Exposure: `spring-boot-starter-actuator`, endpoints `health` and `metrics` only (`/actuator/metrics/discount.applications`). A Prometheus registry is an optional extension.

### 13.3 Errors and API
- Single error format (section 4.2), stable error codes, no technical detail in 500 responses.
- Validate the HTTP format at the edge (Bean Validation) and the business invariants in the domain; never duplicate a rule.

### 13.4 Tests
- Pyramid: many fast domain tests, fewer application tests, few integration and HTTP tests.
- Test names state the behavior (`shouldRejectCode_whenExpired`), Arrange–Act–Assert layout, one behavior per test, a fixed `Clock` for date rules.
- Every acceptance criterion of section 11 has at least one test.
- A tool is used only if it answers a real need (section 7).

### 13.5 Git and delivery
- One commit per feature or endpoint, never per package; a commit leaves the build compiling.
- Message format: `[feature] ...`, `[init]`, `[build]`, `[docs]`, `[test]`, `[refactor]`, imperative mood, in English.
- The root `README.md` only documents what is actually coded and tested.

## 14. Java 25

Java 25 is a long-term-support release; the build pins it with a Gradle toolchain. Modern language features are used where they make the code simpler or safer:

| Feature (since) | Use in the project |
|---|---|
| Records (16) | Immutable models, commands, results and DTOs |
| Sealed classes (17) | `DiscountException` hierarchy, handled exhaustively |
| Pattern matching for `switch` (21) | Planned in the web exception handler to map each sealed exception to its HTTP status, with no `default` branch |
| `Stream.toList()` (16), `String.formatted` (15) | Immutable lists and readable messages |

Features introduced or finalized in Java 25 were reviewed and are **not used**, because the kata has no need for them (compact source files, module imports, flexible constructor bodies, scoped values). Records already validate their fields in compact constructors, which covers the construction needs. Preview features (stable values, structured concurrency) are not enabled. JVM improvements of the release (e.g. compact object headers) are not enabled either, as they are runtime tuning and out of scope.

## 15. CI/CD (GitLab)

Pipeline defined in `.gitlab-ci.yml`, run on every branch push:

| Stage | Job | What it does |
|---|---|---|
| build | `build` | Builds the executable jar (artifact for the image) |
| build | `format` | `spotlessCheck`: fails if the code is not formatted |
| test | `unit-tests` | `test`, JUnit report and coverage data |
| test | `integration-tests` | `integrationTest` with Testcontainers on a Docker-in-Docker service |
| quality | `sonar` | Sonar analysis with the merged JaCoCo coverage |
| quality | `dependency-check` | OWASP Dependency-Check; fails on a vulnerability with CVSS ≥ 7 |
| package | `publish-image` | Builds `backend/Dockerfile` and pushes the image to the GitLab registry, tagged with the commit SHA and the branch slug |
| deploy | `deploy` | Automatic on `develop` (i.e. after a merge into `develop`), manual on any other branch. No deployment target is defined yet: the job only prints the image to deploy |

Configuration and secrets:
- Non-secret configuration comes from GitLab CI/CD variables: `VAULT_SERVER_URL`, `VAULT_AUTH_ROLE`, `VAULT_AUTH_PATH`, `SONAR_HOST_URL`, `SONAR_PROJECT_KEY`.
- Secrets are read from HashiCorp Vault at job runtime with GitLab ID tokens (`secrets:vault`), never stored in GitLab or in the repository: `sonar_token` and `nvd_api_key` in the KV v2 engine `secret`, path `discount-kata/ci`.
- The registry login uses the job's own `CI_REGISTRY_USER` / `CI_REGISTRY_PASSWORD`, provided by GitLab for each job.
- `sonar` only runs when Sonar and Vault are configured, `dependency-check` only when Vault is configured, so the pipeline stays green on a project where they are not set up yet.
- The application itself reads its runtime configuration from environment variables (section 8); in a deployed environment, the database credentials would come from Vault as well.
