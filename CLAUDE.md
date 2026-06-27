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

## Commits

Use conventional commit style on the current branch (e.g. `feat:`, `fix:`, `refactor:`, `test:`, `chore:`).

Before running `git commit`, always show the proposed commit message and wait for approval.

## Domain Vocabulary

Use these exact terms everywhere (code, logs, comments, API responses). See `CONTEXT.md` for the full reference.

| Term | Meaning | Avoid |
|---|---|---|
| **Job** | Unit of work — a Racket brought in to be strung | Order, request, ticket |
| **Stage** | Lifecycle state of a Job: Queued → In Progress → Ready → Done → Paid | Status, phase, step |
| **Stringer** | The operator of the app | User, admin |
| **Customer** | Person who brings Rackets in | Client, player |
| **Racket** | Tennis racket owned by a Customer | Equipment, item |
| **Service Fee** | Labor charge per Job | Labor cost, stringing fee |
| **String Fee** | Material cost for the string product | Material cost, string cost |
| **String** | A string product in inventory | Product, cord |

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

**Use case interface pattern:** Each use case is its own interface in `ports/in/<aggregate>/`. The service class for that aggregate implements all of them. Controllers inject each use case interface individually (not the service class). Input is passed as `XxxCommand` (mutations) or `XxxQuery` (reads) value objects defined alongside the interface.

**Soft-delete pattern:** Aggregates carry a `deletedAt: Instant?` field. Repository adapters filter on `deletedAtIsNull` so soft-deleted records are invisible to all queries. Hard-deletes are not used.

**Error handling:** `GlobalExceptionHandler` (`infrastructure/web/`) maps domain exceptions to RFC 7807 `ProblemDetail` responses. Add a handler there for each new domain exception type. Use `@Valid` + `jakarta.validation` constraints on request bodies; validation failures are also caught by the handler.

## Profiles

- **No profile (default)** = production mode: JSON/Datadog structured logging.
- **`local` profile** = development mode: plain text logging, Docker Compose auto-started.

Activate locally with `SPRING_PROFILES_ACTIVE=local` or `-Dspring.profiles.active=local`.

## Security

OAuth2 Resource Server backed by Keycloak. Spring Security is configured to validate JWTs. Keycloak issuer is hardcoded to `https://auth.lausi95.net/realms/stringpro` for all environments. All endpoints require a valid bearer token. No role-based gating — authentication alone is sufficient.

Swagger UI paths (`/swagger-ui/**`, `/v3/api-docs/**`) are always public. Swagger UI includes a bearer-JWT auth scheme so you can paste Keycloak tokens and test secured endpoints directly.

Relevant config: `infrastructure/config/SecurityConfig.kt`, `infrastructure/config/OpenApiConfig.kt`

## Local Development (Docker Compose)

A `docker-compose.yml` at the project root defines a MongoDB container. The `local` Spring profile enables Spring Boot Docker Compose support, which auto-starts the container when running `./gradlew bootRun`.

MongoDB URI for prod is read from `MONGODB_URI` env var (default: `mongodb://localhost:27017/stringpro`).

## API Documentation

OpenAPI docs are generated at runtime from annotations. Swagger UI is always available at `/swagger-ui.html` (including production). Do not maintain a static OpenAPI spec file.

Annotate controllers with `@Operation`, `@ApiResponse`, and `@Tag`.

## Structured Logging (Datadog)

Production logs (no active profile) are structured JSON for Datadog ingestion via `logstash-logback-encoder`. The `local` profile switches to plain text. Config: `src/main/resources/logback-spring.xml`.

Always log with SLF4J (`private val log = LoggerFactory.getLogger(javaClass)`). Add contextual fields via MDC (e.g. `userId`, `aggregateId`) at the controller layer so they propagate through the call stack.

## Testing Strategy

- **Unit tests**: Pure domain logic and use case services — mock all `ports/out` with MockK. Favour one test class per service/domain class. Tests must not start a Spring context.
- **Integration tests**: One slice per adapter (persistence, web). Use `@DataMongoTest` for repository adapters with an embedded MongoDB (Flapdoodle). Use `@WebMvcTest` + `@MockkBean` for controller slices with a mocked use case. Full `@SpringBootTest` only when testing cross-cutting concerns (security config).
- Follow red-green-refactor: write a failing test, make it pass minimally, then refactor.
- Test naming convention: `should<ExpectedBehavior>When<Condition>` in backtick strings.

**Spring Boot 4.x note**: `@WebMvcTest` moved to `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` (requires `spring-boot-starter-webmvc-test` on the test classpath). Every controller slice test requires this boilerplate to wire security and error handling correctly:
```kotlin
@WebMvcTest(XxxController::class)
@Import(SecurityConfig::class, GlobalExceptionHandler::class)
class XxxControllerTest {
    @MockkBean private lateinit var jwtDecoder: JwtDecoder
    // mock each use case interface individually
}
```
Use `with(jwt())` from `SecurityMockMvcRequestPostProcessors` on every request that should be authenticated.

## Key Dependencies (Gradle)

| Purpose | Artifact |
|---|---|
| Web | `spring-boot-starter-web` |
| Security / JWT | `spring-boot-starter-oauth2-resource-server` |
| MongoDB | `spring-boot-starter-data-mongodb` |
| Validation | `spring-boot-starter-validation` |
| OpenAPI docs | `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.0` |
| Structured logging | `net.logstash.logback:logstash-logback-encoder:8.1` |
| Docker Compose (local) | `spring-boot-docker-compose` (`developmentOnly`) |
| Test mocking | `io.mockk:mockk:1.14.2`, `com.ninja-squad:springmockk:4.0.2` |
| Web MVC test slice | `spring-boot-starter-webmvc-test` |
| MongoDB test slice | `spring-boot-data-mongodb-test` |
| Security test support | `org.springframework.security:spring-security-test` |
| Embedded MongoDB | `de.flapdoodle.embed:de.flapdoodle.embed.mongo.spring4x:4.24.0` |
| Lint | `ktlint-gradle`, `detekt` |
