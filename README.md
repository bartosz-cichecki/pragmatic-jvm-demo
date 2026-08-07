# Pragmatic JVM Engineering Demo

## Project overview

Pragmatic JVM Engineering Demo is a public engineering portfolio project for building a small multi-tenant application through deliberate, reviewable iterations. It revisits selected business cases from an earlier PHP/Symfony engineering demo and explores how they can be modelled and implemented idiomatically on the JVM. It is not a line-by-line port, and it is not presented as a production system.

The repository is intended to make architectural choices, domain reasoning, testing strategy, and engineering trade-offs visible as the project develops. AI assists the development process, while architectural decisions, verification, and review remain under human control.

The intended execution and delivery boundary is local development and demonstration plus verification in GitHub Actions. No VPS deployment, container registry, or image-promotion pipeline is planned.

## What this project is intended to demonstrate

The planned work will demonstrate:

- pragmatic Domain-Driven Design used to clarify business rules rather than as an end in itself;
- modular-monolith boundaries and explicit domain invariants;
- CQRS-lite, with separate write and read concerns where that separation adds value;
- multi-tenancy and authorization boundaries;
- automated testing and architecture verification;
- small iterations that are easy to understand and review;
- AI-assisted development governed by explicit repository rules and human review.

## Technology stack

The Kotlin/Spring Boot development baseline, production-like Docker runtime image, unified quality gate, GitHub Actions CI, first Client domain model, and ArchUnit architecture testing are implemented. Database, persistence, and integration-testing technologies remain planned for later iterations:

- Kotlin targeting Java 21;
- Spring Boot;
- Gradle with Kotlin DSL;
- Spotless with ktlint for Kotlin formatting;
- detekt for Kotlin static analysis;
- GitHub Actions for continuous integration;
- Docker and Docker Compose for the production-like local runtime image;
- PostgreSQL with Flyway migrations;
- JPA/Hibernate for writes;
- Spring `JdbcClient` for reads;
- JUnit 6 for application-context and domain unit tests, with Testcontainers planned for later integration tests;
- ArchUnit for automated architecture checks.

Technology choices will be added only when they support a concrete business or engineering need.

## Planned business scope

The application is planned around a multi-tenant model in which a **Client** defines a tenant space, a **User** represents an identity shared across tenants, and a **ClientMember** connects a user to a client. Tenant-specific roles and membership status will control access within each client.

Later iterations are intended to introduce passwordless sign-in using one-time passwords (OTP), together with authorization at both platform and tenant levels. These capabilities will be introduced incrementally; they are not implemented today.

## Engineering principles

- Business rules belong in the domain.
- The Application layer orchestrates use cases.
- Infrastructure implements technical details.
- HTTP is an inbound adapter.
- Write and read concerns are separated pragmatically.
- Framework conveniences must not blur module boundaries.
- Tests verify behaviour rather than implementation details.
- Architecture rules should be checked automatically where that provides useful feedback.
- KISS takes precedence over unnecessary abstractions.

## Project status

**Status: Client domain model complete**

The repository contains a minimal Kotlin/Spring Boot application, Gradle Wrapper build, application-context and domain unit tests, Kotlin formatting with Spotless and ktlint, detekt static analysis, ArchUnit architecture testing, GitHub Actions CI, an Actuator readiness endpoint, a production-like local runtime image, and the pure-Kotlin `Client` domain model. There are currently no business endpoints, database integration, event flows, or business infrastructure.

The documentation foundation, Spring Boot development baseline, production-like runtime image baseline, unified quality gate with CI, and Client domain model are complete. The next iteration will introduce Client persistence.

## Quality checks

Run every quality gate currently applicable to the project through either supported entry point:

```text
./gradlew qa
make qa
```

`make qa` delegates directly to the Gradle Wrapper task.

## Local development port

The application listens on port `8081` by default. Override it for a single run through the environment when that port is already in use:

```text
SERVER_PORT=9090 make dev
```

## Runtime image

Build the application JAR on the host and package it into the local runtime image, then start and verify that image:

```text
make image
make up
make smoke
make down
```

`make up` starts the existing image without rebuilding it. The application always listens on port `8081` inside the runtime container. `APP_PORT` controls only the port published on the host; it does not change the container's `SERVER_PORT`. Set the same `APP_PORT` value for `make up` and `make smoke` to use another host port.

The image uses an exec-form entrypoint. Supply JVM options through Java's standard `JAVA_TOOL_OPTIONS` environment variable when starting the container; no universal heap limit is built into the image.

## Implementation backlog

The current delivery target is the first complete `Client` creation flow. Detailed implementation order, current status, delivery scope, completion criteria, and verification commands are maintained in the [product backlog](docs/product-backlog.md).

The backlog is the source of truth for delivery planning. It does not replace the normative [architecture contract](docs/architecture.en.md).
