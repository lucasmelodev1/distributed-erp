# CLAUDE.md

## Commands

Maven Wrapper, JDK 25.

```bash
./mvnw compile                           # compile only
./mvnw test -Dtest=TenantControllerTest  # one test class (use while iterating)
./mvnw test                              # all tests (~2 min; starts 4 containers, run once at the end)
./mvnw verify                            # full Maven verification
```

Tests import `TestcontainersConfiguration` (MySQL 8.0, Kafka, Redis, Grafana), so Docker must be running. The optional local stack is `docker compose up`, which reads `.env`. Never commit `.env` or credentials.

## Architecture

Hexagonal (ports & adapters) per feature module (`tenant`, `user`):

- `domain/`: entities, value objects, business rules. JPA annotations are allowed.
- `port/inbound/`: use-case interfaces consumed by controllers (e.g. `TenantManagementPort`, `AuthPort`). There is no `port/outbound/`; repositories are injected directly.
- `adapter/inbound/`: application services implementing inbound ports (e.g. `TenantService`).
- `adapter/outbound/persistence/`: Spring Data repositories.
- `adapter/web/`: controllers and `dto/` (records named `{Action}{Request|Response}`).
- `config/`: feature-specific Spring config (tenant context, interceptor).

Rules:
- Controllers depend on ports, never on service implementations.
- Ports reference domain types or their own records, never web DTOs.
- Services orchestrate (fetch, call domain methods, persist). Business rules live on the domain class.

Non-obvious shared packages:
- `entity/`: only `@MappedSuperclass` bases (`BaseEntity`, `NoIdBaseEntity`), no concrete entities.
- `telemetry/`: OpenTelemetry and request filters, imported explicitly by `DemoApplication`.
- `utils/`: `HashUtils`, `TokenService`, UUIDv7 generator. `email/`: `EmailService`.

## Domain modeling

- Prefer rich domain models: state transitions and invariants are methods on the entity.
  - Good: `authToken.consume()`, `tenant.inviteUser(email, roles)`, `invitation.isValid()`
  - Bad: `authToken.setConsumedAt(Instant.now())` in a service, then `repository.save(...)`
  - Some existing entities (e.g. `AuthToken`) still expose setters. When you touch one, move the rule onto the entity rather than copying the setter pattern.
- Value objects (`TokenHash`, composite ID records) live in `domain/` next to their entity.
- Join entities that only carry foreign keys (`TenantUserRole`, `TenantRolePermission`, `TenantInvitationRole`) are plain anemic classes with a composite ID record. They do not extend the base entities.
- When creating a new entity, ask whether it has business rules; if so, model them as methods from the start.

## Conventions

- Entities extend `BaseEntity` (id + timestamps). `NoIdBaseEntity` is for entities with timestamps but another kind of ID. `Permission` uses its string code as ID.
- UUID IDs use `@UuidV7Id`; the application generates them, the database has no UUID default.
- Soft-deletable entities use `@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")`. Never hard-delete them.
- Public APIs expose inactive vs active (`inactivated_at`), never soft delete. If an entity lacks `inactivated_at`, ask to add it.
- Auth codes are six-digit numeric values stored only via `HashUtils.hash()` / `TokenHash`, never plaintext.
- `created_at`/`updated_at` are not auto-populated (JPA auditing is not enabled), so don't assert on them in tests.

## Database (MySQL 8.0, Aurora MySQL v3 compatible)

- Schema changes go in a new `src/main/resources/db/migrations/V<timestamp>__<description>.sql`. Never rely on Hibernate schema updates (`ddl-auto: validate`), and never edit a migration that has already been applied.
- Every `CREATE TABLE` ends with `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci`, so it does not depend on server defaults.
- UUIDs are `BINARY(16)`; timestamps are `DATETIME(6)` in UTC.
- Use `VARCHAR`, never `TEXT`, for indexed or FK columns. Hashes and codes use `CHARACTER SET ascii COLLATE ascii_bin`, and FK columns must match their parent's type and collation.
- No partial or functional indexes. Email comparison is case-insensitive via the collation.
- DDL auto-commits and cannot be rolled back, so keep each migration small.
- Native queries must not return booleans (MySQL returns numbers); return a count and compare in a `default` method.

## Tenant security

- Sessions are Redis-backed Spring sessions.
- Annotate tenant-scoped controller methods with `@TenantAware`. The interceptor requires a valid `X-Tenant-Id`, verifies membership, sets `TenantContext`, and checks every permission in `withPermissions` (e.g. `@TenantAware(withPermissions = "tenants:update")`).
- `/api/v1/auth/**` bypasses tenant validation. Security also permits `/v3/api-docs/**`, `/swagger-ui/**`, and `/scalar/**`.

## Testing

- `BaseIntegrationTest`: MockMvc integration tests; imports Testcontainers and is `@Transactional`. Attach the user per request with Spring Security's `user(...)` postprocessor.
- `BaseControllerTest`: random-port controller tests on the `test` profile, with helpers for the `SESSION` cookie and tenant headers. Use `RestTestClient`.
- The `test` profile swaps in `EmailServiceTestImpl` and `TokenServiceTestImpl`, so generated numeric codes are all zeroes.
- Create users through repositories/services. Use the tenant creation endpoint when the test is meant to exercise tenant initialization and role/permission setup.

## Adding things

New use case: inbound port, then service in `adapter/inbound/`, then controller method (with `@TenantAware` if tenant-scoped), then request/response records in `adapter/web/dto/`, then a controller test.

New table: new migration (see Database), entity in `domain/` extending `BaseEntity`, repository in `adapter/outbound/persistence/`, then run a test so `ddl-auto: validate` checks the mapping.
