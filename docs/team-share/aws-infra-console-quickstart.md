# TFT-gogo AWS 콘솔 빠른 시작

목표: 2026-06-17까지 도메인 없이 서울 리전 `ap-northeast-2`에 백엔드 1개를 ALB 뒤에 띄운다.

상세 설명은 `aws-infra-mvp-runbook.md`, 체크박스 진행은 `aws-infra-mvp-checklist.md`를 따른다. 이 문서는 콘솔 앞에서 순서와 포트, 역할만 빠르게 확인하는 용도다.

## 0. 먼저 정할 것

- AWS Account ID
- 작업자 IAM 권한
- 팀 고정 IP/CIDR
- MySQL master password
- JWT secret, Riot API key, admin secret token
- ai-server와 PostgreSQL을 내일 같이 만들지 여부

FastAPI 코드가 아직 없으면 내일은 PostgreSQL과 AI ECS를 보류하고, Spring Boot + MySQL + Redis + ALB까지만 완성한다.

## 1. 생성 순서

1. VPC
2. Security Group
3. RDS MySQL
4. ElastiCache Redis
5. ECR
6. SSM Parameter Store secrets
7. IAM roles
8. CloudWatch log group
9. ECS cluster, task definition, service
10. ALB, target group, listener
11. Swagger `/v3/api-docs` 확인

주의: RDS, ElastiCache, ECS, ALB는 비용이 발생한다. 팀 확인 없이 큰 인스턴스나 NAT Gateway를 만들지 않는다.

## 2. VPC

| 항목 | 값 |
| --- | --- |
| Region | `ap-northeast-2` |
| Name | `tftgogo` |
| IPv4 CIDR | `10.0.0.0/16` |
| AZ | 2개 |
| Public subnet | 2개 |
| Private subnet | 2개 |
| NAT Gateway | None |
| VPC endpoints | None |

권장 subnet:

| Name | CIDR | 용도 |
| --- | --- | --- |
| `tftgogo-subnet-public-apne2a` | `10.0.0.0/24` | ALB, MVP ECS |
| `tftgogo-subnet-public-apne2c` | `10.0.1.0/24` | ALB, MVP ECS |
| `tftgogo-subnet-private-apne2a` | `10.0.10.0/24` | RDS, Redis |
| `tftgogo-subnet-private-apne2c` | `10.0.11.0/24` | RDS, Redis |

현재 조건은 NAT Gateway와 VPC endpoint가 모두 없으므로 ECS Fargate는 MVP에서 public subnet + public IP로 시작한다. 대신 backend inbound는 ALB security group에서만 받는다.

## 3. 보안그룹 포트

보안그룹을 먼저 모두 만든 뒤, IP보다 SG-to-SG 규칙으로 연결한다.

| Security Group | Inbound | Source | Outbound |
| --- | --- | --- | --- |
| `tftgogo-alb-sg` | TCP 80 | 팀 고정 IP/CIDR | TCP 8080 to `tftgogo-backend-ecs-sg` |
| `tftgogo-backend-ecs-sg` | TCP 8080 | `tftgogo-alb-sg` | TCP 3306 to MySQL, TCP 6379 to Redis, TCP 443 to internet |
| `tftgogo-mysql-sg` | TCP 3306 | `tftgogo-backend-ecs-sg` | 기본값 유지 |
| `tftgogo-redis-sg` | TCP 6379 | `tftgogo-backend-ecs-sg` | 기본값 유지 |
| `tftgogo-ai-ecs-sg` | TCP 8000 | `tftgogo-backend-ecs-sg` | PostgreSQL 5432, HTTPS 443 |
| `tftgogo-postgres-sg` | TCP 5432 | `tftgogo-ai-ecs-sg` | 기본값 유지 |

ai-server를 내일 배포하지 않으면 `tftgogo-ai-ecs-sg`, `tftgogo-postgres-sg`, PostgreSQL RDS는 보류한다.

절대 열지 않는 것:

- SSH 22 to internet
- Backend 8080 to internet
- MySQL 3306 to internet
- PostgreSQL 5432 to internet
- Redis 6379 to internet

팀 고정 IP가 아직 없으면 임시로 ALB 80만 `0.0.0.0/0`로 열 수 있지만, 세팅 확인 후 바로 팀 IP/CIDR로 좁힌다.

## 4. RDS와 Redis

MySQL:

- Engine: MySQL 8.x
- DB identifier: `tftgogo-mysql`
- Initial DB name: `tftgogo`
- Username: `tftuser`
- Subnet group: private subnet 2개
- Public access: No
- Security group: `tftgogo-mysql-sg`

Redis:

- Service: ElastiCache Redis OSS
- Name: `tftgogo-redis`
- Subnet group: private subnet 2개
- Security group: `tftgogo-redis-sg`
- Cluster mode: Disabled

RDS가 private subnet에 있으므로 로컬 PC에서 바로 접속할 수 없다. schema 적용은 임시 bastion EC2를 팀 IP에서만 SSH 허용 후 삭제하거나, one-off ECS task로 처리한다.

## 5. ECR

- Repository: `tftgogo-backend`
- Visibility: Private
- Scan on push: Enable

PowerShell:

```powershell
$env:AWS_REGION = "ap-northeast-2"
$env:AWS_ACCOUNT_ID = "<account-id>"
$env:ECR_BACKEND_REPO = "$env:AWS_ACCOUNT_ID.dkr.ecr.$env:AWS_REGION.amazonaws.com/tftgogo-backend"

aws ecr get-login-password --region $env:AWS_REGION |
  docker login --username AWS --password-stdin "$env:AWS_ACCOUNT_ID.dkr.ecr.$env:AWS_REGION.amazonaws.com"

docker build -t tftgogo-backend:manual-20260617 ./backend
docker tag tftgogo-backend:manual-20260617 "$env:ECR_BACKEND_REPO:manual-20260617"
docker push "$env:ECR_BACKEND_REPO:manual-20260617"
```

## 6. Secrets

SSM Parameter Store `SecureString`으로 만든다. ECS task definition의 plain environment에 secret 값을 직접 넣지 않는다.

| Parameter | ECS env name |
| --- | --- |
| `/tftgogo/backend/mysql-password` | `SPRING_DATASOURCE_PASSWORD` |
| `/tftgogo/backend/jwt-secret` | `JWT_SECRET` |
| `/tftgogo/backend/riot-api-key` | `RIOT_API_KEY` |
| `/tftgogo/backend/admin-secret-token` | `ADMIN_SECRET_TOKEN` |

OAuth를 HTTPS 도메인 없이 내일 검증하지 않으면 Google/Kakao/Naver secret은 보류한다.

## 7. IAM 역할

`tftgogo-ecs-task-execution-role`

- 용도: ECS가 ECR image pull, CloudWatch Logs 전송, SSM secret read를 하도록 허용
- Trust entity: `ecs-tasks.amazonaws.com`
- Managed policy: `AmazonECSTaskExecutionRolePolicy`
- Inline policy: `ssm:GetParameter`, `ssm:GetParameters` on `arn:aws:ssm:ap-northeast-2:<account-id>:parameter/tftgogo/backend/*`
- Customer managed KMS key를 쓰면 `kms:Decrypt`도 추가

`tftgogo-backend-task-role`

- 용도: Spring Boot 애플리케이션 코드가 AWS API를 직접 호출할 때 사용
- 현재 backend가 AWS API를 직접 호출하지 않으면 권한 없이 생성
- 나중에 S3, Bedrock, Secrets Manager 등이 필요할 때만 최소 권한 추가

`tftgogo-github-deploy-role`

- 내일 콘솔 수동 배포면 보류
- GitHub Actions 배포 자동화 때만 OIDC로 생성
- `iam:PassRole` 대상은 ECS role 2개로 제한

## 8. ECS task definition

| 항목 | 값 |
| --- | --- |
| Family | `tftgogo-backend` |
| Launch type | Fargate |
| CPU/Memory | 0.5 vCPU / 1GB부터 시작 |
| Task execution role | `tftgogo-ecs-task-execution-role` |
| Task role | `tftgogo-backend-task-role` |
| Container name | `backend` |
| Image | ECR `tftgogo-backend:manual-20260617` |
| Container port | 8080 |
| Log group | `/ecs/tftgogo-backend` |

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
```

Secrets:

```text
SPRING_DATASOURCE_PASSWORD=/tftgogo/backend/mysql-password
JWT_SECRET=/tftgogo/backend/jwt-secret
RIOT_API_KEY=/tftgogo/backend/riot-api-key
ADMIN_SECRET_TOKEN=/tftgogo/backend/admin-secret-token
```

## 9. ECS service

- Cluster: `tftgogo-cluster`
- Service: `tftgogo-backend-service`
- Desired tasks: 1
- Subnets: public subnet 2개
- Public IP: On
- Security group: `tftgogo-backend-ecs-sg`
- Load balancer: `tftgogo-alb`
- Target group: `tftgogo-backend-tg`
- Deployment circuit breaker: Enable, rollback Enable

## 10. ALB

- Name: `tftgogo-alb`
- Scheme: Internet-facing
- Subnets: public subnet 2개
- Security group: `tftgogo-alb-sg`
- Listener: HTTP 80
- Target group type: IP
- Target protocol/port: HTTP 8080
- Health check path: 임시 `/v3/api-docs`
- Success code: `200`

`/v3/api-docs`는 임시 확인용이다. 운영 전에는 `/health` 또는 actuator health endpoint를 추가하고 health check path를 바꾼다.

## 11. 완료 기준

- ALB target group target이 healthy
- ALB DNS로 `GET /v3/api-docs`가 200
- CloudWatch Logs에 Spring Boot startup log 출력
- RDS public access가 No
- RDS/Redis inbound source가 internet이 아니라 ECS SG
- Backend ECS inbound source가 internet이 아니라 ALB SG
- Secret 값이 task definition plain environment에 노출되지 않음
- 도메인/HTTPS 전 소셜 로그인 QA는 미완료로 표시

## 12. 도메인 받은 후

1. ACM에서 `ap-northeast-2` 인증서 발급
2. Route53 hosted zone 또는 외부 DNS에 검증 레코드 추가
3. ALB HTTPS 443 listener 추가
4. HTTP 80은 HTTPS redirect
5. OAuth provider callback URI를 HTTPS 도메인으로 변경
6. CORS와 OAuth redirect env를 HTTPS 프론트 origin으로 변경
7. ECS를 private subnet으로 이전하고 NAT Gateway 또는 VPC endpoints 추가 검토
