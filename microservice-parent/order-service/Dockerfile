# =================================================
# Dockerfile for our Spring Boot order-service
# =================================================

# STAGE 1: Build Image
FROM eclipse-temurin:26-jdk AS build

# Set working directory
WORKDIR /app

# === Copy root-level Gradle files (build context = microserices-parent/) ===
# settings NOT setting
COPY ./build.gradle.kts ./settings.gradle.kts ./
COPY ./build.gradle.kts ./
COPY ./gradlew ./gradlew.bat ./
COPY ./gradle ./gradle

# Copy build descriptors for ALL modules (Gradle configures every module in
# settings.gradle.kts at startup, even when only one module is being built)
COPY ./order-service/build.gradle.kts     ./order-service/build.gradle.kts
COPY ./product-service/build.gradle.kts   ./product-service/build.gradle.kts
COPY ./inventory-service/build.gradle.kts ./inventory-service/build.gradle.kts
COPY ./api-gateway/build.gradle.kts        ./api-gateway/build.gradle.kts

# Copy source only for the module being built
COPY ./order-service/src ./order-service/src

# Give execute permissions to Gradle wrapper
RUN chmod +x ./gradlew

# Force explicit TLS protocol list - JDK26 + Docker Desktop
# The TLS hanbdlshake  with Maven Central canm fail without this
ENV JAVA_TOOLS_OPTIONS="-Djdk.tls.client.protocols=TLSv1.2,TLSv1.3 -Dhttps.protocols=TLSv1.2,TLSv1.3"

# Build only the produc-service module/micro-service
RUN ./gradlew  :order-service:clean :order-service:bootjar --no-daemon

# STAGE 2: Package (Runtime) Stage
FROM eclipse-temurin:26-jdk

WORKDIR /app

COPY --from=build /app/order-service/build/libs/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]

