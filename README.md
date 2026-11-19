# Employee Management

Spring Boot 4 practice project (Java 25)

## Prerequisite

- Docker (with Docker Compose). No local Java, Maven or database is needed.

## Run

```bash
docker compose up --build
```

The first build downloads Maven dependencies, so it takes a few minutes.

## URL and port

- App: http://localhost:8080
- Hello API: http://localhost:8080/hello returns `Hello, Employee Management!`
- Default accounts: none yet.

## Stop

```bash
docker compose down
```
