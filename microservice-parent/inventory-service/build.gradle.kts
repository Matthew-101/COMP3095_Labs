plugins {
    java
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "ca.gbc.comp3095"
version = "0.0.1-SNAPSHOT"
description = "inventory-service"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(26)
    }
}

repositories {
    mavenCentral()
}

// Inherit compileOnly (Lombok) and annotationProcessor into test scope automatically,
// matching the order-service convention — avoids duplicating testCompileOnly/testAnnotationProcessor.
configurations {
    testCompileOnly { extendsFrom(configurations.compileOnly.get()) }
    testAnnotationProcessor { extendsFrom(configurations.annotationProcessor.get()) }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springframework.boot:spring-boot-starter-web")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")   // WebTestClient
    testImplementation("org.springframework.boot:spring-boot-testcontainers")

    testImplementation("org.testcontainers:junit-jupiter:1.21.3")
    testImplementation("org.testcontainers:postgresql:1.21.3")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    //Lesson 6.1
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:3.0.3")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
