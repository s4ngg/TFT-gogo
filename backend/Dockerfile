# ── 1단계: 빌드 ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app

# 의존성 캐시 활용 (소스 변경 시 재다운로드 방지)
COPY gradlew build.gradle ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon -q

COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# ── 2단계: 실행 ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=build /app/build/libs/*.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:docker}", "app.jar"]
