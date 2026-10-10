# AGENTS.md

## Commands

The project uses Maven Wrapper and requires JDK 25 (`pom.xml` sets `java.version` to 25).

```bash
./mvnw compile                           # compile only
./mvnw test                              # all tests
./mvnw test -Dtest=TenantControllerTest  # one test class
./mvnw verify                            # full Maven verification
./mvnw clean install                     # clean local build/install
```

Tests import `TestcontainersConfiguration`, which starts MySQL 8.0, Kafka, Redis, and Grafana containers. Docker must be running for test/verify commands. The optional local stack is `docker compose up` and reads `.env` for the app service; do not commit `.env` or credentials.

## Structure

The project follows hexagonal architecture (ports & adapters) per feature module.

### Package layout

Each feature (`tenant`, `user`) uses this internal structure:
- `domain/`: Domain entities, value objects, and business rules. JPA annotations are allowed here.
- `port/inbound/`: Use-case interfaces consumed by controllers (e.g. `AuthPort`, `TenantManagementPort`).
- `port/outbound/`: Infrastructure interfaces for things like persistence or external services (currently empty — repositories are injected directly).
- `adapter/inbound/`: Application services implementing inbound ports.
- `adapter/outbound/persistence/`: Spring Data repository interfaces.
- `adapter/web/`: REST controllers, DTOs, and web-specific wiring.
- `config/`: Feature-specific Spring configuration (e.g. tenant context, interceptor).

### Other packages
- `config`: Global Spring Security, MVC, exception handling, and OpenAPI configuration.
- `entity`: Shared `@MappedSuperclass` base classes (`BaseEntity`, `NoIdBaseEntity`) — not concrete entities.
- `telemetry`: OpenTelemetry and request/context filters; `DemoApplication` imports these configurations.
- `utils`: Shared utilities (hashing, UUID generation, email validation).
- `src/main/resources/db/migrations`: Flyway migrations. `application.yaml` enables out-of-order migrations and JPA `ddl-auto: validate`.

## Architecture

- Controllers depend on inbound **port** interfaces, not on service implementations directly.
- Application services implement inbound ports and orchestrate domain logic + outbound adapters.
- Keep web-layer DTOs in `adapter/web/dto/` — port interfaces should reference domain types or their own request/response records, not web DTOs.
- When adding a new use case, create an inbound port first, then implement it in an adapter service.

## Domain Modeling

- Domain entities live in the `domain/` package of each feature module, not in the global `entity/` package.
- Prefer rich domain models: business rules, state transitions, and invariants belong on the domain class, not in services.
  - Good: `authToken.consume()`, `tenant.inviteUser(email, roles)`, `invitation.isValid()`
  - Bad: `authToken.setConsumedAt(Instant.now())` in a service, then `repository.save(authToken)`
- Services should orchestrate: fetch entities, call domain methods, persist results. Services should not contain domain rules.
- Value objects (e.g. `TokenHash`, composite ID records) belong in `domain/` alongside their entities.
- Join entities (`TenantUserRole`, `TenantRolePermission`, `TenantInvitationRole`) that only carry foreign keys are acceptable as anemic — they have no behavior to push into.

## Conventions

- Add schema changes as a new `V<timestamp>__<description>.sql` migration; never rely on Hibernate schema updates.
- Migrations target MySQL 8.0 (Aurora MySQL v3): InnoDB, `utf8mb4` / `utf8mb4_0900_ai_ci`, UUIDs as `BINARY(16)`, timestamps as `DATETIME(6)` stored in UTC, `VARCHAR` (never `TEXT`) for indexed or FK columns, and `ascii_bin` for hashes and codes. No partial or functional indexes. DDL auto-commits, so keep migrations small.
- Use `@UuidV7Id` for UUID entity IDs. `Permission` uses its string code as its ID; join entities without `updatedAt` do not use the base entities.
- Use `@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")` for soft-deletable entities; do not hard-delete them.
- Public APIs should always use inactive vs active status rather than soft deleting itself. If an entity does not have an inactivated_at column, ask to add it.
- Keep request/response DTOs as records under the feature's `dto` package, named `{Action}{Request|Response}`.
- Auth codes are six-digit numeric values and must be stored through `TokenUtils.hash()`, never as plaintext. In the test environment generated numeric codes are all zeroes.
- When creating a new domain entity, ask whether it has business rules. If yes, model them as methods on the class from the start.
- New entities should extend `BaseEntity` (or `NoIdBaseEntity` for join tables without `updatedAt`).

## Tenant Security

- Session authentication uses Redis-backed Spring sessions. Controller tests use `RestTestClient` and the `SESSION` cookie; MockMvc tests should attach the user per request with Spring Security's `user(...)` postprocessor.
- Annotate tenant-scoped controller methods with `@TenantAware`. The interceptor requires a valid `X-Tenant-Id`, verifies membership, sets `TenantContext`, and checks every permission in `withPermissions`.
- Auth paths under `/api/v1/auth/` bypass tenant validation; security explicitly permits the auth endpoints plus `/v3/api-docs/**`, `/swagger-ui/**`, and `/scalar/**`.

## Testing Patterns

- Extend `BaseIntegrationTest` for MockMvc integration tests; it imports Testcontainers and is `@Transactional`.
- Extend `BaseControllerTest` for random-port controller tests; it uses the `test` profile and provides helpers for session cookies and tenant headers.
- Prefer repository/service setup for users and the tenant creation endpoint when the test is intended to exercise tenant initialization and role/permission setup.
