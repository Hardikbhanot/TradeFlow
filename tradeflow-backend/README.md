# TradeFlow — Wallet Service

Lightweight Spring Boot microservice that manages wallets and transaction records for the TradeFlow backend.

This module lives at `tradeflow-backend/wallet_service` and is part of the larger TradeFlow multi-module project.

## Features

- Wallet CRUD operations
- Transaction recording and querying
- Persistence via Spring Data JPA
- Designed to run independently or together with other backend services

## Tech stack

- Java (8+ / 11+ recommended)
- Spring Boot
- Spring Data JPA (Hibernate)
- Maven (wrapper included)

## Prerequisites

- JDK 11 or newer installed and JAVA_HOME set
- Git (optional)
- A relational database (MySQL, PostgreSQL, etc.) for production. The project may include an in-memory DB for development (check `application.properties`).

You can use the included Maven wrapper (`./mvnw`) so Maven does not need to be installed globally.

## Configuration

Edit `src/main/resources/application.properties` to configure:

- `spring.datasource.url` — JDBC URL for your database
- `spring.datasource.username` and `spring.datasource.password`
- `server.port` — service port (defaults usually to 8080)

If the project uses profiles, set `spring.profiles.active` to switch between `dev`, `test`, and `prod` configurations.

## Build

From the module root (`tradeflow-backend/wallet_service`):

- Using the wrapper:
  ./mvnw clean package

- Or with a system Maven installation:
  mvn clean package

This produces a runnable JAR under `target/`.

## Run

- Run with the Maven Spring Boot plugin:
  ./mvnw spring-boot:run

- Or run the packaged jar:
  java -jar target/*.jar

When running as part of the full backend, start other required services (discovery, API gateway, etc.) as needed.

## Common API (example)

Controller and routing details may vary. The repository contains `TransactionRepository` with:

- `List<Transaction> findByWalletIdOrderByTimestampDesc(Long walletId)` — typically used to return a wallet's transactions ordered newest first.

Example (adjust paths if controllers differ):

- Get transactions for a wallet:
  curl -X GET "http://localhost:8080/wallets/{walletId}/transactions"

Replace host/port/path according to your running configuration.

## Database migrations

If the project uses Flyway or Liquibase, migration files will be in `src/main/resources/db/migration` or a similar directory — check the codebase and the `pom.xml` for configuration.

## Tests

Run unit/integration tests:

  ./mvnw test

Test reports will be available under `target/surefire-reports` / `target/failsafe-reports` depending on configuration.

## Debugging / Development tips

- Use your IDE to run the Spring Boot application from the main class in `src/main/java` for faster edit-run-debug cycles.
- Enable a dev profile with an embedded DB for quick local testing.
- Check logs in `target` or console output for startup problems.

## Contributing

1. Create a branch for your feature or fix: `git checkout -b feature/your-feature`
2. Run tests and ensure the project builds
3. Open a pull request with a description of the change

## License

This repository may include a license file at the project root. If not, check with the project owner for the intended license.

---

If you want a README tailored to the whole multi-module backend or a more detailed API reference generated from the actual controllers, I can create that next.
