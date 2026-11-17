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
- Default accounts: none yet.

> The project currently has only `spring-boot-starter` (no web server), so the app starts and exits.
> Port 8080 becomes active after a web starter is added.

## Stop

```bash
docker compose down
```
