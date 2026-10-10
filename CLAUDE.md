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
- `port/inbound/`: use-case interfaces consumed by data fetchers (e.g. `TenantManagementPort`, `AuthPort`). There is no `port/outbound/`; repositories are injected directly.
- `adapter/inbound/`: application services implementing inbound ports (e.g. `TenantService`).
- `adapter/outbound/persistence/`: Spring Data repositories.
- `adapter/web/dto/`: the use cases' input and payload records, named `{Action}Input` / `{Action}Payload` to match the GraphQL types. They carry the Jakarta validation constraints and are also the parameters and results of the inbound ports (a deliberate simplification: there is no separate port-level command type).
- `adapter/web/graphql/`: Netflix DGS data fetchers (`@DgsComponent`) and DataLoaders. This is the whole API surface.
- `config/`: feature-specific Spring config (tenant context, access checker). Global GraphQL, security and error handling live in the top-level `config/` and `config/graphql/` packages.

Rules:
- Data fetchers depend on ports, never on service implementations.
- Ports take and return domain types or the `adapter/web/dto` records; they never expose GraphQL or servlet types (the HTTP request/response are passed only where login must set the session cookie).
- Services orchestrate (fetch, call domain methods, persist). Business rules live on the domain class.
- Anything that takes ids of another aggregate (roles, users, ...) must check they belong to the current tenant.

Non-obvious shared packages:
- `entity/`: only `@MappedSuperclass` bases (`BaseEntity`, `NoIdBaseEntity`), no concrete entities.
- `telemetry/`: OpenTelemetry and request filters, imported explicitly by `DemoApplication`.
- `utils/`: `HashUtils`, `TokenService`, UUIDv7 generator. `email/`: `EmailService`.

## GraphQL (Netflix DGS)

The API is GraphQL only: `POST /graphql`. There are no REST controllers; **do not add any**. Each feature has a schema file and a data fetcher.

Schema:
- Schema-first, in `src/main/resources/schema/*.graphqls`, one file per feature. `auth.graphqls` declares the root `Mutation` type; every other file uses `extend type Query` / `extend type Mutation`. Test-only schema lives in `src/test/resources/schema/`.
- Describe every type, field and operation with `"""descriptions"""`; this replaces the OpenAPI docs.
- Custom scalars: `UUID` and `DateTime` (`Instant`, UTC). Use `UUID` for identifiers.
- Mutations are named verb-first (`registerUser`, `updateTenant`), take a single `input: {Action}Input!` argument and return a `{Action}Payload`. Never return nothing: return the changed object, the new id, or `{ success }`.
- Nullability is deliberate: non-null unless the value can really be absent (for example `createdAt`, which is not populated today).
- Evolve additively and mark removals with `@deprecated(reason:)`. There is no versioned URL.
- Only expose fields that belong in the API. The schema is the whitelist (never declare `password`, `deleted_at`, ...).

Data fetchers:
- One `@DgsComponent` per feature in `adapter/web/graphql/`, depending only on inbound ports. One method per operation, annotated `@DgsQuery` / `@DgsMutation`.
- Read the input with `@InputArgument`, and call `GraphQlInputValidator.validate(input)` so the Jakarta constraints on the input records apply (violations become `BAD_REQUEST`).
- Get the caller and the HTTP request/response through `GraphQlRequestContext` (`currentUser(dfe)`, ...), never `SecurityContextHolder`; GraphQL execution may not stay on the request thread.
- Association fields (anything lazy on an entity) must be resolved by their own `@DgsData` method backed by a `@DgsDataLoader` using batched queries. `open-in-view` is off, so touching a lazy association throws. Loaders must filter by tenant.
- Lists use Relay cursor connections (`first` / `after`, forward only) ordered by id, never offset paging. Plain lists are only acceptable when naturally bounded (for example the caller's own tenants). Use `Cursors.pageSize`, `Cursors.decode` and `Cursors.toConnection` and return `Connection<T>`; services return a Spring Data `Window<T>`. For an entity with a single `id`, use `repository.findBy(spec, q -> q.sortBy(Sort.by("id")).limit(n).scroll(position))` (see `TenantRoleService`). Spring Data keyset scrolling does not work on paths inside a composite id, so for those add an explicit "after" predicate to the specification and fetch `limit + 1` rows (see `TenantUserService.scrollTenantUsers`).
- Batch every association field with a `@DgsDataLoader` implementing `MappedBatchLoader`, in `adapter/web/graphql/`, keyed by a record that includes `tenantId` where the data is tenant-scoped. The loader calls a port method that takes all keys at once. Implement that query as a JPQL constructor projection into a row record, so the entity's own eager `@ManyToOne` associations are not loaded one by one. Prove it with a test that compares Hibernate's prepared-statement count for a page of 1 against a larger page (see `TenantRoleGraphQlTest`).

Access control (in `config/graphql/`, enforced for every top-level Query/Mutation):
- **Default-deny.** An operation requires an authenticated user unless its method is annotated `@PublicOperation`. `@PublicOperation` cannot be combined with `@TenantAware`.
- `@TenantAware(withPermissions = ...)` works on data fetcher methods like it did on controllers: it needs a valid `X-Tenant-Id` header for a tenant the user belongs to, plus every listed permission. `TenantContext.getTenantId()` is only set while the top-level fetcher runs, so pass the tenant id explicitly to nested fetchers and DataLoaders.
- Access decisions live in one place, `TenantAccessChecker` (called by the GraphQL access instrumentation). In order: the user must be a member; the membership must be active; the tenant must be active; every permission must come from an active, non-deleted role. Inactive means `inactivated_at` is set: an inactive member is denied in that tenant, an inactive tenant rejects every tenant-aware operation, and an inactive role stays assigned but grants nothing. Only operations that reverse an inactive tenant (`reactivateTenant`) set `@TenantAware(allowInactiveTenant = true)`.
- Tenants and roles are never deleted through the API: expose `inactivateX` / `reactivateX` mutations (permission `x:inactivate`), with `active` and `inactivatedAt` on the type. Members can additionally be removed (`removeTenantUser`, permission `tenants:expel`): a soft delete that also drops their role assignments, after which they can rejoin only through a new invitation. A caller cannot inactivate or remove themselves.
- Failures return HTTP 200 with an `errors` entry whose `extensions.code` is `UNAUTHORIZED`, `FORBIDDEN`, `BAD_REQUEST`, `NOT_FOUND` or `INTERNAL_ERROR`. `GraphQlExceptionResolver` maps `AuthenticationException`, `AccessDeniedException`, `IllegalArgumentException`, `ConstraintViolationException` and `EntityNotFoundException`; anything else is logged and returned as a generic internal error. Throw exceptions; do not encode errors in payloads.
- Query depth is limited to 10 and complexity to 200 (`app.graphql.*`). Introspection and GraphiQL are enabled only in the `dev` profile.

## Invitations

- An invitation is addressed to an email and carries roles. The emailed link holds a random code; only its SHA-256 hash is stored (`code_hash`), and the API never returns the code. The email is sent after the transaction commits.
- Everything the invitee does is keyed by the code: `checkInvitedUser(code)` (public, side-effect free), `acceptTenantInvitation` (needs a session for the invited email, otherwise `FORBIDDEN`) and `registerUserFromInvitation` (public; creates the account for the invited email, joins, logs in). Unknown, expired, revoked and already-used codes are all `NOT_FOUND`, so a code reveals nothing.
- Admins (`tenants:invite`) use `tenantInvitations`, `inviteUserToTenant` and `revokeInvitation` (by id). Inviting an address again revokes its pending invitation. Status (`PENDING`, `ACCEPTED`, `REVOKED`, `EXPIRED`) is derived on `TenantInvitation`; the lifetime is `app.invitations.ttl`.
- Joining grants only the invited roles that still exist. A member who was removed (soft-deleted) is restored through `TenantUserRepository.restoreRemoved` because the soft-delete filter hides the row from JPA; an active or inactive member cannot accept (an inactive one is told to ask for reactivation).

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

- Sessions are Redis-backed Spring sessions; GraphQL uses the same `SESSION` cookie (set by the `login` / `registerUser` mutations).
- Annotate tenant-scoped controller methods or data fetcher methods with `@TenantAware`. The interceptor requires a valid `X-Tenant-Id`, verifies membership, sets `TenantContext`, and checks every permission in `withPermissions` (e.g. `@TenantAware(withPermissions = "tenants:update")`).
- At the HTTP level only `/graphql` (and `/graphiql/**` in the dev profile) is permitted anonymously; authentication is enforced per operation (default-deny, see GraphQL above).

## Testing

- `BaseGraphQlTest`: the base for every integration test. It runs the app on a random port with the `test` profile and the Testcontainers services, and logs users in through GraphQL. Use `graphql(...)`, `graphqlAs(...)`, `graphqlIn(tenantId, user, ...)` and `assertGraphQlError(response, "FORBIDDEN")`; pass inputs as variables. Failures are HTTP 200 with `errors`, so assert on `$.errors[0].extensions.classification`.
- The `test` profile swaps in `EmailServiceTestImpl` and `TokenServiceTestImpl`, so generated numeric codes are all zeroes.
- `BaseGraphQlTest` clears `auth_tokens` and the captured emails before each test, because all test tokens are identical. Do not rely on state left over from another test.
- The test profile records every email in `SentEmails`. Read what a user would receive with `sentEmails.lastTextTo(address)` (the six-character token, or the invitation link) instead of calling `tokenService.generate()`.
- `BaseGraphQlTest` has `newTenant`, `newRole`, `graphqlIn(tenantId, user, ...)` and `variables(...)` for the common setup.
- Create users through repositories/services. Use the tenant creation endpoint when the test is meant to exercise tenant initialization and role/permission setup.

## Frontend (`frontend/`)

React 19 + Vite + TypeScript SPA with shadcn/ui (Base UI flavour, preset `b2BnwC9Oi`), urql and React Router. It uses Node, not Maven.

```bash
cd frontend
npm run dev        # http://localhost:5173; proxies /graphql to :8080 (same origin, so the SESSION cookie needs no CORS)
npm run codegen    # regenerate src/gql from ../src/main/resources/schema/*.graphqls; commit the result
npm run typecheck  # tsc -b (not --noEmit: the root tsconfig only has references)
npm run lint && npm test && npm run build
```

- Write operations as `graphql(`...`)` documents next to the feature that uses them, and run `codegen` after changing a document or the backend schema.
- The session cookie is the source of truth: `SessionProvider` reads `me`, and tokens are never stored. `RequireAuth` / `PublicOnly` guard routes. There is no frontend authorization yet.
- Emailed codes are case-sensitive, six-character alphanumeric strings: send exactly what the user typed.
- shadcn skills live in `frontend/.agents/skills` (installed with `npx skills add shadcn/ui`). Add components with `npx shadcn@latest add <name>` and use blocks (`login-01`, `sidebar-07`) as templates.

## Adding things

New use case: inbound port, then service in `adapter/inbound/`, then the schema in `schema/<feature>.graphqls` and a data fetcher method (with `@TenantAware` if tenant-scoped, or `@PublicOperation` if anonymous), then input records in `adapter/web/dto/`, then a `BaseGraphQlTest` test covering success, authentication, tenant/permission and validation failures.

New table: new migration (see Database), entity in `domain/` extending `BaseEntity`, repository in `adapter/outbound/persistence/`, then run a test so `ddl-auto: validate` checks the mapping.
