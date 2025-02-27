# Build stage
FROM maven:3.9.9-eclipse-temurin-17 AS build-hapi

WORKDIR /tmp/hapi-fhir-jpaserver-starter

# Download OpenTelemetry agent for instrumentation
RUN curl -LSsO https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.0.0/opentelemetry-javaagent.jar

# Copy only the POM file first to leverage Docker layer caching
COPY pom.xml .
COPY server.xml .

# Download dependencies
RUN mvn -ntp dependency:go-offline

# Copy source files
COPY src/ /tmp/hapi-fhir-jpaserver-starter/src/

# Build with explicit cleanup to save space
RUN mvn clean package -DskipTests spring-boot:repackage -Pboot \
    && rm -rf /root/.m2/repository/ca/uhn/hapi/fhir \
    && rm -rf /root/.m2/repository/org/springframework \
    && rm -rf /root/.m2/repository/org/hibernate

# Runtime stage
FROM gcr.io/distroless/java17-debian12:nonroot

# Copy the WAR file and OpenTelemetry agent
COPY --from=build-hapi /tmp/hapi-fhir-jpaserver-starter/target/ROOT.war /app/main.war
COPY --from=build-hapi /tmp/hapi-fhir-jpaserver-starter/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar

WORKDIR /app

# Set entry point with JVM memory optimization flags
ENTRYPOINT ["java", "-jar", "-Xmx4g", "-Dspring.profiles.active=default", "main.war"]
