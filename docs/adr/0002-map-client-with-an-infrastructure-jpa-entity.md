# ADR 0002: Map Client with an Infrastructure JPA entity

**Status:** Accepted

**Date:** 2026-08-07

## Context

The `Client` aggregate was introduced as a persistence-independent Kotlin model. Its constructor and state are private, its identifier and name are value classes, and behaviour obtains time through a Domain-owned `ClientTimeProvider`. Persistence must not add a public read API, setters, mutable collections, framework callbacks, or invalid construction paths to that aggregate.

JPA has different object-model constraints. An entity needs persistence-visible state and a no-argument construction mechanism, Hibernate commonly expects proxyable classes, and a framework cannot hydrate the aggregate's `ClientTimeProvider` as ordinary database state. Kotlin classes and properties are final by default, while Kotlin value classes are not a useful direct JPA entity-field boundary without additional converters and mapping details.

## Decision

Map a separate `ClientJpaEntity` in `Client` Infrastructure and keep all JPA and Spring Data types there. The entity contains only the database representation of the aggregate and is mapped to `client.clients`. The Kotlin JPA compiler plugin supplies the persistence constructor for this infrastructure type; it does not open or annotate the Domain aggregate.

The aggregate exposes one Kotlin-`internal`, immutable `PersistenceState` memento and an `internal` restore function. These are available only inside this Gradle module, transfer the complete state atomically, and do not add public getters, setters, or mutable state. Restoration receives the Domain-owned time port from the adapter, whose production implementation delegates to an injected UTC `Clock`. Domain value objects are reconstructed through their validating APIs.

The Domain-owned `ClientRepository` deals only in `Client` and `ClientId`. Its Infrastructure adapter uses a transaction-scoped `EntityManager` directly. The adapter calls neither `flush` nor transaction APIs, and a save attempted without an outer transaction fails instead of creating and committing a repository-owned transaction. Those responsibilities remain with the future top-level command flow. This baseline's integration test owns an explicit test transaction and `flush` only to verify mapping.

Flyway runs automatically during application startup and tests (`spring.flyway.enabled=true`). Its conventional `classpath:db/migration` root is scanned recursively, while context subdirectories record migration ownership and all migrations share one ordered history. Hibernate uses `ddl-auto=validate`, so Flyway alone creates the schema and Hibernate checks the mapping. The first globally unique migration version is `V1` and its file is under `db/migration/client` to record `Client` ownership. PostgreSQL `TIMESTAMP WITH TIME ZONE` columns and Hibernate's UTC JDBC setting preserve `Instant` values without local-time ambiguity.

## Alternatives considered

### Annotate and map the aggregate directly

Direct mapping would remove the explicit mapper, but it would force persistence construction, proxying, value-class conversion, and reinjection of the non-persistent time port into the Domain type. Entity listeners or callbacks would also make Domain hydration depend on framework lifecycle rules. Even though the architecture permits narrowly scoped mapping annotations in Domain, this option would impose broader JPA constraints on the aggregate and make its encapsulation harder to reason about.

### Use an annotation-free Hibernate mapping for the aggregate

XML or programmatic Hibernate metadata would hide annotations, but it would not solve the private-constructor, final-class, value-class, and time-port hydration constraints. It would replace visible mapping code with less discoverable configuration while still coupling aggregate internals to Hibernate's instantiation behaviour.

### Read and write private aggregate fields through reflection

Reflection would keep the source-level API unchanged but make persistence depend on private field names and bypass value-object construction. That failure mode is fragile, difficult to test statically, and inconsistent with an explicit modular-monolith boundary.

### Delegate writes to a Spring Data CRUD repository

Spring Data would reduce the adapter to a small delegation, but its inherited `save` implementation is transactional and creates a transaction when no outer one exists. Although it joins a correctly configured outer transaction, that fallback would let the repository commit independently and weaken the architecture's single-boundary rule. Direct `EntityManager` use keeps JPA inside Infrastructure without adding repository-owned transaction semantics.

## Health and runtime decision

The implemented demo cannot serve its write model without PostgreSQL, so readiness explicitly contains `readinessState` and the datasource `db` contributor. Liveness contains only `livenessState`: losing PostgreSQL makes the instance unable to serve work, but it does not mean that the JVM is dead or that restarting it will repair the external database. A focused integration test stops PostgreSQL after startup and verifies this difference.

Local `bootRun` and tests deliberately use different service lifecycles. The development-only Spring Boot Docker Compose module discovers root `compose.yaml` for `bootRun`; integration tests declare PostgreSQL Testcontainers with `@ServiceConnection` and communicate with the host Docker daemon. The packaged runtime contains neither mechanism and receives standard Spring datasource settings through environment variables.

## Consequences

- Domain remains free of Spring, JPA, Hibernate, and Spring Data dependencies.
- Aggregate persistence has an explicit mapping step and a small amount of duplicated field structure in Infrastructure.
- Adding an aggregate field requires updating the immutable persistence state, entity, mapping, and migration as applicable; the lifecycle integration test protects that path.
- The module-internal memento is a deliberate persistence seam, not a public query API. Read use cases still require query DTOs and SQL in a later scoped iteration.
- PostgreSQL stores timestamps with microsecond resolution while `Instant` can carry nanoseconds. The current Domain contract requires an unambiguous UTC instant but not nanosecond-identical round trips; time will be normalized explicitly if exact persistence equality becomes a business or testing contract.
- Application startup fails if PostgreSQL is unavailable or Flyway cannot apply the pending migrations. A database outage after startup reports readiness `DOWN` while liveness remains independent.
