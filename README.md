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

## Planned technology stack

The following stack is planned; none of it has been introduced in the repository yet:

- Kotlin targeting Java 21;
- Spring Boot;
- Gradle with Kotlin DSL;
- PostgreSQL with Flyway migrations;
- JPA/Hibernate for writes;
- Spring `JdbcClient` for reads;
- JUnit 5 and Testcontainers for automated testing;
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

**Status: foundation / work in progress**

Application implementation has not started. There are currently no application endpoints, domain aggregates, automated tests, quality gates, database integration, event flows, or infrastructure in this repository.

The documentation foundation is complete. The next implementation iteration will establish the Spring Boot development baseline.

## Implementation backlog

The current delivery target is the first complete `Client` creation flow. Detailed implementation order, current status, delivery scope, completion criteria, and verification commands are maintained in the [product backlog](docs/product-backlog.md).

The backlog is the source of truth for delivery planning. It does not replace the normative [architecture contract](docs/architecture.en.md).
