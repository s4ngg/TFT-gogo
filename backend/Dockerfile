# ── 1단계: 빌드 ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app

# 의존성 캐시 활용 (소스 변경 시 재다운로드 방지)
COPY gradlew build.gradle ./
COPY gradle ./gradle
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN ./gradlew dependencies --no-daemon -q

COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# ── 2단계: 실행 ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=build /app/build/libs/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
# JVM 기본 http.maxConnections(=5)는 AiServerClient(SimpleClientHttpRequestFactory,
# 커넥션 풀링 없음)가 ai-server로 보내는 동시 요청을 호스트당 5개로 제한한다.
# open-in-view=false로 DB 커넥션 병목을 없앤 뒤 재측정하니 이 제한이 다음 병목으로
# 드러나 완화한다 (#733).
ENV JAVA_TOOL_OPTIONS="-Dhttp.maxConnections=100"

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
