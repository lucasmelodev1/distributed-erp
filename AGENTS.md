# AGENTS.md

## Build & Run

```bash
# Build (requires Docker running for Testcontainers)
./mvnw clean install

# Run tests only
./mvnw test

# Full verification (unit + integration tests via Testcontainers)
./mvnw verify

# Run specific test class
./mvnw verify -Dtest=TenantControllerTest

# Compile only (no tests)
./mvnw compile
```

Docker must be running for `./mvnw verify` — tests spin up PostgreSQL, Kafka, Redis, and Grafana via Testcontainers.

## Architecture

**Package structure:**
```
com.example.demo
├── config/          # Spring Security, WebMvc, OpenAPI configs
├── email/           # Email service interface + implementation
├── entity/          # BaseEntity, NoIdBaseEntity (shared JPA base classes)
├── telemetry/       # OpenTelemetry, context propagation
├── tenant/          # Multi-tenancy domain
│   ├── authorization/   # Roles, permissions, role-permission mappings
│   ├── invitation/      # Tenant invitations
│   └── dto/             # Request/response records
├── user/            # Authentication domain (login, registration, tokens)
└── utils/           # UUIDv7, token hashing, env utils
```

**Multi-tenancy model:**
- Users belong to tenants via `tenant_users` (composite PK)
- Roles are scoped to tenants (`tenant_roles`)
- Users get roles in tenants via `tenant_user_roles`
- Roles have permissions via `tenant_role_permissions`
- `@TenantAware` annotation on controller methods validates `X-Tenant-Id` header + membership + permissions

**Authorization model:**
```
tenants ──── tenant_users ──── users
    │
    └── tenant_roles ──── tenant_role_permissions ──── permissions
         │
         └── tenant_user_roles (assigns roles to users in tenants)
```

## Database

- **Flyway** manages migrations: `src/main/resources/db/migrations/`
- `out-of-order: true` — migration filenames use `V<timestamp>__<description>.sql`
- JPA `ddl-auto: validate` — schema changes require a migration, never `update`
- Adding columns requires a new migration file, not `update`

## Code Patterns

- **UUIDv7 primary keys**: Use `@UuidV7Id` annotation on `@Id` fields (custom Hibernate generator in `com.example.demo.utils`)
- **Soft delete**: `@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")` — never hard-delete entities. Hibernate filters out soft-deleted entities automatically.
- **DTOs**: Java records in `{feature}/dto/` sub-package. Naming: `{Action}{Request|Response}.java`
- **Auth tokens**: 6-digit numeric codes, stored as SHA-256 hashes via `TokenUtils.hash()` — never store plaintext tokens
- **Error responses**: Use `"{\"error\":\"" + message + "}"` string concatenation (not Jackson ObjectMapper)
- **Tenant-aware endpoints**: Annotate with `@TenantAware` (optional: `withPermissions` for permission checks)

## Entity Inheritance

```
NoIdBaseEntity (createdAt, updatedAt)
└── BaseEntity (adds @Id @UuidV7Id UUID id)
    ├── Tenant, TenantRole, User
    └── Permission (uses String code as @Id, not UUID)
```

Entities WITHOUT `updatedAt` column (e.g., join tables) don't extend any base — define fields manually following the `TenantUserRole` pattern.

## Testing

- **Base class**: Extend `BaseIntegrationTest` for integration tests
- **Security**: Use `SecurityMockMvcRequestPostProcessors.user(user)` for per-request authentication — avoids session-based SecurityContext leakage between MockMvc requests
- **Test users**: Create directly via `UserRepository.save()` + `PasswordEncoder.encode()`
- **Test tenants**: Create via MockMvc POST to `/api/v1/tenant` (exercises full flow including role/permission setup)
- **@Transactional**: Tests are wrapped in a transaction that rolls back after each test

## Security

- Session-based auth with `HttpSessionSecurityContextRepository` + Redis sessions
- `@TenantAware` interceptor validates `X-Tenant-Id` header, checks tenant membership, then checks permissions
- Permission check uses native query joining `tenant_user_roles` → `tenant_role_permissions`
- Auth endpoints (`/api/v1/auth/**`) are `permitAll()` — skipped by tenant validation
- OpenAPI spec shows `X-Tenant-Id` as required via `OperationCustomizer`

## Configuration

- `application.yaml` (not `.properties`)
- Datasource from env vars: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` with defaults in `application.yaml`
- `.env.example` has Postgres defaults; copy to `.env` for local dev
- Redis: `SPRING_REDIS_HOST`, `SPRING_REDIS_PORT`, `SPRING_REDIS_PASSWORD`
- Mail: `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`
- API docs: `/scalar` (Scalar UI), `/v3/api-docs` (OpenAPI spec)
