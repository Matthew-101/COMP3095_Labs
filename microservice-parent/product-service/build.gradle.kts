plugins {
	java
	id("org.springframework.boot") version "4.0.6"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "ca.gbc.comp3095"
version = "0.0.1-SNAPSHOT"
description = "product-service"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(26)
	}
}

repositories {
	mavenCentral()
}

configurations {
	testCompileOnly { extendsFrom(configurations.compileOnly.get()) }
	testAnnotationProcessor { extendsFrom(configurations.annotationProcessor.get()) }
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
	implementation("org.springframework.boot:spring-boot-starter-web")
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	developmentOnly("org.springframework.boot:spring-boot-docker-compose")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	//Lesson 2.2
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	implementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:testcontainers-mongodb")
	implementation("org.springframework.boot:spring-boot-starter-webflux")

	//Lesson 4.2
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
    testImplementation("com.redis:testcontainers-redis:2.2.4")

	//Lesson 6.1
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:3.0.3")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
