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

Tests import `TestcontainersConfiguration`, which starts PostgreSQL, Kafka, Redis, and Grafana containers. Docker must be running for test/verify commands. The optional local stack is `docker compose up` and reads `.env` for the app service; do not commit `.env` or credentials.

## Structure

- `src/main/java/com/example/demo/config`: Spring Security, MVC, exception, and OpenAPI configuration.
- `tenant`: tenant membership, invitations, and authorization (`tenantrole`, `tenantuserrole`, `tenantrolepermission`, `permission`).
- `user`: session authentication, registration, password reset, and email verification.
- `telemetry`: OpenTelemetry and request/context filters; `DemoApplication` imports these configurations.
- `src/main/resources/db/migrations`: Flyway migrations. `application.yaml` enables out-of-order migrations and JPA `ddl-auto: validate`.

## Conventions

- Add schema changes as a new `V<timestamp>__<description>.sql` migration; never rely on Hibernate schema updates.
- Use `@UuidV7Id` for UUID entity IDs. `Permission` uses its string code as its ID; join entities without `updatedAt` do not use the base entities.
- Use `@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")` for soft-deletable entities; do not hard-delete them.
- Public APIs should always use inactive vs active status rather than soft deleting itself. If an entity does not have an inactivated_at column, ask to add it.
- Keep request/response DTOs as records under the feature's `dto` package, named `{Action}{Request|Response}`.
- Auth codes are six-digit numeric values and must be stored through `TokenUtils.hash()`, never as plaintext. In the test environment generated numeric codes are all zeroes.

## Tenant Security

- Session authentication uses Redis-backed Spring sessions. Controller tests use `RestTestClient` and the `SESSION` cookie; MockMvc tests should attach the user per request with Spring Security's `user(...)` postprocessor.
- Annotate tenant-scoped controller methods with `@TenantAware`. The interceptor requires a valid `X-Tenant-Id`, verifies membership, sets `TenantContext`, and checks every permission in `withPermissions`.
- Auth paths under `/api/v1/auth/` bypass tenant validation; security explicitly permits the auth endpoints plus `/v3/api-docs/**`, `/swagger-ui/**`, and `/scalar/**`.

## Testing Patterns

- Extend `BaseIntegrationTest` for MockMvc integration tests; it imports Testcontainers and is `@Transactional`.
- Extend `BaseControllerTest` for random-port controller tests; it uses the `test` profile and provides helpers for session cookies and tenant headers.
- Prefer repository/service setup for users and the tenant creation endpoint when the test is intended to exercise tenant initialization and role/permission setup.
