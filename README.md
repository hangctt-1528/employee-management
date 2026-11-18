# Employee Management

Spring Boot 4 practice project (Java 25): REST API for employees and departments, stored in MySQL with Spring Data JPA.

## Prerequisite

- Docker (with Docker Compose). No local Java, Maven or database is needed.

## Run

```bash
docker compose up --build
```

This starts two containers: `db` (MySQL 8.4) and `app`. The app waits until MySQL is healthy.
The first build downloads Maven dependencies and the MySQL image, so it takes a few minutes.

## URL and port

- App: http://localhost:8080
- Hello API: http://localhost:8080/hello returns `Hello, Employee Management!`
- MySQL: `localhost:3307` (inside Docker: `db:3306`), database `employee_db`, user `employee` / password `employee` (development only)
- Default accounts: none yet.

Sample departments `IT`, `HR`, `Sales` are created at the first start.

## Environment variables (app)

| Variable | Value in docker-compose.yml | Default when not set |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://db:3306/employee_db?...` | `jdbc:mysql://localhost:3306/employee_db` |
| `SPRING_DATASOURCE_USERNAME` | `employee` | `employee` |
| `SPRING_DATASOURCE_PASSWORD` | `employee` | `employee` |

To run the app on your machine (`./mvnw spring-boot:run`) against the MySQL started by compose (`docker compose up -d db`), point it to port 3307:

```bash
SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3307/employee_db?allowPublicKeyRetrieval=true&useSSL=false" ./mvnw spring-boot:run
```

Emails are stored in lower case, so `An@Example.com` and `an@example.com` count as the same email.

## API

| Method | Path | Result |
|---|---|---|
| GET | `/api/departments` | List of departments (use their `id` as `departmentId`) |
| GET | `/api/employees` | List of employees |
| GET | `/api/employees?name=an&departmentId=1` | Search by name (contains, ignores case) and/or department; both given = AND |
| GET | `/api/employees/{id}` | One employee, or `404` |
| POST | `/api/employees` | Add: `201` + `Location`; `400` if name/email/departmentId is missing or the department doesn't exist; `409` if the email is already used |
| PUT | `/api/employees/{id}` | Update: `200`; `400` as above; `404` if no employee; `409` if another employee has the email |
| DELETE | `/api/employees/{id}` | Delete: `204`, or `404` |

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
```

On Windows PowerShell:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/employees -ContentType 'application/json' -Body '{"name":"nguyen van an","email":"an@example.com","departmentId":1}'
```

## Tests

`./mvnw test` uses an in-memory H2 database, so it doesn't need MySQL.

## Stop

```bash
docker compose down      # stop; the data stays in the db-data volume
docker compose down -v   # stop and delete all data
```
