# Multi-stage Dockerfile for IWMS Full-Stack (Java 17 + Spring Boot 3 + React PWA)
# Stage 1: Build the React PWA frontend
FROM node:20-alpine AS frontend-builder
WORKDIR /app/iwms-ui
COPY iwms-ui/package*.json ./
RUN npm install --legacy-peer-deps --no-audit --no-fund
COPY iwms-ui/ ./
RUN npm run build

# Stage 2: Build the Spring Boot executable JAR with bundled frontend
FROM maven:3.9.9-eclipse-temurin-17 AS backend-builder
WORKDIR /app
COPY pom.xml ./
COPY iwms-core/pom.xml ./iwms-core/
COPY iwms-web/pom.xml ./iwms-web/

# Copy sources
COPY iwms-core/src ./iwms-core/src
COPY iwms-web/src ./iwms-web/src

# Copy built frontend assets into Spring Boot static directory
COPY --from=frontend-builder /app/iwms-ui/dist ./iwms-web/src/main/resources/static

# Build fat executable JAR
RUN mvn clean package -DskipTests

# Stage 3: Minimal production JRE runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create data directories for persistence
RUN mkdir -p /app/data /app/backups

# Copy final fat JAR
COPY --from=backend-builder /app/iwms-web/target/iwms.jar /app/iwms.jar

# Expose HTTP port 8443
EXPOSE 8443

# Environment defaults
ENV PORT=8443 \
    JAVA_OPTS="-Xms128m -Xmx512m -XX:+UseG1GC"

# Launch IWMS standalone server
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/iwms.jar --server.port=${PORT}"]
