# Product backlog

This backlog defines the implementation order and delivery scope from the repository's initial documentation-only state to the first complete business flow in the `Client` bounded context.

The repository is a local engineering demo verified by GitHub Actions. It has no VPS or other deployment target, no container registry, and no image-promotion pipeline in the planned scope.

The normative architecture contract remains [the English architecture document](architecture.en.md), with [the Polish architecture document](architecture.pl.md) maintained as its semantically aligned translation. This backlog records sequence and status; it does not replace or duplicate the complete architecture contract.

Each numbered item is a small delivery iteration and should be delivered as a separate, independently reviewable pull request. If an item must be split further, each resulting pull request must still have a coherent outcome and the order and status in this document must be updated.

Update this backlog whenever an item is completed or its outcome, scope, or order changes materially. Use only these statuses: `TODO`, `IN PROGRESS`, and `DONE`.

`SharedKernel` is not implemented as a framework in advance. It grows only when a concrete use case requires a mechanism that is genuinely shared and stable.

## Current delivery plan

Items are ordered by dependency. Implementation items remain `TODO` until their definition of done has been verified in the repository.

| Order | Iteration | Status |
| --- | --- | --- |
| 0 | Repository documentation foundation | `DONE` |
| 1 | Spring Boot development baseline | `DONE` |
| 2 | Production-like runtime image baseline | `DONE` |
| 3 | Unified quality gate and continuous integration | `DONE` |
| 4 | Client domain model | `DONE` |
| 5 | Client persistence baseline | `TODO` |
| 6 | Create Client application use case | `TODO` |
| 7 | Create Client HTTP flow and living documentation | `TODO` |

Commands listed under a `TODO` item are the acceptance contract for that iteration. Their inclusion does not claim that every command or capability required by that iteration is already available.

## Development and delivery model

### JVM build and test model

- A host JVM compiles and tests JVM code.
- The repository's Gradle Wrapper is the only supported Gradle entry point.
- Local development and CI execute the same Gradle tasks.
- Docker is not the environment for compiling or testing JVM code.
- Testcontainers communicates directly with the host Docker daemon.
- Docker-in-Docker is not introduced, and the Docker socket is not mounted into a container that runs Gradle.
- Java 21 is the application toolchain. A developer needs a host JVM supported by the selected Gradle version to start the Wrapper; the Java 21 toolchain may be installed locally or provisioned through the Foojay resolver. Docker with Docker Compose and the repository's Gradle Wrapper are the other local prerequisites. Network access is required for the first toolchain resolution when Java 21 is not already available.

### Runtime image model

- Docker packages and runs the application artifact already built by Gradle.
- The Dockerfile does not execute Gradle or rebuild application source code.
- `make image` first runs `./gradlew bootJar` and then runs `docker build` against that invocation's deterministic JAR path and name. It never packages an arbitrary file merely because it already exists under `build/libs`.
- Local development and CI use that same build-then-package command contract, but each invocation creates its own artifact.
- Within one GitHub Actions run, the image that is built is the exact image that is started and smoke-tested.
- The verified image is ephemeral and is discarded after the workflow run. The backlog does not claim artifact identity across runs, registry publication, promotion, or deployment by digest.

Deferring build-once promotion is intentional, but it has a structural cost if the project scope changes later: CI would need to publish and retain an image, capture its immutable digest, pass that identity between jobs or workflows, and make every later consumer use the digest without rebuilding. That would reshape the artifact lifecycle rather than add a single deployment step.

### Makefile model

The Makefile remains a thin wrapper around `./gradlew`, `docker build`, `docker compose`, and smoke-test commands. It does not contain an independent implementation of the quality pipeline.

The command contract evolves toward:

```text
make dev
make test
make qa
make image
make up
make down
make smoke
```

The essential mappings remain direct:

```text
make dev   -> ./gradlew bootRun
make test  -> ./gradlew test
make qa    -> ./gradlew qa
make image -> ./gradlew bootJar && docker build ...
```

CI invokes `./gradlew qa`; it does not reproduce the aggregate task's formatting, analysis, compilation, and test task list in workflow YAML.

In items 1–3, Actuator readiness represents process-level application readiness because no database exists. Item 5 deliberately changes the explicit readiness health group to include the datasource: from that point, the local and GitHub Actions smoke signal is successful only when the application can reach PostgreSQL. Liveness remains independent of the datasource so a database outage is not misreported as a dead JVM.

## 0. Repository documentation foundation

**Status:** `DONE`

**Goal**

Establish the verified documentation foundation that governs later implementation work.

**Scope**

- The project overview and current documentation-only status in `README.md`.
- The normative English architecture contract in `docs/architecture.en.md`.
- The semantically aligned Polish architecture contract in `docs/architecture.pl.md`.
- Coding-agent instructions in `docs/instructions-for-agents.md` and the repository-level instruction link.
- Links among the README, backlog, agent instructions, and architecture documents so their different responsibilities are discoverable.

**Out of scope**

- Any application, build, test, container, database, or CI implementation.
- Any future architecture or business implementation work.
- Treating planned technologies as already available.

**Definition of done**

- The README, both architecture language versions, and coding-agent instructions are present.
- The English and Polish architecture documents describe the same contract and link to each other.
- The agent instructions identify the English architecture document as the primary architecture source of truth.
- The README links to the architecture contract and this backlog without maintaining a competing detailed implementation list.
- The repository is still accurately described as documentation-only.

**Verification**

```text
test -f README.md
test -f docs/architecture.en.md
test -f docs/architecture.pl.md
test -f docs/instructions-for-agents.md
rg -n 'docs/(architecture\.en|product-backlog)\.md' README.md
git diff --check
```

## 1. Spring Boot development baseline

**Status:** `DONE`

**Goal**

Establish the smallest usable JVM application that can be compiled, tested, and run locally through the Gradle Wrapper.

**Scope**

- Kotlin targeting Java 21.
- Spring Boot and Gradle Kotlin DSL.
- The Gradle Wrapper as the repository Gradle entry point.
- a Java Toolchain configured for Java 21 as the build-level version requirement.
- The Foojay Toolchains resolver convention plugin so Gradle can provision a matching Java 21 toolchain when it is absent from a fresh environment.
- `.sdkmanrc` as an optional developer convenience for selecting JDK 21, not as the only version-control mechanism.
- A minimal Spring Boot application in the documented base package.
- Local startup through `./gradlew bootRun`.
- Spring Boot Actuator and an Actuator readiness endpoint.
- A minimal application-context test.
- Basic environment-based application configuration.
- The first thin Makefile commands: `make dev` and `make test`.

**Out of scope**

- PostgreSQL, Flyway, JPA, and Testcontainers.
- Business contexts, domain classes, and business behaviour in controllers.
- `SharedKernel`.
- Cucumber-JVM.
- ArchUnit rules for packages that do not exist.
- Placeholder business packages, empty layers, and speculative abstractions.

**Definition of done**

- `./gradlew test` succeeds with a Java 21 toolchain resolved by Gradle, whether that toolchain was already installed or downloaded through Foojay.
- `./gradlew bootRun` starts the application.
- The Actuator readiness endpoint responds successfully while the application is running.
- `make dev` and `make test` delegate directly to the corresponding Gradle Wrapper commands.
- The Java Toolchain enforces Java 21 independently of `.sdkmanrc`, and the Foojay resolver can provision it when the host does not already contain Java 21.
- A fresh checkout with a supported bootstrap JVM can resolve the Java 21 toolchain through Gradle and run the build; `.sdkmanrc` is not required.
- The application contains no placeholder business packages or business behaviour.

**Verification**

Run the readiness request in another terminal while `bootRun` is active.

```text
./gradlew -q javaToolchains
./gradlew test
./gradlew bootRun
curl --fail --silent --show-error http://localhost:8081/actuator/health/readiness
make dev
make test
```

## 2. Production-like runtime image baseline

**Status:** `DONE`

**Goal**

Create a production-like runtime image for local demonstration and GitHub Actions smoke testing without moving JVM compilation or testing into Docker.

**Scope**

- A Dockerfile that packages the JAR previously produced by the host Gradle build.
- A JRE 21 runtime base image rather than a full JDK image.
- Layered Spring Boot JAR extraction only if it provides a clear practical caching or startup benefit.
- Non-root container execution.
- Environment-based runtime configuration with no secrets embedded in image layers or defaults.
- Container-aware JVM memory configuration without a hardcoded universal `-Xmx`.
- Explicit graceful-shutdown configuration only if the selected Spring Boot version does not already provide the required behaviour by default.
- A production-like Compose file dedicated to running the built application image and kept distinct from the later local-service `compose.yaml`.
- A `make image` target that executes `./gradlew bootJar` and, only after it succeeds, executes `docker build` for a deterministic JAR path produced by that build.
- Thin `make up`, `make down`, and `make smoke` targets; `make up` starts the existing image and does not trigger another build.
- A readiness smoke request against the running image.

The Dockerfile may use multiple stages for packaging or layer extraction, but no stage may run Gradle or rebuild source code.

**Out of scope**

- Publishing an image to a registry.
- Production deployment or immutable-digest deployment wiring.
- PostgreSQL and other application services.
- Kubernetes or another orchestration platform.
- Business functionality.

**Definition of done**

- The host Gradle build produces the JAR before `docker build` runs.
- `docker build` packages that exact JAR and does not compile or test JVM code.
- The final image contains a JRE rather than a full JDK and runs as a non-root user.
- Runtime configuration is supplied externally and the image contains no secrets.
- The image starts through the production-like Compose definition.
- `make up` does not rebuild or replace the image created by `make image`.
- `make smoke` reaches the Actuator readiness endpoint without claiming to verify a database.
- `make image` always performs `./gradlew bootJar` followed by `docker build`; it cannot silently package a stale or ambiguous JAR left in `build/libs`.

**Verification**

```text
./gradlew bootJar
make image
make up
make smoke
make down
```

## 3. Unified quality gate and continuous integration

**Status:** `DONE`

**Goal**

Establish one quality contract used identically by developers and CI.

**Scope**

- Kotlin formatting, for example Spotless with ktlint.
- detekt static analysis.
- Kotlin compilation and JUnit 6 tests.
- One aggregate Gradle task named `qa` that runs every currently applicable quality gate.
- `make qa` delegating directly to `./gradlew qa`.
- A GitHub Actions CI workflow using Temurin 21.
- Official Gradle setup and dependency caching in CI.
- CI execution of `./gradlew qa` rather than a manually duplicated list of its internal tasks.
- CI execution of `make image`, which builds the application JAR on the CI host and immediately packages its deterministic output.
- Starting and smoke-testing the exact image produced by `make image` in that workflow run.
- Explicitly treating the image as a run-scoped verification artifact that is discarded after the job; no cross-run digest or promotion guarantee is claimed.

The aggregate Gradle task is the source of truth for code-quality checks and must grow when later iterations add applicable integration, architecture, or behaviour tests.

**Out of scope**

- Deployment and production credentials.
- Registry publishing and artifact promotion.
- PostgreSQL or other database integration.
- Business behaviour.
- Architecture rules for code that does not yet exist.

**Definition of done**

- One Gradle Wrapper command runs every quality gate applicable at this stage.
- Local development and CI invoke the same aggregate task.
- A failed formatting, static-analysis, compilation, or test gate stops CI.
- CI verifies both the host JVM build and the packaged runtime image.
- CI smoke-tests the image produced in the same workflow run and does not rebuild between image creation and smoke verification.
- CI does not compile or test JVM code inside Docker.
- The Makefile does not duplicate Gradle's quality-task graph.

**Verification**

```text
./gradlew qa
make qa
make image
make up
make smoke
make down
```

## 4. Client domain model

**Status:** `DONE`

**Goal**

Introduce the first real business model as pure Kotlin, independently of Spring and persistence.

**Scope**

- The first code in the `Client` bounded context, under only the packages required by the introduced types.
- `ClientId` as a domain identifier value object.
- `ClientName` as an immutable value object that normalizes surrounding whitespace, rejects blank values, and enforces the reference model's 120-character maximum.
- The `Client` aggregate and valid client creation.
- Renaming an active client, including no-op behaviour when the normalized name is unchanged.
- An optional client description, because it is part of the reference business model, including changing and clearing it while the client is active.
- Client deactivation, including deliberate behaviour for repeated deactivation.
- Rejection of rename and description changes after deactivation.
- Domain timestamps represented as `Instant` and obtained deterministically through the narrow Domain-owned mechanism required by this model, without calling `Instant.now()` or constructing a system clock in Domain.
- Domain unit tests covering creation, normalization, boundary lengths, state transitions, idempotent operations, inactive-client failures, and deterministic time where observable.
- ArchUnit and the first executable architecture rule: classes in `client.domain` must not depend on any Spring package.

The PHP reference is a source of business intent only. The model and tests must be idiomatic Kotlin and comply with the repository architecture contract.

**Out of scope**

- Spring annotations or Spring dependencies in Domain.
- HTTP, commands, handlers, and persistence.
- JPA, Flyway, repositories, and repository adapters.
- Empty layer packages and speculative ports or interfaces.
- A command bus or other generic application framework.
- Domain events, the event log, and outbox infrastructure.
- A broad or prebuilt `SharedKernel`.
- Layer, bounded-context, cycle, and `SharedKernel` ArchUnit rules whose relevant packages do not exist yet.

**Definition of done**

- The model is implemented as pure Kotlin.
- Domain code does not depend on Spring, Application, Infrastructure, or UI.
- The aggregate exposes behaviour rather than public mutable state or getters added for tests.
- `ClientName` owns its value-level validation and the aggregate owns state-dependent rules.
- Unit tests cover important invariants, boundary values, state transitions, no-op behaviour, and meaningful failure paths.
- Domain time is deterministic in tests and follows the architecture contract's Outside direction.
- The Domain-without-Spring ArchUnit rule runs as part of `./gradlew qa` as soon as the first Domain classes exist and protects iterations 4–6.
- No abstraction or package exists solely for possible future use.

**Verification**

```text
./gradlew test
./gradlew qa
```

## 5. Client persistence baseline

**Status:** `TODO`

**Goal**

Persist the `Client` aggregate in PostgreSQL without weakening the domain model.

**Scope**

- PostgreSQL as the real database used for local development and integration tests.
- A root `compose.yaml` for local development services.
- Spring Boot Docker Compose support for PostgreSQL during `bootRun`.
- Flyway with a migration owned by the `Client` context and a version unique in the combined application history.
- JPA/Hibernate for the `Client` write model.
- A repository port owned by `Client` Domain and introduced because persistence now requires it.
- A repository adapter in `Client` Infrastructure that keeps JPA and Spring Data types inside Infrastructure.
- Environment-based database configuration.
- A production Outside/time implementation backed by an injected `Clock` if required by the domain model.
- PostgreSQL Testcontainers for integration tests, communicating directly with the host Docker daemon.
- `@ServiceConnection` for the PostgreSQL Testcontainers instance when appropriate for the selected Spring Boot version.
- An integration test that persists the aggregate, performs an explicit `flush`, clears the persistence context, reloads the aggregate, and successfully executes further aggregate behaviour after reload.
- An ADR selecting the Kotlin/JPA mapping strategy after considering aggregate encapsulation and Kotlin constraints.
- A documented decision on whether application startup runs Flyway migrations.
- Any minimal adjustment needed to keep the production-like runtime Compose workflow operable with environment-provided PostgreSQL configuration.
- An explicitly configured Actuator readiness health group that includes the datasource health contributor from this iteration onward, because the implemented demo is unusable without PostgreSQL.
- Liveness that remains independent of PostgreSQL, plus a short decision note recording why readiness and liveness differ for this local and GitHub Actions demo.
- A focused integration test proving that readiness reflects datasource availability while liveness remains independent of a database outage, rather than trusting Spring Boot defaults.

The two development mechanisms remain distinct:

```text
bootRun -> Spring Boot Docker Compose support
test    -> Testcontainers
```

`@ServiceConnection` belongs to the Testcontainers test setup; it is not described as the Spring Boot Docker Compose integration mechanism.

**Out of scope**

- Docker-in-Docker.
- Gradle execution inside Docker or a Docker socket mounted into a Gradle container.
- Read models and `JdbcClient` queries.
- HTTP endpoints and command dispatching.
- Domain events, the event log, and transactional outbox.
- Production deployment.

**Definition of done**

- Flyway migrations run successfully against real PostgreSQL.
- Integration tests use PostgreSQL through Testcontainers and the host Docker daemon.
- The selected mapping strategy is recorded in an ADR and preserves aggregate encapsulation.
- The repository adapter exposes no JPA or Spring Data type outside Infrastructure and never owns `flush`, commit, or transaction boundaries.
- The persistence test proves that a cleared and reloaded aggregate remains behaviourally valid.
- `bootRun` and tests use their documented, distinct database-startup mechanisms.
- The Flyway startup decision is explicit and documented.
- Readiness is `UP` only while the application can reach PostgreSQL, while liveness does not fail solely because PostgreSQL is unavailable.
- `./gradlew qa` includes the applicable integration test.

**Verification**

```text
./gradlew test
./gradlew qa
./gradlew bootRun
curl --fail --silent --show-error http://localhost:8081/actuator/health/readiness
```

## 6. Create Client application use case

**Status:** `TODO`

**Goal**

Implement the first write use case without exposing it through HTTP yet.

**Scope**

- `CreateClientCommand` as an immutable application contract.
- A minimal typed creation result containing `ClientId`, because the following HTTP iteration needs to identify the created resource; no aggregate, JPA entity, or read model is returned.
- A command handler that orchestrates creation and persistence without reproducing domain validation.
- A domain factory only if valid creation needs one.
- Reuse of the Domain-owned repository port and its existing Infrastructure adapter.
- A minimal typed command dispatcher supporting the implemented command, without designing a framework for hypothetical commands.
- One central Spring transaction boundary around top-level command dispatch.
- One controlled `flush` point inside that command transaction and outside the repository adapter.
- Only the command and transaction contracts that this flow genuinely requires in `SharedKernel`.
- Application-level tests for handler orchestration.
- Transaction integration tests against PostgreSQL covering persistence, explicit flush, successful commit, and rollback on failure.

The handler orchestrates the use case; all business validation remains in Domain. The typed result is exposed only after the transaction commits successfully.

**Out of scope**

- HTTP and Cucumber-JVM.
- Platform authentication and tenant authorization.
- Domain-event recording or an event log unless a concrete consumer is introduced by a separately scoped change.
- Integration events, outbox, and worker infrastructure.
- A generic command framework designed for future use cases.
- Unrelated `SharedKernel` mechanisms.

**Definition of done**

- Dispatching a valid command creates and persists one valid `Client`.
- The result exposes only the created `ClientId` and is returned after a successful commit.
- Invalid business input is rejected by Domain, not reimplemented in the handler.
- Transaction ownership is unambiguous and centralized.
- Persistence and one controlled `flush` occur inside the single command transaction.
- A failing command leaves no partial database state.
- `SharedKernel` contains only mechanisms used by this implemented flow.

**Verification**

```text
./gradlew test
./gradlew qa
```

## 7. Create Client HTTP flow and living documentation

**Status:** `TODO`

**Goal**

Expose the first complete business flow from HTTP to PostgreSQL and preserve parity with the reference demo's acceptance-test and living-documentation approach.

**Scope**

- `POST /api/clients` as the first public business endpoint.
- Transport validation for a required, non-blank client name of at most 120 characters and an optional string description of at most 1,000 characters, without replacing Domain validation.
- Mapping valid HTTP input to `CreateClientCommand` and dispatching it through the typed command dispatcher.
- An HTTP `201 Created` response containing the created client identifier.
- Consistent client-safe error responses for malformed transport data and rejected business input.
- Cucumber-JVM integrated with JUnit Platform and Spring Boot Test.
- MockMvc for each scenario's `When` step.
- PostgreSQL Testcontainers for the behaviour suite.
- A focused `JdbcClient` query and immutable read DTO used by the `Then` step to verify observable persisted state; this is not a client-listing or general read-model framework.
- A readable Gherkin feature containing at least successful client creation and rejection of invalid client data.
- The remaining applicable ArchUnit rules for layer direction, bounded-context isolation, cycles, and `SharedKernel`, building on the Domain-without-Spring rule introduced in item 4.
- Inclusion of the behaviour and architecture suites in `./gradlew qa`.
- CI invoking `make image`, starting that run-scoped image with its required PostgreSQL service, smoke-checking the packaged application, and discarding the image after the workflow run.

Behaviour scenarios follow the architecture contract:

```text
Given -> arrange state through commands or application use cases
When  -> perform the HTTP action with MockMvc
Then  -> verify state through a JdbcClient query/read DTO
```

If a creation scenario requires no prior business state, its `Given` step should remain minimal. Any state that is required must not be arranged through HTTP.

Cucumber-JVM is the JVM counterpart of Behat for this repository. Its feature files are visible living documentation of the main business flows, not merely another testing API.

Readiness uses the explicit datasource-backed health group selected in item 5; the runtime smoke therefore checks that both the application and its required PostgreSQL dependency are usable. It is not evidence of any deployment environment because none is in scope.

**Out of scope**

- Client listing or a general-purpose read-model framework.
- User and OTP authentication.
- `ClientMember` and tenant authorization.
- Domain events, event log, outbox, and asynchronous processing.
- Unrelated refactoring or additional endpoints.

**Definition of done**

- A valid request completes the flow from HTTP through UI, Application, and Domain to PostgreSQL.
- The success response is `201 Created` and contains the created client identifier.
- Invalid client data produces the documented error response and no client row.
- The main flow and material invalid-input behaviour are readable Gherkin scenarios.
- MockMvc verifies the inbound HTTP contract.
- A `JdbcClient` query verifies the resulting database state without exposing the aggregate.
- The behaviour suite runs against PostgreSQL through Testcontainers.
- ArchUnit retains the Domain-without-Spring rule and adds the layer, dependency-direction, context, cycle, and `SharedKernel` boundaries now applicable to the complete package structure.
- The aggregate quality task includes unit, integration, architecture, and behaviour tests applicable to the repository.
- CI verifies the host JVM build and smoke-tests the rebuilt runtime image without compiling JVM code inside Docker.

**Verification**

```text
./gradlew test
./gradlew qa
make image
make up
make smoke
make down
```

## Future work after the first complete Client flow

High-level candidates only:

- Client read models with Spring `JdbcClient`.
- User and OTP authentication.
- `ClientMember` and tenant authorization.
- Domain events and an event log.
- Transactional outbox and asynchronous processing.
- Additional business flows and architecture rules as corresponding code is introduced.
