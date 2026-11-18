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

## Employee API (in-memory, data is lost on restart)

| Method | Path | Result |
|---|---|---|
| GET | `/api/employees` | List of employees (`[]` when empty) |
| GET | `/api/employees?name=an` | Employees whose name contains `an` (ignores case) |
| GET | `/api/employees/{id}` | One employee, or `404` |
| POST | `/api/employees` | Add an employee: `201` with a `Location` header, or `400` if `name`/`email` is blank |

```bash
curl -i -X POST http://localhost:8080/api/employees \
  -H "Content-Type: application/json" \
  -d '{"name":"nguyen van an","email":"an@example.com"}'
# 201, {"id":1,"code":"EMP0001","name":"Nguyen Van An","email":"an@example.com"}

curl -i http://localhost:8080/api/employees
curl -i "http://localhost:8080/api/employees?name=an"
curl -i http://localhost:8080/api/employees/1
```

On Windows PowerShell:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/employees -ContentType 'application/json' -Body '{"name":"nguyen van an","email":"an@example.com"}'
```

## Stop

```bash
docker compose down
```
