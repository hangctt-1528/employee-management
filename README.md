# Employee Management

Spring Boot 4 practice project (Java 25): web pages (Thymeleaf) and a REST API for employees and departments, stored in MySQL with Spring Data JPA. Logging with SLF4J + Logback, profiles `dev` / `prod`. Actuator (health, metrics), a scheduled task and a cached report API. Statistics (employees per department, total) as REST API and web page, computed with `@Query`. Spring Security: login page for the web pages, HTTP Basic or JWT for the REST API, roles USER (read only) and ADMIN (full CRUD).

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
- Login: http://localhost:8080/login, register: http://localhost:8080/register
- Web pages: http://localhost:8080/employees/list (see [Web pages](#web-pages))
- Hello API: http://localhost:8080/hello returns `Hello, Employee Management!` (open to everyone)
- Report API: http://localhost:8080/api/reports/employee-count, http://localhost:8080/api/reports/statistics, http://localhost:8080/api/reports/employees-by-department
- Statistics page: http://localhost:8080/employees/statistics
- Actuator: http://localhost:8080/actuator/health (same port, see [Actuator](#actuator))
- MySQL: `localhost:3307`, reachable only from this machine (inside Docker: `db:3306`), database `employee_db`, user `employee` / password `employee` (development only)

Sample departments `IT`, `HR`, `Sales` are created at the first start.

### Default accounts

| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | ADMIN: read and add / update / delete employees |
| `user` | `user1234` | USER: read only |

They are created at the first start from `APP_ADMIN_PASSWORD` / `APP_USER_PASSWORD`. Changing these variables later does not change an existing account. Accounts created on the `/register` page are always USER.

## Environment variables (app)

| Variable | Value in docker-compose.yml | Default when not set |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` | `dev` |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://db:3306/employee_db?...` | dev: `jdbc:mysql://localhost:3307/employee_db?...`; prod: required |
| `SPRING_DATASOURCE_USERNAME` | `employee` | dev: `employee`; prod: required |
| `SPRING_DATASOURCE_PASSWORD` | `employee` | dev: `employee`; prod: required |
| `APP_ADMIN_PASSWORD` | `admin123` | dev: `admin123`; prod: required (at least 8 characters) |
| `APP_USER_PASSWORD` | `user1234` | dev: `user1234`; prod: required (at least 8 characters) |
| `APP_JWT_SECRET` | `compose-secret-...` | dev: a development key; prod: required (at least 32 characters) |
| `APP_COOKIE_SECURE` | not set | prod: `false`. Set `true` only behind HTTPS (the session cookie is then sent over HTTPS only) |

The values in `docker-compose.yml` are for development only: change them for a real deployment.

To run the app on your machine with the `dev` profile, start only MySQL with compose; `dev` already points to port 3307:

```bash
docker compose up -d db
./mvnw spring-boot:run        # PowerShell: .\mvnw spring-boot:run
```

If you use a MySQL installed on your machine (port 3306) instead, set `SPRING_DATASOURCE_URL`.

Emails are stored in lower case, so `An@Example.com` and `an@example.com` count as the same email.

## Security

| | Visitor (not logged in) | USER | ADMIN |
|---|---|---|---|
| `/login`, `/register`, `/hello`, `/actuator/health` | yes | yes | yes |
| Web: `/employees/list`, `/employees/search`, `/employees/statistics` | redirect to `/login` | yes | yes |
| Web: `/employees/add` | redirect to `/login` | `403` page | yes |
| API: `GET /api/employees/**`, `/api/departments`, `/api/reports/**`, `/api/auth/me` | `401` | yes | yes |
| API: `POST` / `PUT` / `DELETE /api/employees/**`, and any other `/api` request | `401` | `403` | yes |
| Other Actuator endpoints | `401` | `403` | yes |

- **Web pages:** log in on `/login` (session cookie), log out with the "Log out" button. Forms are protected against CSRF. USER does not see the "Add employee" link.
- **Register:** on `/register` (username, password, confirm password). The new account is always USER. Username: 3 to 50 letters, digits, `.`, `_` or `-`, not case sensitive (`Binh` = `binh`). Password: at least 8 characters and at most 72 bytes (a letter with an accent counts as 2 or 3 bytes), stored as a BCrypt hash.
- **REST API**, two ways (no session, no CSRF token needed):
  - **HTTP Basic:** send the username and password with every request: `curl -u user:user1234 ...`
  - **JWT:** `POST /api/auth/login` returns a token; send it with every request as `Authorization: Bearer <token>`. The token is valid for 1 hour (`app.security.jwt-expiration-seconds`), signed with `APP_JWT_SECRET` (HS256); the app checks the signature, the expiry and the issuer (`iss`). There is no refresh token and a token cannot be revoked: to log out, the client just forgets it.
- `GET /api/auth/me` returns who you are (use it to test a login).
- HTTP Basic, the JWT and the session cookie travel in clear text over HTTP: use HTTPS outside your machine (and set `APP_COOKIE_SECURE=true`).
- The app container runs as a normal user (not root).

```bash
# HTTP Basic
curl -i -u user:user1234 http://localhost:8080/api/auth/me

# JWT: log in, then use the token
curl -i -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
# 200, {"accessToken":"eyJhbGciOiJIUzI1NiJ9...","tokenType":"Bearer","expiresIn":3600}

curl -i http://localhost:8080/api/employees -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

## Profiles

Config files in `src/main/resources`: `application.yml` (shared; `dev` is used when no profile is set), `application-dev.yml`, `application-prod.yml`.

| Profile | Used by | Database and passwords | Logs |
|---|---|---|---|
| `dev` (default) | `./mvnw spring-boot:run` | Environment variables, with development defaults (`localhost:3307`, `admin123`...) | `DEBUG` for the app, plus every SQL statement (`org.hibernate.SQL`) |
| `prod` | `docker compose up` (`SPRING_PROFILES_ACTIVE: prod`) | Only from the environment variables; if one is missing the app stops at startup | `INFO` |

To change the profile, set `SPRING_PROFILES_ACTIVE=prod`, or run `./mvnw spring-boot:run -Dspring-boot.run.profiles=prod` (with all the variables of the table above set).

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
| Statistics computed (`dev` only) | `DEBUG` | `Statistics computed: totalEmployees=3, departments=3` |
| Account created | `INFO` | `Default account created: id=1, role=ADMIN`, `User registered: id=3, role=USER` |
| Refused: username taken, role not allowed (API) | `WARN` | `Cannot register user: username is already used`, `Access denied on DELETE /api/employees/1` |

Logs contain only ids and roles, never names, emails, usernames or passwords.

## Actuator

Spring Boot Actuator runs on the same port 8080. Only these endpoints are open; any other `/actuator/...` path returns `404`.

| URL | Who | Shows |
|---|---|---|
| http://localhost:8080/actuator/health | everyone | `UP` / `DOWN` (`503`); details (database `db`, disk space) only for ADMIN |
| http://localhost:8080/actuator/metrics | ADMIN | Names of all metrics; one metric: e.g. `/actuator/metrics/http.server.requests` or `/actuator/metrics/jvm.memory.used` |
| http://localhost:8080/actuator/caches | ADMIN | The caches (`employeeCount`); `DELETE` on this URL clears them |
| http://localhost:8080/actuator/scheduledtasks | ADMIN | The scheduled tasks and their interval |

```bash
curl -u admin:admin123 http://localhost:8080/actuator/health
curl -u admin:admin123 http://localhost:8080/actuator/metrics
```

## Scheduled tasks and cache

| Task | Interval | What it does |
|---|---|---|
| `SystemStatusTask` | 30 s (first run at startup) | Logs `System running` |
| `EmployeeCountCacheTask` | 60 s | Clears the cache of the report API |

`GET /api/reports/employee-count` is cached, so its result is at most 1 minute old:

- Call it twice: the same `countedAt` means the second answer came from the cache (no database query).
- Add an employee: the total does **not** change until the cache is cleared (up to 1 minute). This is on purpose.

The statistics (`/api/reports/statistics`, `/api/reports/employees-by-department`, `/employees/statistics`) are **not** cached: they always show the current data, so for up to 1 minute their total can differ from `employee-count`.

## Pagination

The employee list is paged with Spring Data (`Pageable` / `Page`): the database returns only one page (`LIMIT` / `OFFSET`) plus a `COUNT` for the totals. Employees are always ordered by `id`.

| Parameter | Default | Rule |
|---|---|---|
| `page` | `0` | 0-based (the web pages show it + 1); `0` or more, otherwise `400` |
| `size` | `10` | `1` to `100`, otherwise `400` |

`GET /api/employees` returns an object (not a plain array):

```json
{
  "content": [{"id":11,"name":"Nguyen Van An","email":"an@example.com","departmentId":1,"departmentName":"IT"}],
  "page": 1, "size": 10, "totalElements": 23, "totalPages": 3, "first": false, "last": false
}
```

A page after the last one returns `200` with `"content": []` (the web page shows "Page N does not exist." with a link to the last page).

```bash
curl -u user:user1234 "http://localhost:8080/api/employees?page=1&size=10"
```

## Statistics

Computed in the database:

- Per department, a JPQL `@Query` in `DepartmentRepository.countEmployeesByDepartment()`: `select new ...DepartmentEmployeeCount(d.id, d.name, count(e)) from Department d left join Employee e on e.department = d group by d.id, d.name order by count(e) desc, d.name asc`. `LEFT JOIN` keeps departments with 0 employees.
- Total: `count()`, already provided by `JpaRepository`.

Both run in one read-only transaction, so the total always equals the sum of the rows.

| Where | Shows |
|---|---|
| http://localhost:8080/employees/statistics | Page: total, table "Department / Employees" (most employees first) and a Total row |
| `GET /api/reports/statistics` | `{"totalEmployees":3,"departments":[{"departmentId":1,"departmentName":"IT","employeeCount":2},...],"generatedAt":"..."}` |
| `GET /api/reports/employees-by-department` | Only the list `[{"departmentId":1,"departmentName":"IT","employeeCount":2},...]` |

## Web pages

Open in a browser (log in first; see [Default accounts](#default-accounts)):

| URL | Role | Page |
|---|---|---|
| http://localhost:8080/login | everyone | Login form |
| http://localhost:8080/register | everyone | Create a USER account |
| http://localhost:8080/employees/list | USER, ADMIN | Table of employees, 10 per page (Prev / page numbers / Next) |
| http://localhost:8080/employees/add | ADMIN | Form to add an employee; invalid data is shown under each field |
| http://localhost:8080/employees/search | USER, ADMIN | Search by name and/or department, with the results below the form (paged; the page links keep the conditions) |
| http://localhost:8080/employees/statistics | USER, ADMIN | Total employees and employees per department (see [Statistics](#statistics)) |

Errors on these pages are shown as an HTML error page (`403` when the role is not allowed).

## API

| Method | Path | Role | Result |
|---|---|---|---|
| POST | `/api/auth/login` | everyone | `{"username","password"}` -> `200` + JWT; `401` wrong username or password; `400` blank field |
| GET | `/api/auth/me` | logged in | Who you are: `{"id":1,"username":"admin","role":"ADMIN"}` |
| GET | `/api/departments` | USER, ADMIN | List of departments (use their `id` as `departmentId`) |
| GET | `/api/employees` | USER, ADMIN | One page of employees (10 by default), see [Pagination](#pagination) |
| GET | `/api/employees?name=an&departmentId=1&page=0&size=10` | USER, ADMIN | Search by name (contains, ignores case) and/or department; both given = AND; paged |
| GET | `/api/employees/{id}` | USER, ADMIN | One employee; `404` if not found; `400` if `id` is not a number |
| POST | `/api/employees` | ADMIN | Add: `201` + `Location`; `400` invalid data or unknown department; `409` email already used |
| PUT | `/api/employees/{id}` | ADMIN | Update: `200`; `400` as above; `404` if no employee; `409` if another employee has the email |
| DELETE | `/api/employees/{id}` | ADMIN | Delete: `204`, or `404` |
| GET | `/api/reports/employee-count` | USER, ADMIN | Total number of employees, e.g. `{"totalEmployees":5,"countedAt":"2026-11-19T10:15:30.123"}`; cached up to 1 minute (`countedAt` = when it was counted) |
| GET | `/api/reports/statistics` | USER, ADMIN | Total + employees per department, not cached (see [Statistics](#statistics)) |
| GET | `/api/reports/employees-by-department` | USER, ADMIN | Employees per department only |

Validation rules for the POST / PUT body:

| Field | Rule |
|---|---|
| `name` | required, not blank, at most 100 characters |
| `email` | required, a valid email address, at most 100 characters |
| `departmentId` | required, a number, must be an existing department |

```bash
curl -i -u user:user1234 http://localhost:8080/api/departments

curl -i -u admin:admin123 -X POST http://localhost:8080/api/employees \
  -H "Content-Type: application/json" \
  -d '{"name":"nguyen van an","email":"an@example.com","departmentId":1}'
# 201, {"id":1,"name":"Nguyen Van An","email":"an@example.com","departmentId":1,"departmentName":"IT"}

curl -i -u user:user1234 "http://localhost:8080/api/employees?name=an&departmentId=1&page=0&size=10"

curl -i -u admin:admin123 -X PUT http://localhost:8080/api/employees/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"nguyen van an","email":"an.nguyen@example.com","departmentId":2}'

curl -i -u user:user1234 -X DELETE http://localhost:8080/api/employees/1
# 403, USER cannot delete

curl -i -u admin:admin123 -X DELETE http://localhost:8080/api/employees/1

curl -i -u user:user1234 http://localhost:8080/api/reports/employee-count

curl -i -u user:user1234 http://localhost:8080/api/reports/statistics
```

On Windows PowerShell, use `curl.exe` (not the `curl` alias) for `-u`, or:

```powershell
$token = (Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/login -ContentType 'application/json' -Body '{"username":"admin","password":"admin123"}').accessToken
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/employees -Headers @{ Authorization = "Bearer $token" } -ContentType 'application/json' -Body '{"name":"nguyen van an","email":"an@example.com","departmentId":1}'
```

## Errors

Every error of the REST API (`/api/...` and `/actuator/...`, including unknown URLs there) returns the same JSON shape. Errors of the web pages (and unknown web URLs such as `/employees/nope`) show the HTML error page instead.

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
| `400` | Validation failed (`fieldErrors`), unknown department, broken JSON, a value of the wrong type (e.g. `/api/employees/abc`), or `page` < 0 / `size` not in 1..100 |
| `401` | Not logged in, wrong username or password, bad or expired token |
| `403` | Logged in, but the role is not allowed (e.g. USER deletes an employee) |
| `404` | Employee not found, or no endpoint at this URL |
| `405` | HTTP method not supported for this URL |
| `409` | Email already used by another employee (also when two requests save the same email at the same time) |
| `415` | `Content-Type` is not `application/json` |
| `500` | Unexpected error (fixed message; details are only in the app log as `ERROR`, see [Logs](#logs)) |

```bash
curl -i -u admin:admin123 -X POST http://localhost:8080/api/employees \
  -H "Content-Type: application/json" \
  -d '{"name":"","email":"not-an-email","departmentId":1}'
# 400, fieldErrors: {"name":"Name must not be blank","email":"Email is not valid"}

curl -i -u user:user1234 http://localhost:8080/api/employees/999
# 404, "message":"Employee not found with id 999"

curl -i http://localhost:8080/api/employees
# 401, not logged in
```

## Tests

`./mvnw test` uses an in-memory H2 database, so it doesn't need MySQL. `src/test/resources/config/application.yml` overrides only what differs from the main `application.yml`: profile `test` (so `dev` / `prod` are not loaded), the H2 datasource with `ddl-auto: create-drop`, and the passwords of the default accounts used by the security tests.

## Stop

```bash
docker compose down      # stop; the data stays in the db-data volume
docker compose down -v   # stop and delete all data
```
