# Spring Multi-Tenant Template

A Spring Boot 4 / JDK 25 starter for multi-tenant backends: a GraphQL API (Netflix DGS) on MySQL, built with hexagonal architecture (ports & adapters).

## Features

- **GraphQL API** (`POST /graphql`), schema-first, with Relay cursor pagination, batched DataLoaders, and query depth and complexity limits. GraphiQL is available in the `dev` profile at `/graphiql`.
- **Authentication**: email and password, with emailed six-character verification codes for registration and password reset. Redis-backed sessions (`SESSION` cookie).
- **Multi-tenancy**: the `X-Tenant-Id` header selects a tenant; `@TenantAware` operations check membership, tenant state and permissions. Default-deny: every operation requires a login unless marked `@PublicOperation`.
- **Tenant management**: tenants, roles and permissions, members, and email invitations (accept an existing account or register straight from the invite). Tenants, roles and members are inactivated rather than deleted.
- **Persistence**: MySQL 8.0 (Aurora MySQL v3 compatible), JPA, Flyway migrations, UUIDv7 IDs.
- **Observability**: OpenTelemetry (logs, traces, metrics) with a span and metrics per GraphQL operation, and Actuator.
- **Email**: SMTP. Kafka is on the classpath but not used yet.
- **Testing**: Testcontainers (MySQL, Kafka, Redis, Grafana).

## Getting started

```bash
docker compose up                              # optional local stack (reads .env)
./mvnw test                                    # requires Docker
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

With the `dev` profile, open <http://localhost:8080/graphiql> to explore the schema. Introspection and GraphiQL are disabled in every other profile.

## Using the API

Every request is `POST /graphql` with a JSON body. Log in once and send the `SESSION` cookie afterwards; operations that act inside a tenant also need the `X-Tenant-Id` header.

```graphql
mutation { login(input: { email: "me@example.com", password: "Password@123" }) { id email } }
{ myTenants { id name active } }                       # then pick a tenant id
{ tenantUsers(first: 20) { edges { node { user { name } roles { name } } } pageInfo { endCursor } } }
```

Failures are returned with HTTP 200 and an `errors` entry whose `extensions.code` is `UNAUTHORIZED`, `FORBIDDEN`, `BAD_REQUEST`, `NOT_FOUND` or `INTERNAL_ERROR`.

See [CLAUDE.md](CLAUDE.md) for architecture and conventions.
