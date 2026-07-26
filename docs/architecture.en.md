# Architecture

[Polski](architecture.pl.md) | [English](architecture.en.md)

This document is the repository's normative architecture contract. It defines the rules that every system element must follow if that element is added. It is not an implementation status report, backlog, or sequence of work. The absence of a mechanism from the repository does not weaken the rules that become applicable when corresponding code is introduced.

## 1. Purpose and priorities

- KISS takes precedence over an abstraction that does not solve a concrete problem.
- Production code is written in Kotlin and runs on Java 21.
- The system is a modular monolith with explicit bounded-context and layer boundaries.
- Dependency direction is part of the architecture and must be enforced automatically wherever ArchUnit can verify it.
- CQRS-lite separates writes and reads technically and semantically without separate deployments or databases.
- Commands, queries, and events form stable internal contracts. They are not versioned without a compatibility problem that requires versioning.
- Every write use case has one obvious transaction-management and `flush` point.
- Convention takes precedence over manual configuration when it does not obscure ownership or implementation choice.
- Domain code must be testable with deterministic time and without runtime infrastructure.
- Microservices, a message broker, Redis, and similar infrastructure require a concrete business or operational justification. They are not the default means of module separation.

## 2. Bounded contexts and ownership

Every business capability, domain concept, table, migration, and contract has one owner.

- `Client` owns the tenant space, client membership, tenant-specific roles, and access rules within a client.
- `User` owns user identity shared across tenants and authentication, including OTP mechanisms.
- `SharedKernel` contains only genuinely shared, stable mechanisms and contracts, such as command and event handling, domain-event recording, time, the event log, integration-event contracts, the outbox, and asynchronous-processing support.

`SharedKernel` is not a home for business concepts merely because they occur in more than one module. Sharing requires one stable meaning. `SharedKernel` must not depend on any business context.

A context owns its domain model, database schema, SQL, migrations, and public contracts exposed to other contexts. No context directly reads or modifies another context's domain model or tables.

## 3. Packages and directory structure

The application base package is:

```text
com.bartoszcichecki.pragmaticjvmdemo
```

Context code is organised by layer:

```text
src/main/kotlin/com/bartoszcichecki/pragmaticjvmdemo/
├── sharedkernel/
├── client/
│   ├── domain/
│   ├── application/
│   ├── infrastructure/
│   └── ui/
└── user/
    ├── domain/
    ├── application/
    ├── infrastructure/
    └── ui/
```

Within a context, use the following template and expand it only as required by actual code:

```text
{boundedcontext}/
├── domain/
│   └── {aggregate}/
│       ├── event/
│       ├── factory/
│       ├── outside/
│       └── repository/
├── application/
│   └── {aggregate}/
│       ├── command/
│       │   └── {action}/
│       └── query/
│           └── dto/
├── infrastructure/
│   ├── {aggregate}/
│   │   ├── persistence/
│   │   ├── query/
│   │   └── outside/
│   └── configuration/
└── ui/
    ├── http/
    │   └── api/
    ├── input/
    └── console/
```

Kotlin package names are lowercase, and type names follow Kotlin conventions. Create an aggregate package only for an existing aggregate. Empty packages, placeholder types, and abstractions “for later” are not allowed.

Flyway migrations are grouped by owner, for example:

```text
src/main/resources/db/migration/{boundedcontext}/
src/main/resources/db/migration/sharedkernel/
```

## 4. Layers and dependency direction

### 4.1 Domain

- Contains aggregates, entities, value objects, factories, policies and domain services, domain events, and repository and Outside ports.
- Does not depend on `application`, `infrastructure`, or `ui`.
- May depend on the Kotlin/JDK baseline, its own context, and suitable `SharedKernel` domain contracts.
- Minimal annotations required solely for persistence mapping are allowed, but they must not affect the domain's public API or behaviour.
- Does not depend on Spring application services, Spring MVC, Spring `JdbcClient`, repository implementations, or integration adapters.

### 4.2 Application

- Orchestrates use cases through the Domain model and ports owned by the current context.
- Depends on Domain and contracts owned by its own Application layer.
- Does not depend on Infrastructure implementations or UI.
- Does not know or inject Outside ports. Business decisions that need external read-only state remain in Domain.
- Does not execute SQL or manipulate JPA entities from another context.

### 4.3 Infrastructure

- Implements repositories, query services, Outside, message publication, and other technical ports.
- Contains JPA/Hibernate, Spring `JdbcClient`, SQL, external integrations, event transport, and module-owned Spring configuration.
- May depend on Domain and Application contracts.
- Contains no business rules.

### 4.4 UI

- Contains inbound adapters, including HTTP controllers and console commands.
- Validates the format and completeness of transport data, maps input to an application command or query, and maps results to transport responses.
- Contains no business rules, does not open transactions, and does not access JPA, `JdbcClient`, or SQL directly.

### 4.5 Reading data from another context (ACL)

- Context A does not issue SQL against tables owned by context B.
- If A needs data owned by B, an adapter in A's Infrastructure layer invokes a public query contract exposed by B.
- A defines its own result or DTO for its need and maps B's response in A's Infrastructure adapter. B's types are not re-exported into A's Domain or Application layers.
- The adapter is the anti-corruption layer. In a modular monolith, the call remains synchronous and in process; it requires neither networking nor serialisation.

### 4.6 A write initiated across contexts

- If a use case in A must initiate a change owned by B, A's Application layer depends on a port defined by A.
- The port implementation in A's Infrastructure layer may dispatch B's public command through the internal command bus and, if necessary, obtain a result through B's public query contract.
- A's Domain and Application layers do not import B's Domain or Application types. The foreign contract and its mapping remain in A's Infrastructure adapter.
- A context does not modify another context's aggregate or invoke its repository directly.

## 5. Domain model

### 5.1 Aggregates, entities, and value objects

- An aggregate defines a consistency boundary and enforces every invariant that must be preserved atomically.
- Aggregate state may be changed only through explicit behaviour on the aggregate root. Public setters and application mapping that bypasses behaviour are not allowed.
- A constructor or factory creates only a valid aggregate. Every operation rejects a change that violates an invariant; temporarily invalid state must not be persisted.
- An entity has a stable identity and a lifecycle controlled by the aggregate root. An internal entity does not have its own repository unless it is itself the root of a separate aggregate.
- A value object is immutable, compares by value, and validates its own constraints. A primitive does not replace a value object when the concept has domain semantics or rules.
- A rule involving multiple pieces of aggregate state belongs in the aggregate. A domain rule that does not naturally belong to one entity belongs in a factory, policy, or domain service, not in a handler.

### 5.2 Aggregate encapsulation

- Aggregates expose behaviour, not internal state.
- Public properties or getters that reveal state are not added for orchestration, API responses, or tests.
- JPA hydration may use private or persistence-only access, but must not create a public read API for the aggregate.
- Domain events are the contract for facts resulting from a state change and contain the data consumers require.
- UI and API reads use query services and DTOs, not aggregates.

### 5.3 Outside

Outside is the domain's controlled, read-oriented window onto state it does not own.

- An Outside port belongs to Domain and may be used by aggregates, factories, policies, and domain services.
- Application does not know Outside and does not pass technical data merely to make decisions on the domain's behalf.
- Outside exposes business-relevant questions such as current time, permission state, hashes, counters, limits, or facts from other contexts.
- Outside causes no external business side effects. It may record a domain event in an in-memory collector, but it must not change another aggregate or an external system.
- Cross-context access through Outside is read-only and passes through an ACL in Infrastructure.
- Infrastructure implements Outside and delegates to `SharedKernel` mechanisms or the data owner's public query services.

### 5.4 Policy as a domain service

- A policy is a domain service; its name gives it no additional architectural privileges.
- A policy should be pure when the caller already has all required domain data.
- When a decision needs external read-only state, a policy may depend on Outside or a narrow domain read port. Do not move the decision into Application merely to keep the policy superficially pure.
- Application passes business input. The policy obtains the state it needs and enforces the rule.

```text
handler: policy.assertCanAddItems(aggregateId, requestedCount)
policy:  currentCount = outside.countItems(aggregateId)
         require(currentCount + requestedCount <= limit)
```

### 5.5 Time in the domain

- Domain timestamps such as `createdAt`, `updatedAt`, `statusChangedAt`, and `occurredAt` use `java.time.Instant`.
- The domain obtains the current `Instant` through Outside. Application and UI do not pass a technical `now` or creation time solely for testability.
- A production Outside implementation obtains time from an injected `java.time.Clock`. An aggregate does not depend on `Clock` directly.
- Domain code does not call `Instant.now()` or construct a system clock.
- Tests control time through a fake Outside or a fixed or mutable `Clock` used by the Outside implementation.
- The backend and database store unambiguous UTC instants. JPA, JDBC, and Flyway mappings must preserve `Instant` without timezone ambiguity.
- The event log, outbox, leases, and workers use an injected `Clock`; PostgreSQL is not the source of application time.
- Conversion to a user's local timezone belongs to UI. Domain and read models do not change the meaning of a stored instant.
- A date range supplied as business input is a value object validated in Domain. UI converts a local range into unambiguous instants before passing it to the backend.

## 6. CQRS-lite

CQRS-lite means separating write and read responsibilities within one system and one database. It does not mean event sourcing or separate query infrastructure.

### 6.1 Commands and handlers

- Every use case that changes domain state is represented by a command.
- A command is an immutable Kotlin value and contains no business logic.
- A handler handles one use case and orchestrates factories, repositories, policies, and aggregates.
- Business validation remains in Domain. A handler does not reproduce invariants or bypass aggregate behaviour to modify state.
- The aggregate records its domain events through Outside. A handler does not construct or record them on the aggregate's behalf.
- Handlers return `Unit` by default.
- If a use case requires a minimal business result, a command may have a typed result. The result must not be an aggregate, JPA entity, or read model, and is exposed to the caller only after the transaction commits successfully.
- Commands are dispatched through a central, typed dispatcher. UI, subscribers, and cross-context adapters do not invoke handlers directly.

### 6.2 Repositories and the write transaction

- A repository port belongs to Domain and uses domain language and aggregate roots.
- A repository exists for an aggregate root, not for every entity. It is not used to build read responses.
- A repository adapter belongs to Infrastructure and uses JPA/Hibernate to load and persist aggregates.
- A repository attaches or persists changes but does not call `flush()`, commit, or open transactions.
- Every top-level command has one Spring transaction boundary around the central dispatcher or its decorator. A controller and handler do not define competing boundaries.
- JPA changes, the event log, synchronous domain-event handling, and outbox writes complete in the same transaction. Failure of any part rolls back the whole operation.
- The central flow coordinates draining the event collector and one explicit `flush` point. Events from a rolled-back transaction must not be treated as published.
- A technical exception to the single boundary requires an ADR and must remain outside aggregate business rules.

### 6.3 Queries and read models

- A read-only use case is exposed through a query service owned by the data owner.
- A query-service implementation uses SQL through Spring `JdbcClient`, not JPA/Hibernate entity loading.
- A query returns an immutable, use-case-specific DTO or read model, never an aggregate or JPA entity.
- A query causes no side effects and does not enter the domain write path.
- SQL, mapping, and DTOs belong to the context that owns the data.
- An independent read model is required when the read shape or performance needs do not match the write model. Do not add one without a use case that needs it.

### 6.4 Technical SQL operations

Direct SQL writes outside JPA are allowed for `SharedKernel` technical mechanisms such as the event log, atomic outbox claims, and idempotency records. This is an explicit exception, not the normal write path in a business context.

## 7. Events and side effects

### 7.1 Domain events

Every aggregate domain event contains:

- the aggregate identifier as the context's value object;
- `occurredAt` as an `Instant`;
- event-specific data required by its consumers.

Field names are consistent within a context. Internal domain events are not versioned.

A domain event is a synchronous, in-process contract. The domain records it through Outside, and the central command flow stores it in the event log and dispatches it through a synchronous event bus inside the command transaction. It is not an asynchronous queue item or a durable delivery contract between processes.

### 7.2 Integration events

- An integration event is a separate contract from a domain event.
- It supports asynchronous technical communication between modules or processes when the consumer should not participate in the command transaction.
- Its payload contains primitives and simple JSON structures. It does not contain aggregates, JPA entities, or framework transport types.
- Publication writes an outbox record in the current database transaction and does not dispatch the event in memory.
- A durable record contains a technical event identifier, a stable event name, a JSON payload, and `createdAt` from an injected `Clock`.
- A synchronous Application handler may translate a domain event into an integration event. If asynchronous publication is the purpose, it uses the integration-event publisher instead of dispatching an additional command solely to reach the outbox.
- Synchronous handlers and asynchronous subscribers are Spring beans discovered through explicit typed contracts and constructor injection.

### 7.3 Subscribers and side effects

- A synchronous subscriber does not modify JPA entities or repositories directly.
- If a synchronous reaction requires a domain change, the subscriber dispatches a dedicated command. That command participates in the top-level transaction.
- A change owned by another context goes through its public contract according to the ACL rules.
- A non-transactional external side effect that is to run asynchronously is initiated by an integration event persisted in the outbox.
- A side-effect handler must be business-idempotent if redelivery can repeat the external operation.

## 8. Transactional outbox and asynchronous processing

When an integration event must be published asynchronously, it must be persisted through the transactional outbox within the same transaction as the domain change.

- `shared.async_outbox` is the durable queue and state store for integration events.
- A Spring Boot worker polls PostgreSQL. The default model requires no message broker, Redis, or `LISTEN/NOTIFY`.
- The worker claims a batch with one atomic `UPDATE ... FROM (SELECT ... FOR UPDATE SKIP LOCKED) ... RETURNING` and stores an ownership token.
- A record is eligible when it is unprocessed, below the attempt limit, and either unclaimed or protected only by an expired lease.
- The default batch size is 50, the attempt limit is five, the lease is five minutes, and the delay between empty polls is five seconds. The values are configurable.
- Claiming increments the attempt counter. A failure stores a bounded description, releases the claim, and leaves the record available until the attempt limit is reached.
- After five failed attempts, a record is not claimed automatically. A separate dead-letter queue is not part of this model.
- The payload is deserialised by stable event name and delivered only to subscribers compatible with its type.
- `shared.async_consumption` stores claim and idempotency state for an event, subscriber, and handler combination.
- Before invoking a handler, the worker atomically creates or takes over a `processing` record. A `processed` record is skipped, while another worker's valid lease defers processing until later.
- On success, the worker records `processed` only while ownership is retained. On failure, it releases its own consumption claim and the outbox record for retry.
- The outbox record is marked processed only after every matching subscriber succeeds and while record ownership is retained.
- A command dispatched by an asynchronous subscriber runs in its own worker transaction.
- Technical idempotency prevents another run of a handler marked `processed`, but does not replace business idempotency for an external side effect. A process can stop after the side effect but before recording success.

## 9. Persistence and migrations

- JPA/Hibernate handles aggregate writes; Spring `JdbcClient` handles read models.
- Spring Data may be used inside a repository adapter, but its interfaces and types do not replace the domain repository port and do not leave Infrastructure.
- JPA mapping details must not weaken aggregate encapsulation, move business rules into persistence entities, or reverse dependency direction.
- Technical database identifiers do not replace domain identifier types at the Domain boundary.
- A Flyway migration belongs to the context that owns the schema objects it changes. Migrations for shared mechanisms belong to `SharedKernel`.
- Migration versions are unique across the application's combined Flyway history.
- Application startup or an explicit Gradle task applies one ordered set of pending migrations from every registered location. A context directory denotes ownership, not separate execution.

## 10. Dependency injection and configuration

- Spring constructor injection is the standard. Field injection is not allowed.
- Component scanning starts at `com.bartoszcichecki.pragmaticjvmdemo` and respects package boundaries.
- Domain types carry no Spring component annotations. Infrastructure configuration may explicitly expose a domain factory, policy, or service as a bean while retaining constructor injection and keeping the domain type independent of Spring.
- Spring configuration, persistence mapping, adapters, and port bindings belong to the relevant context's Infrastructure layer.
- Explicit bean configuration is required when convention cannot express an implementation choice, qualified collection, decorator, environment value, or library configuration.
- Global configuration contains only genuinely shared elements. Module details remain in the module.
- A production bean is not made more visible or mutable solely for tests. Tests use constructor composition, fakes, or Spring test configuration.

## 11. HTTP and platform routes

- HTTP is an inbound adapter. The API uses JSON and the `/api` path prefix.
- A public endpoint contract consists of its path, HTTP method, input, response status, and JSON payload. Changing any element requires updated behaviour tests and an explicit compatibility review.
- A route name is an internal routing and security contract, not a public HTTP contract.
- The `platform_` name prefix is reserved for platform-only endpoints.
- A platform endpoint does not require active-client selection, but general request and origin checks run before tenant handling is bypassed.
- A platform endpoint requires an authenticated platform administrator; another authenticated user receives HTTP 403.
- Administrator status is established during authentication from an explicitly configured allowlist and is never accepted from request input.
- A test based on Spring routing metadata verifies that a name containing `platform` starts with `platform_`.
- Adding or changing a route requires review of platform naming, role mapping, tenant-bypass rules, and route-dependent handlers.

## 12. ArchUnit rules

ArchUnit tests are the executable counterpart of dependency rules. They must cover the following restrictions whenever code to which a rule applies exists in the repository:

- `domain` classes do not depend on `application`, `infrastructure`, or `ui`, and do not depend on Spring MVC or `JdbcClient`;
- `application` classes do not depend on `infrastructure` or `ui`, and do not depend on Outside ports;
- `ui` classes do not access persistence types or `JdbcClient`;
- `infrastructure` classes do not create a reverse dependency from another layer and are not imported by Domain or Application;
- `SharedKernel` does not depend on business contexts;
- one context's Domain and Application layers do not import another context's Domain or Application layers;
- an allowed cross-context dependency resides in the consumer's Infrastructure layer and targets the owner's public contract;
- there are no dependency cycles between contexts or layers.

A rule may tolerate an absent package in an empty module, but it must automatically cover the first class added to that package. Technical exceptions, such as permitted mapping annotations in Domain, must be narrowly scoped, explicit, and testable. Table ownership and SQL content that cannot be assessed reliably from bytecode require review and focused integration tests. Checks that depend on runtime metadata, such as route naming, use a Spring context test when static analysis is insufficient.

## 13. Test strategy

- **Domain unit tests:** JUnit 6 verifies aggregate, value-object, factory, and policy behaviour, including invariants, material failure paths, and recorded events. Tests use fake Outside implementations and deterministic `Instant` values.
- **Integration tests:** Spring Boot Test and Testcontainers with PostgreSQL verify repository adapters, JPA mapping, `JdbcClient` queries, migrations, the event log, transaction boundaries, and outbox behaviour when a change affects those mechanisms.
- **Behaviour tests:** JUnit 6 and Spring Boot Test cover at least the happy path of a complete business flow through UI, Application, Domain, and Infrastructure, as well as material failure behaviour required by the use case's rules.
- **Architecture tests:** ArchUnit verifies static layer and context boundaries, while focused Spring context tests verify conventions visible only at runtime.

A separate mapping test is not required for every aggregate if a meaningful integration or behaviour test exercises persistence, `flush`, reload, and behaviour. A dedicated test is required when mapping lacks such coverage or is non-trivial.

Behaviour-test conventions:

- scenarios use readable aliases instead of raw UUIDs;
- **Given** arranges state through commands or Application use cases, never through HTTP;
- **When** performs the HTTP action under test;
- **Then** verifies state through a `JdbcClient` query service/read model; HTTP in this phase is used only to assert responses and error mapping;
- shared setup may use a small registry that maps aliases to identifiers when repetition justifies the abstraction;
- tests verify observable behaviour and contracts, not private implementation details.

## 14. Quality gates

The Gradle Wrapper is the sole repository entry point for building JVM code and running quality checks, and build configuration uses Kotlin DSL. Every change runs the gates relevant to its scope in the following order:

1. Kotlin formatting and linting;
2. detekt static analysis and compilation;
3. ArchUnit architecture tests;
4. unit and integration tests;
5. behaviour tests when a change affects UI or a complete flow.

Each gate must finish with its complete result and a successful exit code before the next begins. After a failure, fix the cause and rerun every affected gate. An aggregate Gradle task must not omit integration or behaviour tests or obscure the source of a failure.

A change that introduces an element covered by an architecture rule or test level must include the corresponding automated check. For a documentation-only change, the minimum gate is:

```text
git diff --check
```

## 15. Changing the architecture

- A decision that conflicts with this contract requires a separate ADR describing context, alternatives, and consequences.
- An accepted architecture change updates both language versions of this document and the corresponding ArchUnit tests or other automated checks at the same time.
- A local exception must not be hidden as an implementation detail. It must have a narrow scope and explicit justification next to the code or in an ADR.
- Concrete implementation tasks, work order, and delivery status belong in the backlog, not in this document.
