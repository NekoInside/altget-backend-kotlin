# Repository Guidelines

## Project Structure & Module Organization

This is a Kotlin 2.2/Spring Boot backend targeting Java 17. Production code lives under `src/main/kotlin/ltd/guimc/web/altget`, organized by responsibility: `controller` for HTTP endpoints, `service` for business logic, `component` and `config` for cross-cutting infrastructure, and `entity`, `mapper`, and `enum` for persistence and API models. Tests mirror the package structure under `src/test/kotlin`. Runtime configuration is in `src/main/resources/application.yaml`; Flyway migrations are in `src/main/resources/db/migration`, and email templates are in `src/main/resources/templates/email`.

## Build, Test, and Development Commands

- `./gradlew test` (Linux/macOS) or `gradlew.bat test` (Windows): run the JUnit 5 test suite.
- `./gradlew bootJar` or `gradlew.bat bootJar`: build the executable Spring Boot jar at `build/libs/app.jar`.
- `./gradlew bootRun` or `gradlew.bat bootRun`: run the service locally; provide reachable MariaDB and Redis services and valid local configuration first.
- `./gradlew bootJar test`: reproduce the GitHub Actions build and test job.

Use JDK 17. If Java is not on `PATH`, set `JAVA_HOME` to the repository-compatible JDK under `~/.jdks`; see `AGENTS.user.md` when present for machine-specific setup notes. Tests disable Flyway, but integration/context tests can still require local infrastructure depending on the code under test.

## Coding Style & Naming Conventions

Use four-space indentation, Kotlin idioms, and concise functions. Keep packages lowercase; use `PascalCase` for classes, `camelCase` for properties/functions, and suffix Spring roles consistently (`Controller`, `Service`, `Mapper`, `Config`). Keep request/response/database models in their corresponding `entity` subpackages. No repository formatter or linter is configured, so preserve surrounding formatting and run a build before submission.

## Testing Guidelines

Add focused JUnit 5 tests beside the production package. Name files `*Test.kt` and use descriptive backtick test names where helpful. Include regression coverage for controller, service, security, payment, and migration-related changes. There is no stated coverage threshold; all tests must pass locally and in CI.

## Commit & Pull Request Guidelines

Use short imperative Conventional Commit-style subjects, matching history such as `feat: ...` and `fix: ...`. Keep commits focused. PRs should explain behavior changes, link the relevant issue when applicable, describe configuration or database migration impact, and include test evidence. CI must pass; include screenshots only when an API or rendered template change needs visual context.

## Security & Configuration Tips

Do not commit real credentials, JWT secrets, OAuth keys, mail credentials, or payment-provider keys. Use local overrides/environment variables for `application.yaml` values, and review Flyway SQL carefully because migrations run against the database on deployment.

## Local User Notes

Machine-specific contributor notes belong in the ignored `AGENTS.user.md`. Read it when present; do not assume it exists in a fresh clone or copy its private values into tracked files.
