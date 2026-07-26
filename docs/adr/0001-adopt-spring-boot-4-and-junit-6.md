# ADR 0001: Adopt Spring Boot 4 and JUnit 6

**Status:** Accepted

**Date:** 2026-07-26

## Context

The first executable baseline starts from a documentation-only repository with no application code or compatibility constraints. The architecture and backlog previously named JUnit 5, while the current stable Spring Boot generation provides and recommends JUnit 6 for Kotlin applications.

## Decision

Use Spring Boot 4 and its managed JUnit 6 testing baseline. Keep the Gradle Wrapper, Java 21 Toolchain, and Foojay resolver as the build and toolchain controls required by the product backlog.

## Consequences

- New tests use JUnit 6 and Spring Boot 4 testing support.
- Kotlin and other managed dependencies follow the versions selected by the Spring Boot 4 dependency set unless a concrete requirement justifies an override.
- The English and Polish architecture contracts and the product backlog use JUnit 6 consistently.
