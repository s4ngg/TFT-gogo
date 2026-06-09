# Guide/PatchNotes Local Smoke Data

This folder defines the manual local DB baseline for Guide and PatchNotes smoke testing.

## Policy

- Schema management is manual SQL for now.
- `spring.jpa.hibernate.ddl-auto` is `none`.
- Flyway/Liquibase is not configured in `backend/build.gradle`.
- The public Guide/PatchNotes APIs do not require `RIOT_API_KEY`.

## Start Infrastructure

```powershell
docker compose up -d mysql redis
```

## Apply Schema And Seed

With a host MySQL client:

```powershell
mysql --default-character-set=utf8mb4 -h 127.0.0.1 -P 3306 -u tftuser -ptftpass tftgogo < backend/src/main/resources/db/local-smoke/01_schema.sql
mysql --default-character-set=utf8mb4 -h 127.0.0.1 -P 3306 -u tftuser -ptftpass tftgogo < backend/src/main/resources/db/local-smoke/02_seed.sql
```

Or through the MySQL container:

```powershell
Get-Content -Encoding UTF8 backend/src/main/resources/db/local-smoke/01_schema.sql | docker exec -i tftgogo-mysql mysql --default-character-set=utf8mb4 -utftuser -ptftpass tftgogo
Get-Content -Encoding UTF8 backend/src/main/resources/db/local-smoke/02_seed.sql | docker exec -i tftgogo-mysql mysql --default-character-set=utf8mb4 -utftuser -ptftpass tftgogo
```

## Run Backend

```powershell
cd backend
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

For Docker Compose backend runs, `docker-compose.yml` passes datasource, Redis, admin token, JWT, and Riot placeholder values through environment variables.

## Smoke Checks

```powershell
curl http://localhost:8080/api/guide
curl "http://localhost:8080/api/guide/traits?page=1&pageSize=10"
curl "http://localhost:8080/api/guide/items?page=1&pageSize=10"
curl "http://localhost:8080/api/guide/augments?page=1&pageSize=10"
curl "http://localhost:8080/api/guide/champions?page=1&pageSize=10&cost=4"

curl http://localhost:8080/api/patch-notes
curl "http://localhost:8080/api/patch-notes/17.3/changes?page=1&pageSize=10"
curl "http://localhost:8080/api/patch-notes/17.3/changes?category=CHAMPION&impact=HIGH"

curl -H "X-Admin-Token: local-admin-token" http://localhost:8080/api/admin/guides
curl -H "X-Admin-Token: local-admin-token" http://localhost:8080/api/admin/patch-notes
```

Expected result: Guide and PatchNotes public APIs return seeded `17.3` data without frontend fallback data.
