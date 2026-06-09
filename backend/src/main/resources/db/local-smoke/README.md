# Guide/PatchNotes Local Smoke Data

This folder defines the manual local DB baseline for Guide and PatchNotes smoke testing.

## Policy

- Schema management is manual SQL for now.
- `spring.jpa.hibernate.ddl-auto` is `none`.
- Flyway/Liquibase is not configured in `backend/build.gradle`.
- The public Guide/PatchNotes APIs do not require `RIOT_API_KEY`.
- `ai-server/.env` is optional for this smoke path; only MySQL, Redis, and backend are required.

## Start Infrastructure

Docker Compose maps MySQL to host port `3307` and backend to host port `8081` by default so it does not collide with a local MySQL/STS backend already using `3306`/`8080`.

```powershell
docker compose up -d mysql redis
```

## Apply Schema And Seed

With a host MySQL client:

```powershell
mysql --default-character-set=utf8mb4 -h 127.0.0.1 -P 3307 -u tftuser -ptftpass tftgogo < backend/src/main/resources/db/local-smoke/01_schema.sql
mysql --default-character-set=utf8mb4 -h 127.0.0.1 -P 3307 -u tftuser -ptftpass tftgogo < backend/src/main/resources/db/local-smoke/02_seed.sql
```

Or through the MySQL container:

```powershell
Get-Content -Encoding UTF8 backend/src/main/resources/db/local-smoke/01_schema.sql | docker exec -i tftgogo-mysql mysql --default-character-set=utf8mb4 -utftuser -ptftpass tftgogo
Get-Content -Encoding UTF8 backend/src/main/resources/db/local-smoke/02_seed.sql | docker exec -i tftgogo-mysql mysql --default-character-set=utf8mb4 -utftuser -ptftpass tftgogo
```

## Run Backend

Docker Compose is the recommended local smoke path because `application*.yml` files are local-only and ignored by git. The backend service receives datasource, Redis, admin token, JWT, and Riot placeholder values from `docker-compose.yml`.

```powershell
docker compose up --build backend
```

Optional STS/Gradle path: run with the `local` profile only after preparing a local `backend/src/main/resources/application-local.yml` that matches the same MySQL, Redis, admin token, JWT, and Riot placeholder values.

```powershell
cd backend
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

## Smoke Checks

```powershell
curl.exe http://localhost:8081/api/guide
curl.exe "http://localhost:8081/api/guide/traits?page=1&pageSize=10"
curl.exe "http://localhost:8081/api/guide/items?page=1&pageSize=10"
curl.exe "http://localhost:8081/api/guide/augments?page=1&pageSize=10"
curl.exe "http://localhost:8081/api/guide/champions?page=1&pageSize=10&cost=4"

curl.exe http://localhost:8081/api/patch-notes
curl.exe "http://localhost:8081/api/patch-notes/17.3/changes?page=1&pageSize=10"
curl.exe "http://localhost:8081/api/patch-notes/17.3/changes?category=CHAMPION&impact=HIGH"

curl.exe -H "X-Admin-Token: local-admin-token" http://localhost:8081/api/admin/guides
curl.exe -H "X-Admin-Token: local-admin-token" http://localhost:8081/api/admin/patch-notes
```

Expected result: Guide and PatchNotes public APIs return seeded `17.3` data without frontend fallback data.
