# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**stringpro-backend** is a Kotlin/Spring Boot REST API using Clean Architecture, MongoDB for persistence, and Keycloak as the OAuth2 authorization server (resource server only — valid access token required, no role gating).

## Build & Run

```bash
# Build
./gradlew build

# Run (dev)
./gradlew bootRun

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.stringpro.application.domain.service.SomeServiceTest"

# Run a single test method
./gradlew test --tests "com.stringpro.application.domain.service.SomeServiceTest.shouldDoSomething"

# Lint / static analysis
./gradlew ktlintCheck
./gradlew detekt
```

## Architecture

This project follows Clean Architecture. Dependencies point inward: Infrastructure → Application → Domain.

```
src/main/kotlin/com/stringpro/
├── infrastructure/          # Spring config, controllers, DB adapters, security
│   ├── config/              # Spring beans (Security, MongoDB, OpenAPI)
│   ├── web/                 # REST controllers (thin — delegate to use cases)
│   ├── persistence/         # MongoDB repository adapters (implement out-ports)
│   └── security/            # Keycloak JWT filter/config
└── application/
    ├── domain/
    │   ├── model/           # DDD aggregates, entities, value objects (no framework deps)
    │   └── service/         # Use case implementations (depend only on ports and domain)
    └── ports/
        ├── in/              # Use case interfaces (driven ports)
        └── out/             # Outgoing ports — persistence, external integrations
```

**Key rules:**
- `domain/model` has zero framework or infrastructure dependencies.
- `domain/service` classes implement the `ports/in` interfaces and depend only on `ports/out` interfaces — never on infrastructure directly.
- Controllers resolve use cases via the `ports/in` interfaces, never call service classes directly.
- Each aggregate gets its own sub-package at every layer (e.g. `model/job/`, `service/job/`, `ports/in/job/`).

## Security

OAuth2 Resource Server backed by Keycloak. Spring Security is configured to validate JWTs using the Keycloak issuer URI. All endpoints require a valid bearer token. No role-based gating — authentication alone is sufficient.

Relevant config: `infrastructure/config/SecurityConfig.kt`

```yaml
# application.yml (key properties)
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI}
```

## API Documentation

OpenAPI docs are generated at runtime from annotations. Swagger UI is available at `/swagger-ui.html` in non-production profiles. Dependencies: `springdoc-openapi-starter-webmvc-ui`.

Annotate controllers with `@Operation`, `@ApiResponse`, and `@Tag`. Do not maintain a static OpenAPI spec file.

## Structured Logging (Datadog)

Production logs are structured JSON for Datadog ingestion. Use `logstash-logback-encoder` with a `logback-spring.xml` that activates JSON output on the `prod` profile and plain text otherwise.

Always log with SLF4J (`private val log = LoggerFactory.getLogger(javaClass)`). Add contextual fields via MDC (e.g. `userId`, `aggregateId`) at the controller layer so they propagate through the call stack.

## Testing Strategy

- **Unit tests**: Pure domain logic and use case services — mock all `ports/out` with MockK. Favour one test class per service/domain class. Tests must not start a Spring context.
- **Integration tests**: One slice per adapter (persistence, web). Use `@DataMongoTest` for repository adapters with an embedded MongoDB (Flapdoodle). Use `@WebMvcTest` + `@MockkBean` for controller slices with a mocked use case. Full `@SpringBootTest` only when testing cross-cutting concerns (security config).
- Follow red-green-refactor: write a failing test, make it pass minimally, then refactor.
- Test naming convention: `should<ExpectedBehavior>When<Condition>` in backtick strings.

## Key Dependencies (Gradle)

| Purpose | Artifact |
|---|---|
| Web | `spring-boot-starter-web` |
| Security / JWT | `spring-boot-starter-oauth2-resource-server` |
| MongoDB | `spring-boot-starter-data-mongodb` |
| OpenAPI docs | `springdoc-openapi-starter-webmvc-ui` |
| Structured logging | `logstash-logback-encoder` |
| Test mocking | `mockk`, `springmockk` |
| Embedded MongoDB | `de.flapdoodle.embed.mongo` |
| Lint | `ktlint-gradle`, `detekt` |
