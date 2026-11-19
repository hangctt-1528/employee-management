# Employee Management

Spring Boot 4 practice project (Java 25): web pages (Thymeleaf) and a REST API for employees and departments, stored in MySQL with Spring Data JPA. Logging with SLF4J + Logback, profiles `dev` / `prod`. Actuator (health, metrics), a scheduled task and a cached report API.

## Prerequisite

- Docker (with Docker Compose). No local Java, Maven or database is needed.

## Run

```bash
docker compose up --build
```

This starts two containers: `db` (MySQL 8.4) and `app`. The app waits until MySQL is healthy.
The first build downloads Maven dependencies and the MySQL image, so it takes a few minutes.
Docker Compose runs the app with the `prod` profile (see [Profiles](#profiles)).

## URL and port

- App: http://localhost:8080
- Web pages: http://localhost:8080/employees/list (see [Web pages](#web-pages))
- Hello API: http://localhost:8080/hello returns `Hello, Employee Management!`
- Report API: http://localhost:8080/api/reports/employee-count
- Actuator: http://localhost:8080/actuator/health (same port, see [Actuator](#actuator))
- MySQL: `localhost:3307` (inside Docker: `db:3306`), database `employee_db`, user `employee` / password `employee` (development only)
- Default accounts: none yet.

Sample departments `IT`, `HR`, `Sales` are created at the first start.

## Environment variables (app)

| Variable | Value in docker-compose.yml | Default when not set |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` | `dev` |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://db:3306/employee_db?...` | dev: `jdbc:mysql://localhost:3307/employee_db?...`; prod: required |
| `SPRING_DATASOURCE_USERNAME` | `employee` | dev: `employee`; prod: required |
| `SPRING_DATASOURCE_PASSWORD` | `employee` | dev: `employee`; prod: required |

To run the app on your machine with the `dev` profile, start only MySQL with compose; `dev` already points to port 3307:

```bash
docker compose up -d db
./mvnw spring-boot:run        # PowerShell: .\mvnw spring-boot:run
```

If you use a MySQL installed on your machine (port 3306) instead, set `SPRING_DATASOURCE_URL`.

Emails are stored in lower case, so `An@Example.com` and `an@example.com` count as the same email.

## Profiles

Config files in `src/main/resources`: `application.yml` (shared, default profile `dev`), `application-dev.yml`, `application-prod.yml`.

| Profile | Used by | Database | Logs |
|---|---|---|---|
| `dev` (default) | `./mvnw spring-boot:run` | Environment variables, with defaults for `localhost:3307` | `DEBUG` for the app, plus every SQL statement (`org.hibernate.SQL`) |
| `prod` | `docker compose up` (`SPRING_PROFILES_ACTIVE: prod`) | Only from the 3 `SPRING_DATASOURCE_*` variables; if one is missing the app stops at startup | `INFO` |

To change the profile, set `SPRING_PROFILES_ACTIVE=prod`, or run `./mvnw spring-boot:run -Dspring-boot.run.profiles=prod` (with the 3 `SPRING_DATASOURCE_*` variables set).

## Logs

The app logs to the console (Spring Boot's default Logback setup). With Docker:

```bash
docker compose logs -f app
```

| Event | Level | Example |
|---|---|---|
| Employee added / updated / deleted | `INFO` | `c.e.e.service.EmployeeService : Employee added: id=1, departmentId=1` |
| Refused: email already used, employee or department not found | `WARN` | `Cannot add employee: email is already used`, `Employee not found: id=999` |
| Start of each add / update / delete (`dev` only) | `DEBUG` | `Adding employee: departmentId=1` |
| Unexpected error (500) | `ERROR` + stack trace | `Unexpected error on GET /api/employees/1` |
| Every 30 seconds | `INFO` | `c.e.e.scheduler.SystemStatusTask : System running` |
| Report counted in the database / report cache cleared (`dev` only) | `DEBUG` | `Counting employees in the database`, `Employee count cache cleared` |

Logs contain only ids, never names or emails.

## Actuator

Spring Boot Actuator runs on the same port 8080. Only these endpoints are open; any other `/actuator/...` path returns `404`.

| URL | Shows |
|---|---|
| http://localhost:8080/actuator/health | `UP` / `DOWN` (`503`), with details: database (`db`), disk space |
| http://localhost:8080/actuator/metrics | Names of all metrics; one metric: e.g. `/actuator/metrics/http.server.requests` or `/actuator/metrics/jvm.memory.used` |
| http://localhost:8080/actuator/caches | The caches (`employeeCount`); `DELETE` on this URL clears them |
| http://localhost:8080/actuator/scheduledtasks | The scheduled tasks and their interval |

There is no login yet (Lab 9), so don't expose this app to the Internet.

## Scheduled tasks and cache

| Task | Interval | What it does |
|---|---|---|
| `SystemStatusTask` | 30 s (first run at startup) | Logs `System running` |
| `EmployeeCountCacheTask` | 60 s | Clears the cache of the report API |

`GET /api/reports/employee-count` is cached, so its result is at most 1 minute old:

- Call it twice: the same `countedAt` means the second answer came from the cache (no database query).
- Add an employee: the total does **not** change until the cache is cleared (up to 1 minute). This is on purpose.

## Web pages

Open in a browser:

| URL | Page |
|---|---|
| http://localhost:8080/employees/list | Table of all employees |
| http://localhost:8080/employees/add | Form to add an employee; invalid data is shown under each field |
| http://localhost:8080/employees/search | Search by name and/or department, with the results below the form |

Errors on these pages are shown as an HTML error page.

## API

| Method | Path | Result |
|---|---|---|
| GET | `/api/departments` | List of departments (use their `id` as `departmentId`) |
| GET | `/api/employees` | List of employees |
| GET | `/api/employees?name=an&departmentId=1` | Search by name (contains, ignores case) and/or department; both given = AND |
| GET | `/api/employees/{id}` | One employee; `404` if not found; `400` if `id` is not a number |
| POST | `/api/employees` | Add: `201` + `Location`; `400` invalid data or unknown department; `409` email already used |
| PUT | `/api/employees/{id}` | Update: `200`; `400` as above; `404` if no employee; `409` if another employee has the email |
| DELETE | `/api/employees/{id}` | Delete: `204`, or `404` |
| GET | `/api/reports/employee-count` | Total number of employees, e.g. `{"totalEmployees":5,"countedAt":"2026-11-19T10:15:30.123"}`; cached up to 1 minute (`countedAt` = when it was counted) |

Validation rules for the POST / PUT body:

| Field | Rule |
|---|---|
| `name` | required, not blank, at most 100 characters |
| `email` | required, a valid email address, at most 100 characters |
| `departmentId` | required, a number, must be an existing department |

```bash
curl -i http://localhost:8080/api/departments

curl -i -X POST http://localhost:8080/api/employees \
  -H "Content-Type: application/json" \
  -d '{"name":"nguyen van an","email":"an@example.com","departmentId":1}'
# 201, {"id":1,"name":"Nguyen Van An","email":"an@example.com","departmentId":1,"departmentName":"IT"}

curl -i "http://localhost:8080/api/employees?name=an&departmentId=1"

curl -i -X PUT http://localhost:8080/api/employees/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"nguyen van an","email":"an.nguyen@example.com","departmentId":2}'

curl -i -X DELETE http://localhost:8080/api/employees/1

curl -i http://localhost:8080/api/reports/employee-count
```

On Windows PowerShell:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/employees -ContentType 'application/json' -Body '{"name":"nguyen van an","email":"an@example.com","departmentId":1}'
```

## Errors

Every error of the REST API (`/api/...`, and unknown URLs) returns the same JSON shape:

```json
{
  "timestamp": "2026-11-18T10:15:30.123",
  "status": 400,
  "error": "Bad Request",
  "message": "Request data is not valid",
  "path": "/api/employees",
  "fieldErrors": {
    "email": "Email is not valid",
    "name": "Name must not be blank"
  }
}
```

`fieldErrors` is filled only for validation errors; otherwise it is `{}`.

| Status | When |
|---|---|
| `400` | Validation failed (`fieldErrors`), unknown department, broken JSON, or a value of the wrong type (e.g. `/api/employees/abc`) |
| `404` | Employee not found, or no endpoint at this URL |
| `405` | HTTP method not supported for this URL |
| `409` | Email already used by another employee |
| `415` | `Content-Type` is not `application/json` |
| `500` | Unexpected error (fixed message; details are only in the app log as `ERROR`, see [Logs](#logs)) |

```bash
curl -i -X POST http://localhost:8080/api/employees \
  -H "Content-Type: application/json" \
  -d '{"name":"","email":"not-an-email","departmentId":1}'
# 400, fieldErrors: {"name":"Name must not be blank","email":"Email is not valid"}

curl -i http://localhost:8080/api/employees/999
# 404, "message":"Employee not found with id 999"
```

## Tests

`./mvnw test` uses an in-memory H2 database (`src/test/resources/application.yml`, which replaces the main `application.yml`), so no profile is active and it doesn't need MySQL.

## Stop

```bash
docker compose down      # stop; the data stays in the db-data volume
docker compose down -v   # stop and delete all data
```
