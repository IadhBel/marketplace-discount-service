# Discount Kata

Solution to the Carrefour Java Kata (see [`instruction.md`](instruction.md)): a customer applies a discount code to a cart before checkout, and codes can be restricted to certain products.

## Documentation

| Document | Content |
|---|---|
| [`instruction.md`](instruction.md) | The original kata brief |
| [`BACKEND.md`](BACKEND.md) | Business rules, architecture, REST contract, persistence, tests and run instructions of the backend |

## Tech stack

Java 25, Spring Boot 4.1, Gradle (Groovy), PostgreSQL, Flyway, MapStruct, Springdoc OpenAPI, Lombok.

## Repository layout

```
.
├── backend/                 Spring Boot application (Gradle subproject), with its Dockerfile
├── frontend/                Vue.js application (not started)
├── keycloak/                realm imported by the local Keycloak
├── docker-compose.yml       local PostgreSQL and Keycloak
├── .gitlab-ci.yml           CI/CD pipeline
├── settings.gradle          Gradle multi-project (includes backend)
├── gradlew                  Gradle wrapper
├── BACKEND.md               functional and technical specification
└── instruction.md           kata brief
```

The backend follows Clean Architecture with a hexagonal approach (Ports & Adapters) and light CQRS: `domain`, `application` and `adapter` packages under `com.carrefour.discountkata.discount`. The detailed target structure (packages, classes, tests and naming rules) is in [`BACKEND.md`](BACKEND.md), section 3.3.

## Prerequisites

- JDK 25
- Docker (PostgreSQL, Keycloak)

## Current status

Implemented: Gradle project and Spring Boot skeleton (dependencies, configuration), local PostgreSQL and Keycloak with Docker Compose (not run yet).

Not implemented yet: the discount feature (domain, application, persistence, REST API), its tests, and the Vue.js frontend required by the brief. The application cannot be started yet. The detailed status of each feature is tracked in [`BACKEND.md`](BACKEND.md), section 11.

## Useful commands

All commands are run from the repository root.

```
./gradlew :backend:build            # compile, test and check formatting
./gradlew :backend:spotlessApply    # format the code
docker compose up -d                # start PostgreSQL and Keycloak locally
```

The GitLab pipeline (`.gitlab-ci.yml`) builds, tests, checks formatting, runs Sonar and OWASP Dependency-Check, then publishes the Docker image. Its configuration and secrets are described in [`BACKEND.md`](BACKEND.md), section 15.

This section will be extended as features are added and tested.
