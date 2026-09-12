# Build stage: the whole reactor is built so the architecture tests run as part of the image build.
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build

# Copy only the POMs first so dependency resolution is cached independently of source changes.
COPY pom.xml .
COPY order-domain/pom.xml order-domain/
COPY order-application/pom.xml order-application/
COPY order-adapter-rest/pom.xml order-adapter-rest/
COPY order-adapter-persistence/pom.xml order-adapter-persistence/
COPY order-adapter-messaging/pom.xml order-adapter-messaging/
COPY order-bootstrap/pom.xml order-bootstrap/
RUN mvn -B -q dependency:go-offline -DskipTests

COPY . .
RUN mvn -B clean package

FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

# Run unprivileged: nothing in this service needs root.
RUN addgroup -S orders && adduser -S -G orders orders
COPY --from=build /build/order-bootstrap/target/order-service.jar app.jar
USER orders

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s \
  CMD wget -qO- http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
