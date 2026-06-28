# Bank Account Service

A Spring Boot REST service for managing bank accounts.


## Assumptions

- Customer Name is unique id of a customer, which should be maintained by another service
- When we search accounts, at least one filter parameter must be provided: `accountNumber`, `customerName`, or `accountNickname`, individually or in combination.
- Does not support pagination yet, which needs more work.


## Design

```
                        +------------------------------------------------+
                        |               Spring Boot App                  |
  Client  -- REST -->   |  Controller --> Circuit Breaker --> Service    |  -->  PostgreSQL
                        |                                    |           |
                        +------------------------------------|------------+
                                                             |
                                                           Redis
                                                   (cache read / write)
```

## Implementation Details

- Flyway — maintain database migration schemas
- Redis — query result caching (3-minute TTL). When a new account is created, the related cache is evicted.
- JPA — implement ORM between DB tables and Java DTOs, and JPA query is used to implement search.
- Validations — most validations are handled by Spring Boot. The max-accounts-per-customer rule is enforced via a DB trigger to prevent race conditions, ensuring the constraint is protected by the transaction.
- Circuit Breaker — handles DB unavailability. When DB connectivity fails, the circuit breaker opens and returns a 503 for the next 5 minutes without attempting further DB calls. `DataIntegrityViolationException` (e.g. trigger-enforced business rules) is explicitly excluded from tripping the circuit breaker. The circuit breaker evaluates the failure rate after a minimum of 6 calls within a sliding window of 10.


## More to consider
- Infrastructure setup, for example K8s
- Authentication, might be machine to machine OAuth2 flow
- Api Gateway and load balancer

## Test
- Unit tests are written to only validate validation logic and a happy scenario, where all dependencies are mocked.
- Integration tests are more comprehensive, it can validate DB and redis queries. It launches postgres and redis in containers, so testing code can talk to them.

### Prerequisites

- Java 21+
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
