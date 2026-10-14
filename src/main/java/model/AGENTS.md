# Repository Guidelines

## Project Structure & Module Organization

This guide is in `src/main/java/model`, which currently contains the `model` package (including `anuncio.java`). The repository root is four levels above this directory. Application code lives under `src/main/java/com/dev/isac_plus`; runtime configuration is in `src/main/resources/application.properties`; tests are in `src/test/java/com/dev/isac_plus`. The Maven build and wrapper are at the repository root (`pom.xml`, `mvnw`, and `mvnw.cmd`). Keep Java packages aligned with their directory paths.

## Build, Test, and Development Commands

Run commands from the repository root:

- `./mvnw test` compiles the project and runs the test suite.
- `./mvnw spring-boot:run` starts the application locally.
- `./mvnw package` builds the application JAR under `target/`.

On Windows, use `mvnw.cmd` with the same Maven goals. The project targets Java 17.

## Coding Style & Naming Conventions

Follow the existing Java style: tab indentation, one top-level type per file, and standard Java naming (UpperCamelCase classes, lowerCamelCase methods and fields). Keep packages lowercase and use the established root package `com.dev.isac_plus`. Use descriptive names; Java source filenames should match their public type names. No formatter or linter is configured in the current build.

## Testing Guidelines

Tests use JUnit 5 and Spring Boot's test support. Place tests under the matching package in `src/test/java`, name them `*Tests` or `*Test`, and use `./mvnw test` to run them. Add focused tests for new behavior; the current suite includes a Spring application context smoke test.

## Commit & Pull Request Guidelines

The available history contains only the initial commit (`primeiro commit`), so no established message convention can be inferred. Use a short imperative subject that describes the change, optionally with a scope (for example, `model: add announcement fields`). Pull requests should explain the behavior change, list validation performed, and link any related issue. Include screenshots only when a change affects a user-visible interface.

## Configuration

Keep environment-specific values, especially database credentials, out of committed configuration. Use local environment variables or an untracked local configuration file when adding external service settings.
