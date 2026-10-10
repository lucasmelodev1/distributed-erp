# Spring Multi-Tenant Template

A Spring Boot 4 / JDK 25 starter for multi-tenant APIs, built with hexagonal architecture (ports & adapters).

## Features

- **Passwordless/email auth**: six-digit codes (hashed at rest), Redis-backed sessions
- **Multi-tenancy**: `X-Tenant-Id` header, `@TenantAware` controllers, membership and permission checks
- **Tenant management**: tenants, custom roles and permissions, user invitations
- **Users**: user API with active/inactive status (soft delete internally)
- **Persistence**: MySQL 8.0 (Aurora MySQL v3 compatible), JPA, Flyway migrations, UUIDv7 IDs
- **Observability**: OpenTelemetry (logs, traces, metrics), Actuator
- **Messaging & email**: Kafka, SMTP
- **API docs**: OpenAPI with Scalar UI
- **Testing**: Testcontainers (MySQL, Kafka, Redis, Grafana), GreenMail

## Getting started

```bash
docker compose up   # optional local stack (reads .env)
./mvnw test         # requires Docker
./mvnw spring-boot:run
```

See [CLAUDE.md](CLAUDE.md) for architecture and conventions.
