# Bank Account Service

A Spring Boot REST service for managing bank accounts.


## Assumptions

- Customer Name is unique id of a customer, which should be maintained by another service
- When we search accounts, at least one filter parameters should be provided, customerName, accountNumber, or nickname, one of them or a combination of them.
- Does not support pagination yet, which needs more work.


## Implementation Details

- Flyway — maintain database migration schemas
- Redis — query result caching (10-minute TTL). When a new account is created, the related cache will be evicted.
- JPA — implement ORM between DB tables and Java DTOs, and JPA query is used to implement search.
- Validations - Most validations are straightforward, are done by Springboot. The validation of max accounts for a customer is tricky, to avoid racing conditions or inconsistent data, DB triggers are used, which will make sure the insert operation is protected by transactions. 


## Test
- Unit tests are written to only validate validation logic and a happy scenario, where all dependencies are mocked.
- Integration tests are more comprehensive, it can validate DB and redis queries. It launches postgres and redis in containers, so testing code can talk to them.

### Prerequisites

- Java 21
- Docker — [Colima](https://github.com/abiosoft/colima) or Docker Desktop
- Maven

### Running Locally

#### 1. Start dependencies
```bash
docker compose up -d
```
#### 2. Run the application
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Or from your IDE, set the active profile to `local`.

### Run tests

```bash
./mvnw test
```

### AI
Claude code is used when writing these code.
