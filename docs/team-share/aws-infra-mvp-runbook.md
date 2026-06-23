# TFT-gogo AWS MVP 인프라 구축 런북

목표: 도메인이 아직 없는 상태에서 서울 리전(ap-northeast-2)에 TFT-gogo 백엔드를 먼저 배포할 수 있는 최소 AWS 인프라를 만든다.

내일 콘솔 작업만 빠르게 진행해야 하면 먼저 [AWS 콘솔 빠른 시작](./aws-infra-console-quickstart.md)을 보고, 체크박스 관리는 [AWS MVP 구축 체크리스트](./aws-infra-mvp-checklist.md)를 사용한다. 막히는 항목만 이 런북의 상세 섹션을 확인한다.

## 내일 작업 요약

1. VPC `tftgogo`를 `10.0.0.0/16`, public 2개, private 2개, NAT Gateway 없음, VPC endpoint 없음으로 만든다.
2. 필요한 Security Group을 먼저 만든다.
3. MySQL RDS, Redis를 private subnet에 만든다. PostgreSQL은 ai-server가 내일 범위가 아니면 보류할 수 있다.
4. ECR `tftgogo-backend`에 backend 이미지를 push한다.
5. SSM Parameter Store에 DB 비밀번호, JWT secret, Riot key, admin token을 `SecureString`으로 저장한다.
6. ECS task execution role과 backend task role을 만든다.
7. CloudWatch log group `/ecs/tftgogo-backend`를 만든다.
8. ECS Fargate task definition과 service를 만든다. NAT가 없으므로 MVP에서는 public subnet + public IP를 사용한다.
9. ALB HTTP 80을 팀 고정 IP/CIDR에만 열고, target group을 ECS service에 연결한다.
10. ALB target health와 `/v3/api-docs` 응답을 확인한다.

내일 절대 하지 않는 것:

- RDS/Redis를 public access로 열지 않는다.
- MySQL 3306, PostgreSQL 5432, Redis 6379를 개인 IP나 `0.0.0.0/0`에 열지 않는다.
- Fargate task에 SSH 22를 열지 않는다.
- secret 값을 ECS plain environment에 붙여넣지 않는다.
- 도메인/HTTPS 전에는 실사용 OAuth 로그인 QA를 완료됐다고 보지 않는다.

## 0. 중요한 결정

- VPC는 요청값 그대로 만든다.
- RDS와 Redis는 private subnet에 둔다.
- ECS Fargate는 현재 1차 MVP에서는 public subnet에 둔다.

이유:

- 현재 조건이 `NAT Gateway: None`, `VPC endpoints: None`이다.
- ECS Fargate task를 private subnet에 두면 ECR 이미지 pull과 CloudWatch Logs 전송을 위해 NAT Gateway 또는 ECR/CloudWatch 관련 VPC endpoint가 필요하다.
- 따라서 내일 배포 성공을 우선하면 ECS는 public subnet + public IP로 시작하고, 보안그룹으로 ALB에서만 8080 접근을 허용한다.
- 이후 운영 전환 시 ECS를 private subnet으로 옮기고 NAT Gateway 또는 VPC endpoint를 추가한다.
- 도메인과 ACM 적용 전에는 팀 내부 검증용으로만 열고, OAuth/실사용 로그인/관리자 작업은 HTTPS 도메인 적용 후 검증한다.

참고:

- https://docs.aws.amazon.com/AmazonECS/latest/developerguide/fargate-task-networking.html
- https://docs.aws.amazon.com/AmazonECR/latest/userguide/vpc-endpoints.html
- https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/USER_VPC.WorkingWithRDSInstanceinaVPC.html

내일 바로 진행할 최소 순서:

1. VPC와 subnet을 먼저 만든다.
2. Security Group을 모두 만든다.
3. RDS MySQL과 Redis를 private subnet에 만든다.
4. RDS schema 적용 방법을 확정하고 schema를 적용한다.
5. ECR `tftgogo-backend`를 만들고 backend image를 push한다.
6. SSM Parameter Store `SecureString`에 secret 값을 넣는다.
7. IAM role 2개를 만든다.
8. CloudWatch log group을 만든다.
9. ECS cluster와 task definition을 만든다.
10. ALB와 target group/listener를 만든다.
11. ECS service를 target group에 연결해서 만든다.
12. ALB target health가 healthy인지 확인한다.

주의: 실제 생성은 RDS, ElastiCache, ALB, ECS에서 비용이 발생할 수 있으므로 팀 확인 후 진행한다.

## 1. VPC

AWS Console > VPC > Create VPC > VPC and more

| 항목 | 값 |
| --- | --- |
| Name tag auto-generation | `tftgogo` |
| IPv4 CIDR block | `10.0.0.0/16` |
| IPv6 CIDR block | No IPv6 CIDR block |
| Tenancy | Default |
| Number of Availability Zones | 2 |
| AZ 예시 | `ap-northeast-2a`, `ap-northeast-2c` |
| Number of public subnets | 2 |
| Number of private subnets | 2 |
| NAT gateways | None |
| VPC endpoints | None |
| DNS hostnames | Enable |
| DNS resolution | Enable |

권장 CIDR:

| Subnet | CIDR | 용도 |
| --- | --- | --- |
| `tftgogo-subnet-public-apne2a` | `10.0.0.0/24` | ALB, MVP ECS |
| `tftgogo-subnet-public-apne2c` | `10.0.1.0/24` | ALB, MVP ECS |
| `tftgogo-subnet-private-apne2a` | `10.0.10.0/24` | RDS, Redis |
| `tftgogo-subnet-private-apne2c` | `10.0.11.0/24` | RDS, Redis |

생성 후 확인:

- Public route table에 `0.0.0.0/0 -> Internet Gateway`가 있어야 한다.
- Private route table에는 기본적으로 local route만 있어야 한다.

## 2. Security Groups

Security group은 리소스 이름으로 참조한다. IP를 직접 열기보다 SG-to-SG 규칙을 우선 사용한다.

전체 규칙 요약:

| SG | Inbound | Source | Outbound |
| --- | --- | --- | --- |
| `tftgogo-alb-sg` | TCP 80 | 팀 고정 IP/CIDR | backend ECS 8080 |
| `tftgogo-backend-ecs-sg` | TCP 8080 | `tftgogo-alb-sg` | RDS/Redis + HTTPS, ai-server 배포 시 AI 8000 |
| `tftgogo-ai-ecs-sg` | TCP 8000 | `tftgogo-backend-ecs-sg` | PostgreSQL + HTTPS |
| `tftgogo-mysql-sg` | TCP 3306 | `tftgogo-backend-ecs-sg` | 기본값 |
| `tftgogo-postgres-sg` | TCP 5432 | `tftgogo-ai-ecs-sg` | 기본값 |
| `tftgogo-redis-sg` | TCP 6379 | `tftgogo-backend-ecs-sg` | 기본값 |

열지 않는 포트:

- SSH 22: Fargate에는 SSH 접속하지 않는다.
- MySQL 3306 / PostgreSQL 5432 / Redis 6379: 인터넷 또는 개인 IP에 직접 열지 않는다.
- Backend 8080: 인터넷에 직접 열지 않고 ALB SG에서만 받는다.

### 2.1 ALB

Name: `tftgogo-alb-sg`

Inbound:

| Type | Port | Source |
| --- | --- | --- |
| HTTP | 80 | 팀 고정 IP/CIDR |

Outbound:

| Type | Port | Destination |
| --- | --- | --- |
| Custom TCP | 8080 | `tftgogo-backend-ecs-sg` |

팀 고정 IP가 없고 임시 공개 검증이 꼭 필요하면 HTTP 80을 `0.0.0.0/0`으로 열 수는 있지만, 그 상태에서는 실제 사용자 로그인, OAuth callback, 관리자 토큰 작업, 운영 secret 검증을 하지 않는다.

도메인과 ACM 인증서 추가 후 HTTPS 443 listener를 `0.0.0.0/0`으로 열고, HTTP 80은 HTTPS redirect로 전환한다.

### 2.2 Backend ECS

Name: `tftgogo-backend-ecs-sg`

Inbound:

| Type | Port | Source |
| --- | --- | --- |
| Custom TCP | 8080 | `tftgogo-alb-sg` |

Outbound:

| Type | Port | Destination |
| --- | --- | --- |
| MySQL/Aurora | 3306 | `tftgogo-mysql-sg` |
| Custom TCP | 6379 | `tftgogo-redis-sg` |
| Custom TCP | 8000 | `tftgogo-ai-ecs-sg` |
| HTTPS | 443 | `0.0.0.0/0` |

MVP에서 ECS task는 public subnet에 배치하지만, inbound는 ALB security group에서만 허용한다. ECR pull과 CloudWatch Logs 전송은 task execution role과 public outbound HTTPS를 사용한다.

ai-server를 아직 배포하지 않으면 8000 outbound rule은 만들지 않는다.

### 2.3 AI ECS

Name: `tftgogo-ai-ecs-sg`

Inbound:

| Type | Port | Source |
| --- | --- | --- |
| Custom TCP | 8000 | `tftgogo-backend-ecs-sg` |

Outbound:

| Type | Port | Destination |
| --- | --- | --- |
| PostgreSQL | 5432 | `tftgogo-postgres-sg` |
| HTTPS | 443 | `0.0.0.0/0` |

ai-server 코드와 Dockerfile이 준비되기 전에는 SG만 만들어 두거나 이 단계 전체를 보류한다.

### 2.4 MySQL RDS

Name: `tftgogo-mysql-sg`

Inbound:

| Type | Port | Source |
| --- | --- | --- |
| MySQL/Aurora | 3306 | `tftgogo-backend-ecs-sg` |

Outbound: 기본값 유지

### 2.5 PostgreSQL RDS

Name: `tftgogo-postgres-sg`

Inbound:

| Type | Port | Source |
| --- | --- | --- |
| PostgreSQL | 5432 | `tftgogo-ai-ecs-sg` |

ai-server가 아직 없으면 SG와 inbound rule 생성, PostgreSQL RDS 생성을 모두 보류할 수 있다. 나중에 실제 호출 주체 SG가 확정된 뒤 추가한다.

### 2.6 Redis ElastiCache

Name: `tftgogo-redis-sg`

Inbound:

| Type | Port | Source |
| --- | --- | --- |
| Custom TCP | 6379 | `tftgogo-backend-ecs-sg` |

Outbound: 기본값 유지

## 3. RDS

### 3.1 DB subnet group

RDS > Subnet groups > Create DB subnet group

| 항목 | 값 |
| --- | --- |
| Name | `tftgogo-db-subnet-group` |
| VPC | `tftgogo` |
| Subnets | private subnet 2개 |

### 3.2 MySQL

RDS > Databases > Create database

| 항목 | 값 |
| --- | --- |
| Engine | MySQL |
| Version | MySQL 8.x |
| Template | Free tier 또는 Dev/Test |
| DB instance identifier | `tftgogo-mysql` |
| Master username | `tftuser` |
| Public access | No |
| VPC | `tftgogo` |
| DB subnet group | `tftgogo-db-subnet-group` |
| Security group | `tftgogo-mysql-sg` |
| Initial database name | `tftgogo` |
| Backup | 개발 단계에서는 1~3일 |
| Storage autoscaling | Enable 권장 |
| Deletion protection | 내일 검증용이면 Off, 팀 공유 DB면 On |

백엔드 환경변수 매핑:

```text
SPRING_DATASOURCE_URL=jdbc:mysql://<mysql-endpoint>:3306/tftgogo?sslMode=REQUIRED&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
SPRING_DATASOURCE_USERNAME=tftuser
SPRING_DATASOURCE_PASSWORD=<secret>
SPRING_JPA_HIBERNATE_DDL_AUTO=none
SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT=org.hibernate.dialect.MySQL8Dialect
```

### 3.3 MySQL schema 적용

현재 backend는 `SPRING_JPA_HIBERNATE_DDL_AUTO=none`이고 Flyway/Liquibase 의존성이 없다. RDS만 만들고 schema를 적용하지 않으면 서버가 떠도 실제 API가 테이블 없음 오류로 실패할 수 있다.

현재 repo에서 확인되는 수동 SQL:

| 경로 | 용도 |
| --- | --- |
| `backend/src/main/resources/db/local-smoke/01_schema.sql` | 로컬 스모크 기준 기본 schema |
| `backend/src/main/resources/db/local-smoke/02_seed.sql` | 로컬 스모크 seed |
| `backend/src/main/resources/db/migration/V4__*.sql` ~ `V9__*.sql` | 누적 수동 migration |

내일 선택지:

| 선택지 | 사용 시점 | 주의 |
| --- | --- | --- |
| 임시 bastion EC2 | 콘솔 중심으로 빠르게 확인할 때 | public subnet에 두고 SSH 22는 팀 고정 IP/CIDR만 허용, schema 적용 후 삭제 |
| one-off ECS task | EC2를 만들고 싶지 않을 때 | MySQL client가 포함된 임시 이미지 또는 migration 전용 이미지를 준비해야 함 |
| Flyway/Liquibase 추가 | 운영 배포를 안정화할 때 | 별도 backend PR로 의존성/마이그레이션 전략 확정 필요 |

RDS는 private subnet이고 public access가 `No`라서 로컬 PC나 CloudShell에서 직접 접속할 수 없다. 내일은 팀과 schema 기준을 먼저 맞춘 뒤, `01_schema.sql`과 필요한 `V*.sql`을 순서대로 적용한다. `02_seed.sql`은 운영/공유 DB에 넣어도 되는 데이터인지 확인한 뒤 선택한다.

### 3.4 PostgreSQL

RDS > Databases > Create database

| 항목 | 값 |
| --- | --- |
| Engine | PostgreSQL |
| Version | PostgreSQL 16 계열 우선 검토 |
| DB instance identifier | `tftgogo-postgres` |
| Master username | `tftuser` |
| Public access | No |
| VPC | `tftgogo` |
| DB subnet group | `tftgogo-db-subnet-group` |
| Security group | `tftgogo-postgres-sg` |
| Initial database name | `tftgogo_ai` |

주의:

- 현재 로컬 compose는 `pgvector/pgvector:pg16`을 쓴다.
- RDS PostgreSQL에서 pgvector 확장이 필요하면 DB 접속 후 `CREATE EXTENSION vector;` 가능 여부를 확인한다.
- ai-server 코드가 아직 없으면 PostgreSQL 생성은 내일 비용/범위에 맞춰 보류해도 된다.
- PostgreSQL을 만들더라도 backend가 직접 접근하지 않으면 backend ECS SG를 PostgreSQL SG source에 추가하지 않는다.

## 4. ElastiCache Redis

ElastiCache > Redis OSS caches > Create

| 항목 | 값 |
| --- | --- |
| Deployment option | Design your own cache |
| Creation method | Cluster cache |
| Name | `tftgogo-redis` |
| Engine | Redis OSS |
| Node type | 개발용 최소 타입 |
| Multi-AZ | 개발 단계에서는 필요 시 비활성 |
| Cluster mode | Disabled |
| VPC | `tftgogo` |
| Subnet group | private subnet 2개 |
| Security group | `tftgogo-redis-sg` |

Subnet group:

| 항목 | 값 |
| --- | --- |
| Name | `tftgogo-redis-subnet-group` |
| VPC | `tftgogo` |
| Subnets | private subnet 2개 |

백엔드 환경변수 매핑:

```text
SPRING_DATA_REDIS_HOST=<redis-primary-endpoint>
SPRING_DATA_REDIS_PORT=6379
```

## 5. ECR

ECR > Repositories > Create repository

| 항목 | 값 |
| --- | --- |
| Visibility | Private |
| Repository name | `tftgogo-backend` |
| Image tag mutability | 내일 수동 배포는 Mutable, 운영 자동화 후 Immutable 권장 |
| Scan on push | Enable |

ai-server용 repository는 코드/Dockerfile 준비 후 `tftgogo-ai-server`로 추가한다.

로컬에서 수동 push할 때 필요한 값:

```text
AWS_ACCOUNT_ID=<계정 ID>
AWS_REGION=ap-northeast-2
ECR_BACKEND_REPO=$AWS_ACCOUNT_ID.dkr.ecr.ap-northeast-2.amazonaws.com/tftgogo-backend
```

PowerShell 예시:

```powershell
$env:AWS_REGION = "ap-northeast-2"
$env:AWS_ACCOUNT_ID = "<계정 ID>"
$env:ECR_BACKEND_REPO = "$env:AWS_ACCOUNT_ID.dkr.ecr.$env:AWS_REGION.amazonaws.com/tftgogo-backend"

aws ecr get-login-password --region $env:AWS_REGION |
  docker login --username AWS --password-stdin "$env:AWS_ACCOUNT_ID.dkr.ecr.$env:AWS_REGION.amazonaws.com"

docker build -t tftgogo-backend:manual-YYYYMMDD ./backend
docker tag tftgogo-backend:manual-YYYYMMDD "$env:ECR_BACKEND_REPO:manual-YYYYMMDD"
docker push "$env:ECR_BACKEND_REPO:manual-YYYYMMDD"
```

push 후 ECR 콘솔에서 image digest가 보이는지 확인하고, ECS task definition에는 tag 또는 digest URI를 넣는다.

## 6. IAM

### 6.1 Secret 저장 위치

내일 MVP에서는 SSM Parameter Store `SecureString`을 권장한다. ECS task definition에서는 plain environment가 아니라 container `secrets` 항목의 `valueFrom`으로 참조한다. 값 자체를 task definition 화면에 붙여넣지 않는다.

권장 parameter 이름:

| 이름 | 용도 |
| --- | --- |
| `/tftgogo/backend/mysql-password` | `SPRING_DATASOURCE_PASSWORD` |
| `/tftgogo/backend/jwt-secret` | `JWT_SECRET` |
| `/tftgogo/backend/riot-api-key` | `RIOT_API_KEY` |
| `/tftgogo/backend/admin-secret-token` | `ADMIN_SECRET_TOKEN` |
| `/tftgogo/backend/google-client-id` | Google OAuth client id |
| `/tftgogo/backend/google-client-secret` | Google OAuth client secret |
| `/tftgogo/backend/kakao-client-id` | Kakao OAuth client id |
| `/tftgogo/backend/kakao-client-secret` | Kakao OAuth client secret |
| `/tftgogo/backend/naver-client-id` | Naver OAuth client id |
| `/tftgogo/backend/naver-client-secret` | Naver OAuth client secret |

OAuth가 내일 범위 밖이면 OAuth parameter는 나중에 추가해도 된다.

주의: `application*.yml`은 git에서 제외되어 있고, repo에는 `application.yml.example`만 있다. Google은 Spring 기본 provider 값으로 일부 설정을 추론할 수 있지만, Kakao/Naver는 provider URI, grant type, scope 같은 non-secret 설정이 필요하다. OAuth를 내일 검증하지 않을 거면 OAuth secret도 넣지 않고 `GET /api/v1/auth/social/{provider}`가 `SOCIAL_PROVIDER_NOT_CONFIGURED`로 막히는 상태를 허용한다. OAuth를 켤 거면 8.2의 OAuth non-secret 환경변수까지 함께 넣는다.

### 6.2 ECS task execution role

Name: `tftgogo-ecs-task-execution-role`

Trusted entity:

```json
{
  "Service": "ecs-tasks.amazonaws.com"
}
```

Attach policy:

- `AmazonECSTaskExecutionRolePolicy`

추가 inline policy:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ssm:GetParameters",
        "ssm:GetParameter",
        "kms:Decrypt"
      ],
      "Resource": [
        "arn:aws:ssm:ap-northeast-2:<account-id>:parameter/tftgogo/backend/*",
        "arn:aws:kms:ap-northeast-2:<account-id>:key/<kms-key-id>"
      ]
    }
  ]
}
```

KMS customer managed key를 쓰지 않으면 `kms:Decrypt` 문장은 실제 Parameter Store 암호화 키 기준으로 조정하거나 제거한다. 전용 CMK를 쓰는 경우에는 `kms:ViaService=ssm.ap-northeast-2.amazonaws.com` 조건을 추가한다.

### 6.3 Backend task role

Name: `tftgogo-backend-task-role`

초기에는 앱 코드가 AWS API를 직접 호출하지 않으면 별도 권한 없이 생성한다.

필요 시 이후 S3, Secrets Manager, Bedrock 등 실제 사용하는 AWS API에만 최소 권한을 부여한다.

### 6.4 GitHub Actions 배포 role

내일 콘솔 수동 배포만 하면 보류한다. CI/CD를 바로 붙이면 OIDC role `tftgogo-github-deploy-role`을 추가한다.

필요 권한 범위:

- ECR 로그인, 이미지 push: `ecr:GetAuthorizationToken`, `ecr:BatchCheckLayerAvailability`, `ecr:InitiateLayerUpload`, `ecr:UploadLayerPart`, `ecr:CompleteLayerUpload`, `ecr:PutImage`
- ECS 배포: `ecs:RegisterTaskDefinition`, `ecs:UpdateService`, `ecs:DescribeServices`, `ecs:DescribeTaskDefinition`
- Role 전달: `iam:PassRole` 대상은 `tftgogo-ecs-task-execution-role`, `tftgogo-backend-task-role`로 제한

OIDC trust policy에는 최소한 아래 조건을 둔다.

```json
{
  "StringEquals": {
    "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
  },
  "StringLike": {
    "token.actions.githubusercontent.com:sub": "repo:s4ngg/TFT-gogo:ref:refs/heads/develop"
  }
}
```

`iam:PassRole`에는 `iam:PassedToService=ecs-tasks.amazonaws.com` 조건을 추가한다.

## 7. CloudWatch Logs

CloudWatch > Log groups > Create log group

| 항목 | 값 |
| --- | --- |
| Log group name | `/ecs/tftgogo-backend` |
| Retention | 개발 단계 7~14일 |

ECS task definition에서 log group 자동 생성을 켜도 되지만, 내일 디버깅 시간을 줄이려면 미리 만든다.

## 8. ECS

### 8.1 Cluster

ECS > Clusters > Create cluster

| 항목 | 값 |
| --- | --- |
| Cluster name | `tftgogo-cluster` |
| Infrastructure | AWS Fargate |

### 8.2 Task definition

ECS > Task definitions > Create new task definition

| 항목 | 값 |
| --- | --- |
| Family | `tftgogo-backend` |
| Launch type | AWS Fargate |
| OS/Architecture | Linux/X86_64 |
| CPU/Memory | 0.5 vCPU / 1GB부터 시작 |
| Task execution role | `tftgogo-ecs-task-execution-role` |
| Task role | `tftgogo-backend-task-role` |
| Container name | `backend` |
| Image URI | ECR `tftgogo-backend` 이미지 URI |
| Container port | 8080 |
| Protocol | TCP |
| Log driver | awslogs |
| Log group | `/ecs/tftgogo-backend` |
| Log stream prefix | `ecs` |

일반 환경변수:

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

ai-server를 함께 배포할 때만 추가:

```text
AI_SERVER_URL=http://<service-discovery-name>:8000
AI_SERVER_TIMEOUT_SECONDS=10
```

`AI_SERVER_URL=http://tftgogo-ai-service:8000` 같은 이름은 Cloud Map 또는 Service Connect 설정 없이는 해석되지 않는다. ai-server가 MVP 제외면 task definition 예시에서 빼고, AI API 호출 전까지 별도 작업으로 남긴다.

도메인/HTTPS 전 임시값:

```text
APP_OAUTH2_AUTHORIZED_REDIRECT_URI=http://localhost:5173/oauth/callback
APP_OAUTH2_LOGIN_FAILURE_REDIRECT_URI=http://localhost:5173/login
APP_CORS_ALLOWED_ORIGINS_0=http://localhost:5173
```

위 HTTP 값은 로컬 프론트에서 AWS 백엔드를 임시 확인할 때만 둔다. 실사용 로그인/OAuth/토큰 QA는 아래처럼 HTTPS 도메인 적용 후 진행한다.

```text
APP_OAUTH2_AUTHORIZED_REDIRECT_URI=https://<domain>/oauth/callback
APP_OAUTH2_LOGIN_FAILURE_REDIRECT_URI=https://<domain>/login
APP_CORS_ALLOWED_ORIGINS_0=https://<domain>
```

`APP_OAUTH2_*`와 `APP_CORS_ALLOWED_ORIGINS_0`는 백엔드 ALB 주소가 아니라 브라우저가 실제 접속하는 프론트엔드 origin 기준으로 잡는다. 프론트가 CloudFront/S3/별도 ALB에 올라가면 그 origin을 넣는다.

OAuth를 활성화할 때 필요한 non-secret 환경변수:

```text
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_REDIRECT_URI={baseUrl}/login/oauth2/code/{registrationId}
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_SCOPE_0=profile
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_SCOPE_1=email
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KAKAO_CLIENT_AUTHENTICATION_METHOD=client_secret_post
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KAKAO_AUTHORIZATION_GRANT_TYPE=authorization_code
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KAKAO_REDIRECT_URI={baseUrl}/login/oauth2/code/{registrationId}
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KAKAO_SCOPE_0=profile_nickname
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KAKAO_SCOPE_1=profile_image
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KAKAO_SCOPE_2=account_email
SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_KAKAO_AUTHORIZATION_URI=https://kauth.kakao.com/oauth/authorize
SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_KAKAO_TOKEN_URI=https://kauth.kakao.com/oauth/token
SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_KAKAO_USER_INFO_URI=https://kapi.kakao.com/v2/user/me
SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_KAKAO_USER_NAME_ATTRIBUTE=id
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_NAVER_AUTHORIZATION_GRANT_TYPE=authorization_code
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_NAVER_REDIRECT_URI={baseUrl}/login/oauth2/code/{registrationId}
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_NAVER_SCOPE_0=name
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_NAVER_SCOPE_1=email
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_NAVER_SCOPE_2=profile_image
SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_NAVER_AUTHORIZATION_URI=https://nid.naver.com/oauth2.0/authorize
SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_NAVER_TOKEN_URI=https://nid.naver.com/oauth2.0/token
SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_NAVER_USER_INFO_URI=https://openapi.naver.com/v1/nid/me
SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_NAVER_USER_NAME_ATTRIBUTE=response
```

Secret 환경변수:

```text
SPRING_DATASOURCE_PASSWORD=/tftgogo/backend/mysql-password
JWT_SECRET=/tftgogo/backend/jwt-secret
RIOT_API_KEY=/tftgogo/backend/riot-api-key
ADMIN_SECRET_TOKEN=/tftgogo/backend/admin-secret-token
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID=/tftgogo/backend/google-client-id
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET=/tftgogo/backend/google-client-secret
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KAKAO_CLIENT_ID=/tftgogo/backend/kakao-client-id
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KAKAO_CLIENT_SECRET=/tftgogo/backend/kakao-client-secret
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_NAVER_CLIENT_ID=/tftgogo/backend/naver-client-id
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_NAVER_CLIENT_SECRET=/tftgogo/backend/naver-client-secret
```

위 블록은 ECS task definition의 `Environment variables`가 아니라 `Secrets`에 넣는다. JSON으로 보면 아래 형태다.

```json
"secrets": [
  {
    "name": "SPRING_DATASOURCE_PASSWORD",
    "valueFrom": "arn:aws:ssm:ap-northeast-2:<account-id>:parameter/tftgogo/backend/mysql-password"
  },
  {
    "name": "JWT_SECRET",
    "valueFrom": "arn:aws:ssm:ap-northeast-2:<account-id>:parameter/tftgogo/backend/jwt-secret"
  }
]
```

주의:

- 도메인/HTTPS가 없으면 소셜 로그인 provider callback 등록이 제한될 수 있다.
- HTTP ALB DNS 상태에서는 실사용 로그인, 소셜 로그인, 관리자 기능, 운영 secret 검증을 하지 않는다.
- `AI_SERVER_URL`은 ai-server 배포 전에는 실제 사용되는 API가 없을 때만 임시값으로 둔다. ai-server ECS service와 service discovery를 만들기 전에는 `localhost`로 두지 않는다.
- 운영 secret은 task definition의 plain environment보다 SSM Parameter Store `valueFrom`으로 주입한다.
- `SPRING_PROFILES_ACTIVE=docker`는 현재 Docker Compose와 AWS env-only 실행을 공유하기 위한 값이다. AWS 전용 설정 파일이 없으므로 필요한 설정은 task definition environment/secrets로 모두 주입한다. 장기적으로는 `prod` 또는 `aws` profile 전략을 별도 PR에서 정리한다.

### 8.3 Service

ECS > Cluster > Services > Create

| 항목 | 값 |
| --- | --- |
| Launch type | Fargate |
| Application type | Service |
| Service name | `tftgogo-backend-service` |
| Desired tasks | 1 |
| VPC | `tftgogo` |
| Subnets | public subnet 2개 |
| Public IP | Turned on |
| Security group | `tftgogo-backend-ecs-sg` |
| Load balancer | Application Load Balancer |
| Container | `backend:8080` |
| Deployment circuit breaker | Enable, rollback Enable |
| Health check grace period | 60초부터 시작 |

## 9. ALB

EC2 > Load Balancers > Create Application Load Balancer

| 항목 | 값 |
| --- | --- |
| Name | `tftgogo-alb` |
| Scheme | Internet-facing |
| IP address type | IPv4 |
| VPC | `tftgogo` |
| Mappings | public subnet 2개 |
| Security group | `tftgogo-alb-sg` |
| Listener | HTTP 80 |
| Target group type | IP |
| Target group name | `tftgogo-backend-tg` |
| Protocol/Port | HTTP 8080 |

순서:

1. Target group `tftgogo-backend-tg`를 먼저 만든다.
2. ALB `tftgogo-alb`를 만들고 HTTP 80 listener를 target group으로 forward한다.
3. ECS service 생성 화면에서 기존 ALB와 target group을 선택한다.

Health check:

| 항목 | 값 |
| --- | --- |
| Protocol | HTTP |
| Path | `/v3/api-docs` |
| Success codes | `200` |

주의:

- 현재 코드 기준 `/health` 보안 허용 설정은 있지만 실제 `/health` controller 또는 actuator 의존성이 보이지 않는다.
- 나중에 `/health` endpoint를 추가하면 ALB health check path를 `/health`로 바꾼다.
- `/v3/api-docs`는 Swagger 문서 endpoint라 health check로는 임시 방편이다. 공개 접근 전에는 `/health` endpoint 추가 또는 actuator health 추가를 선행하고, Swagger 공개 범위는 팀 정책에 맞춰 제한한다.
- `/v3/api-docs`가 200이어도 DB/Redis readiness가 보장되는 것은 아니다. API 스모크 테스트를 별도로 실행한다.

## 10. Route53 + ACM

도메인을 받은 뒤 진행한다.

1. Route53 hosted zone 생성
2. 외부 registrar를 쓰면 hosted zone의 NS record를 registrar에 위임
3. ACM에서 `ap-northeast-2` 리전 인증서 발급
4. DNS validation 레코드 생성
5. `api.<domain>` 또는 `<domain>` A alias record를 ALB로 연결
6. ALB listener에 HTTPS 443 추가
7. HTTP 80은 HTTPS redirect로 변경
8. OAuth provider callback URI를 실제 도메인 HTTPS 주소로 변경

예상 URI:

```text
https://<domain>/login/oauth2/code/google
https://<domain>/login/oauth2/code/kakao
https://<domain>/login/oauth2/code/naver
https://<domain>/oauth/callback
```

## 11. 생성 전 최종 체크

- [ ] 리전이 `ap-northeast-2`인지 확인
- [ ] VPC CIDR이 `10.0.0.0/16`인지 확인
- [ ] public subnet 2개와 private subnet 2개가 서로 다른 AZ에 있는지 확인
- [ ] RDS public access가 `No`인지 확인
- [ ] RDS schema 적용 경로를 확정했고, ECS service 시작 전 schema가 적용되었는지 확인
- [ ] MySQL SG inbound source가 backend ECS SG인지 확인
- [ ] Redis SG inbound source가 backend ECS SG인지 확인
- [ ] Backend ECS SG inbound source가 ALB SG인지 확인
- [ ] 도메인/HTTPS 전 ALB HTTP 80 source가 팀 고정 IP/CIDR인지 확인
- [ ] SSH 22, DB 3306/5432, Redis 6379가 인터넷에 열려 있지 않은지 확인
- [ ] ECS service가 MVP 단계에서는 public subnet + public IP인지 확인
- [ ] ECS task execution role에 ECR pull, CloudWatch Logs, SSM Parameter read 권한이 있는지 확인
- [ ] Backend task role에 불필요한 관리자 권한이 없는지 확인
- [ ] Secret 값이 plain environment가 아니라 `valueFrom`으로 들어갔는지 확인
- [ ] CloudWatch log group `/ecs/tftgogo-backend`가 있고 retention이 설정되었는지 확인
- [ ] ECR image URI가 task definition에 들어갔는지 확인
- [ ] `JWT_SECRET`이 CSPRNG로 생성한 32바이트 이상 값이고 다른 환경과 재사용되지 않았는지 확인
- [ ] ALB health check path가 임시 `/v3/api-docs`인지, `/health` 추가 후 `/health`인지 확인
- [ ] 도메인 전에는 OAuth provider 실사용 QA가 제한될 수 있음을 팀에 공유
- [ ] OAuth를 켤 경우 client id/secret뿐 아니라 Kakao/Naver provider URI와 scope 환경변수까지 들어갔는지 확인
- [ ] `APP_CORS_ALLOWED_ORIGINS_0`가 백엔드 주소가 아니라 실제 프론트엔드 origin인지 확인

## 12. 후속 개선

- [ ] `/health` endpoint 또는 actuator health 추가
- [ ] `application-docker.yml` 또는 AWS용 prod profile 전략 정리
- [ ] Flyway 또는 Liquibase 기반 schema migration 자동화
- [ ] SSM Parameter Store로 secret 주입
- [ ] ECS private subnet 이전
- [ ] NAT Gateway 또는 VPC endpoints 추가
- [ ] HTTPS/ACM/Route53 적용
- [ ] CloudWatch alarm과 log retention 설정
- [ ] GitHub Actions에서 ECR push + ECS deploy 자동화
