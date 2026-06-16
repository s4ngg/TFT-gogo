# TFT-gogo AWS MVP 구축 체크리스트

목표: 2026-06-17까지 서울 리전 `ap-northeast-2`에 백엔드 배포가 가능한 MVP 인프라를 만든다.

이 파일은 체크박스 진행용 요약이다. 콘솔 앞에서 빠르게 볼 순서는 `docs/team-share/aws-infra-console-quickstart.md`, 상세 설명은 `docs/team-share/aws-infra-mvp-runbook.md`를 기준으로 한다.

## 0. 팀에 먼저 받아야 하는 값

- [ ] AWS Account ID
- [ ] 작업자 AWS IAM 권한 범위
- [ ] 팀 고정 IP/CIDR. 없으면 임시로 ALB HTTP 80만 `0.0.0.0/0` 가능
- [ ] MySQL master password
- [ ] JWT secret. 32바이트 이상 랜덤값, 다른 환경과 재사용 금지
- [ ] Riot API key
- [ ] Admin secret token
- [ ] 프론트엔드 접속 origin. 도메인 전이면 로컬 검증용 origin만 사용
- [ ] ai-server를 내일 같이 띄울지 여부
- [ ] PostgreSQL을 내일 만들지, ai-server 준비 후 만들지 여부

## 1. VPC

- [ ] Region: `ap-northeast-2`
- [ ] VPC name: `tftgogo`
- [ ] IPv4 CIDR: `10.0.0.0/16`
- [ ] AZ: 2개
- [ ] Public subnet: 2개
- [ ] Private subnet: 2개
- [ ] NAT Gateway: None
- [ ] VPC endpoints: None
- [ ] DNS hostnames: Enable
- [ ] DNS resolution: Enable

권장 subnet:

| Name | CIDR | 용도 |
| --- | --- | --- |
| `tftgogo-subnet-public-apne2a` | `10.0.0.0/24` | ALB, MVP ECS |
| `tftgogo-subnet-public-apne2c` | `10.0.1.0/24` | ALB, MVP ECS |
| `tftgogo-subnet-private-apne2a` | `10.0.10.0/24` | RDS, Redis |
| `tftgogo-subnet-private-apne2c` | `10.0.11.0/24` | RDS, Redis |

주의: NAT Gateway와 VPC endpoint가 없으므로 ECS Fargate를 private subnet에 넣으면 ECR pull과 CloudWatch Logs 전송이 막힐 수 있다. 내일 MVP는 ECS를 public subnet + public IP로 배치하고, inbound는 ALB SG에서만 받는다.

## 2. 보안그룹

보안그룹은 먼저 전부 만든 뒤 SG-to-SG 규칙으로 연결한다.

| SG 이름 | Inbound | Source | Outbound |
| --- | --- | --- | --- |
| `tftgogo-alb-sg` | TCP 80 | 팀 고정 IP/CIDR | backend ECS 8080 |
| `tftgogo-backend-ecs-sg` | TCP 8080 | `tftgogo-alb-sg` | MySQL 3306, Redis 6379, HTTPS 443 |
| `tftgogo-ai-ecs-sg` | TCP 8000 | `tftgogo-backend-ecs-sg` | PostgreSQL 5432, HTTPS 443 |
| `tftgogo-mysql-sg` | TCP 3306 | `tftgogo-backend-ecs-sg` | 기본값 |
| `tftgogo-postgres-sg` | TCP 5432 | `tftgogo-ai-ecs-sg` | 기본값 |
| `tftgogo-redis-sg` | TCP 6379 | `tftgogo-backend-ecs-sg` | 기본값 |

내일 ai-server를 배포하지 않으면:

- [ ] `tftgogo-ai-ecs-sg`는 생성만 하거나 보류
- [ ] PostgreSQL RDS 생성 보류 가능
- [ ] backend ECS outbound 8000 규칙은 만들지 않음

절대 열지 않는 포트:

- [ ] SSH 22
- [ ] MySQL 3306 to internet
- [ ] PostgreSQL 5432 to internet
- [ ] Redis 6379 to internet
- [ ] Backend 8080 to internet

## 3. RDS

### MySQL

- [ ] DB subnet group: `tftgogo-db-subnet-group`, private subnet 2개
- [ ] Engine: MySQL 8.x
- [ ] Identifier: `tftgogo-mysql`
- [ ] Initial DB name: `tftgogo`
- [ ] Username: `tftuser`
- [ ] Public access: No
- [ ] Security group: `tftgogo-mysql-sg`
- [ ] Backup retention: 개발 단계 1~3일
- [ ] Deletion protection: 팀 공유 DB면 On, 내일 임시 검증이면 Off 가능

Schema 적용:

- [ ] `backend/src/main/resources/db/local-smoke/01_schema.sql` 적용 여부 확인
- [ ] `backend/src/main/resources/db/migration/V4__*.sql`부터 `V9__*.sql`까지 필요한 migration 순서 확인
- [ ] `02_seed.sql`은 공유 DB에 넣어도 되는 데이터인지 팀 확인 후 선택

Private RDS라 로컬 PC에서 바로 접속할 수 없다. 내일은 임시 bastion EC2를 만들고 팀 고정 IP에서만 SSH를 열어 schema 적용 후 삭제하거나, one-off ECS task로 적용한다.

### PostgreSQL

- [ ] ai-server 내일 배포 여부 확인
- [ ] 배포한다면 `tftgogo-postgres`, DB name `tftgogo_ai`, private subnet, `tftgogo-postgres-sg`
- [ ] pgvector 필요 시 `CREATE EXTENSION vector;` 가능 여부 확인
- [ ] ai-server가 없으면 비용 절감을 위해 보류 가능

## 4. Redis

- [ ] ElastiCache Redis OSS
- [ ] Name: `tftgogo-redis`
- [ ] Subnet group: `tftgogo-redis-subnet-group`, private subnet 2개
- [ ] Security group: `tftgogo-redis-sg`
- [ ] Cluster mode: Disabled
- [ ] Node type: 개발용 최소 타입

## 5. ECR

- [ ] Repository: `tftgogo-backend`
- [ ] Visibility: Private
- [ ] Scan on push: Enable
- [ ] Image tag: `manual-YYYYMMDD` 형식으로 push

PowerShell 예시:

```powershell
$env:AWS_REGION = "ap-northeast-2"
$env:AWS_ACCOUNT_ID = "<account-id>"
$env:ECR_BACKEND_REPO = "$env:AWS_ACCOUNT_ID.dkr.ecr.$env:AWS_REGION.amazonaws.com/tftgogo-backend"

aws ecr get-login-password --region $env:AWS_REGION |
  docker login --username AWS --password-stdin "$env:AWS_ACCOUNT_ID.dkr.ecr.$env:AWS_REGION.amazonaws.com"

docker build -t tftgogo-backend:manual-YYYYMMDD ./backend
docker tag tftgogo-backend:manual-YYYYMMDD "$env:ECR_BACKEND_REPO:manual-YYYYMMDD"
docker push "$env:ECR_BACKEND_REPO:manual-YYYYMMDD"
```

## 6. Secret 저장

SSM Parameter Store `SecureString`으로 만든다. ECS task definition의 plain environment에 secret을 넣지 않는다.

| Parameter | ECS env name |
| --- | --- |
| `/tftgogo/backend/mysql-password` | `SPRING_DATASOURCE_PASSWORD` |
| `/tftgogo/backend/jwt-secret` | `JWT_SECRET` |
| `/tftgogo/backend/riot-api-key` | `RIOT_API_KEY` |
| `/tftgogo/backend/admin-secret-token` | `ADMIN_SECRET_TOKEN` |

OAuth를 내일 켜는 경우에만 추가:

| Parameter | ECS env name |
| --- | --- |
| `/tftgogo/backend/google-client-id` | `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID` |
| `/tftgogo/backend/google-client-secret` | `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET` |
| `/tftgogo/backend/kakao-client-id` | `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KAKAO_CLIENT_ID` |
| `/tftgogo/backend/kakao-client-secret` | `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KAKAO_CLIENT_SECRET` |
| `/tftgogo/backend/naver-client-id` | `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_NAVER_CLIENT_ID` |
| `/tftgogo/backend/naver-client-secret` | `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_NAVER_CLIENT_SECRET` |

## 7. IAM 역할

### `tftgogo-ecs-task-execution-role`

용도: ECS가 ECR image pull, CloudWatch Logs 전송, SSM secret read를 하도록 허용한다.

- [ ] Trust entity: `ecs-tasks.amazonaws.com`
- [ ] Managed policy: `AmazonECSTaskExecutionRolePolicy`
- [ ] Inline policy: SSM parameter read

Inline policy 예시:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ssm:GetParameter",
        "ssm:GetParameters"
      ],
      "Resource": "arn:aws:ssm:ap-northeast-2:<account-id>:parameter/tftgogo/backend/*"
    }
  ]
}
```

SSM SecureString을 customer managed KMS key로 암호화했다면 `kms:Decrypt` 권한도 추가한다.

### `tftgogo-backend-task-role`

용도: 애플리케이션 코드가 AWS API를 호출할 때 사용하는 역할이다.

- [ ] 현재 backend가 AWS API를 직접 호출하지 않으면 권한 없이 생성
- [ ] 나중에 S3, Bedrock, Secrets Manager 등이 실제로 필요할 때 최소 권한만 추가

### `tftgogo-github-deploy-role`

내일 콘솔 수동 배포면 보류한다. GitHub Actions 배포 자동화를 바로 붙일 때만 만든다.

- [ ] OIDC trust는 `repo:s4ngg/TFT-gogo:ref:refs/heads/develop`처럼 대상 branch 제한
- [ ] `iam:PassRole` 대상은 ECS role 2개로 제한
- [ ] ECR push, ECS register/update/describe 권한만 부여

## 8. CloudWatch Logs

- [ ] Log group: `/ecs/tftgogo-backend`
- [ ] Retention: 7~14일

## 9. ECS

### Cluster

- [ ] Cluster name: `tftgogo-cluster`
- [ ] Infrastructure: Fargate

### Task definition

- [ ] Family: `tftgogo-backend`
- [ ] CPU/Memory: 0.5 vCPU / 1GB부터 시작
- [ ] Task execution role: `tftgogo-ecs-task-execution-role`
- [ ] Task role: `tftgogo-backend-task-role`
- [ ] Container name: `backend`
- [ ] Image URI: ECR `tftgogo-backend:manual-YYYYMMDD`
- [ ] Container port: 8080
- [ ] Log group: `/ecs/tftgogo-backend`

Plain environment:

```text
SPRING_PROFILES_ACTIVE=docker
SPRING_DATASOURCE_URL=jdbc:mysql://<mysql-endpoint>:3306/tftgogo?sslMode=REQUIRED&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
SPRING_DATASOURCE_USERNAME=tftuser
SPRING_JPA_HIBERNATE_DDL_AUTO=none
SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT=org.hibernate.dialect.MySQL8Dialect
SPRING_DATA_REDIS_HOST=<redis-primary-endpoint>
SPRING_DATA_REDIS_PORT=6379
APP_META_DECK_STARTUP_AGGREGATE=false
APP_GUIDE_CDRAGON_STARTUP_IMPORT=false
JWT_ACCESS_TOKEN_EXPIRATION_MILLIS=3600000
APP_OAUTH2_AUTHORIZED_REDIRECT_URI=http://localhost:5173/oauth/callback
APP_OAUTH2_LOGIN_FAILURE_REDIRECT_URI=http://localhost:5173/login
APP_CORS_ALLOWED_ORIGINS_0=http://localhost:5173
```

Secrets:

```text
SPRING_DATASOURCE_PASSWORD=/tftgogo/backend/mysql-password
JWT_SECRET=/tftgogo/backend/jwt-secret
RIOT_API_KEY=/tftgogo/backend/riot-api-key
ADMIN_SECRET_TOKEN=/tftgogo/backend/admin-secret-token
```

도메인/HTTPS가 생기면 `APP_OAUTH2_*`, `APP_CORS_ALLOWED_ORIGINS_0`를 실제 프론트엔드 HTTPS origin으로 바꾼다.

### Service

- [ ] Service name: `tftgogo-backend-service`
- [ ] Desired tasks: 1
- [ ] Subnets: public subnet 2개
- [ ] Public IP: On
- [ ] Security group: `tftgogo-backend-ecs-sg`
- [ ] Load balancer: `tftgogo-alb`
- [ ] Target group: `tftgogo-backend-tg`
- [ ] Deployment circuit breaker: Enable, rollback Enable

## 10. ALB

- [ ] Name: `tftgogo-alb`
- [ ] Scheme: Internet-facing
- [ ] Subnets: public subnet 2개
- [ ] Security group: `tftgogo-alb-sg`
- [ ] Listener: HTTP 80
- [ ] Target group type: IP
- [ ] Target group name: `tftgogo-backend-tg`
- [ ] Target protocol/port: HTTP 8080
- [ ] Health check path: 임시 `/v3/api-docs`
- [ ] Success code: `200`

`/v3/api-docs`는 Swagger 문서 endpoint라 임시 health check다. 운영 전에는 `/health` 또는 actuator health를 추가한 뒤 health check path를 바꾼다.

## 11. 완료 확인

- [ ] ALB target group target이 healthy
- [ ] ALB DNS로 `GET /v3/api-docs`가 200
- [ ] CloudWatch Logs에 backend startup log 출력
- [ ] RDS 3306이 인터넷에 열려 있지 않음
- [ ] Redis 6379가 인터넷에 열려 있지 않음
- [ ] Backend task public IP가 있어도 inbound source는 ALB SG뿐임
- [ ] Secret 값이 task definition 화면의 plain environment에 노출되지 않음
- [ ] 도메인/HTTPS 전 OAuth 실사용 QA는 미완료로 표시

## 12. 내일 이후 작업

- [ ] `/health` endpoint 또는 actuator health 추가
- [ ] schema migration 전략을 Flyway 또는 Liquibase로 정리
- [ ] 도메인 확보 후 Route53 + ACM + HTTPS 적용
- [ ] OAuth provider callback URI를 HTTPS 도메인으로 변경
- [ ] ECS를 private subnet으로 이동
- [ ] NAT Gateway 또는 ECR/CloudWatch/SSM VPC endpoints 추가
- [ ] GitHub Actions ECR push + ECS deploy 자동화
