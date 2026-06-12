# =============================================
# Dockerfile for Spring Boot Inventory Service (Multi-Module Gradle)
# =============================================

# Stage 1: Build stage
FROM eclipse-temurin:26-jdk AS build

# Set working directory
WORKDIR /app

# === Copy root-level Gradle files (build context = microservices-parent/) ===
COPY ./build.gradle.kts ./settings.gradle.kts ./
COPY ./gradlew ./gradlew.bat ./
COPY ./gradle ./gradle

# === Copy build descriptors for ALL modules (Gradle configures every module in
#     settings.gradle.kts at startup, even when only one module is being built) ===
COPY ./inventory-service/build.gradle.kts  ./inventory-service/build.gradle.kts
COPY ./order-service/build.gradle.kts      ./order-service/build.gradle.kts
COPY ./product-service/build.gradle.kts    ./product-service/build.gradle.kts
COPY ./api-gateway/build.gradle.kts        ./api-gateway/build.gradle.kts

# === Copy source only for the module being built ===
COPY ./inventory-service/src ./inventory-service/src

# Give execute permission to Gradle wrapper
RUN chmod +x ./gradlew

# Force explicit TLS protocol list - JDK26 + Docker Desktop
# The TLS hanbdlshake  with Maven Central canm fail without this
ENV JAVA_TOOLS_OPTIONS="-Djdk.tls.client.protocols=TLSv1.2,TLSv1.3 -Dhttps.protocols=TLSv1.2,TLSv1.3"

# Build only the inventory-service module
RUN ./gradlew :inventory-service:clean :inventory-service:bootJar --no-daemon

# Stage 2: Runtime stage
FROM eclipse-temurin:26-jdk

WORKDIR /app

# Copy the built JAR
COPY --from=build /app/inventory-service/build/libs/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
