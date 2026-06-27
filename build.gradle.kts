plugins {
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.1.21"
    kotlin("plugin.spring") version "2.1.21"
    id("org.jlleitschuh.gradle.ktlint") version "12.3.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
}

group = "com.stringpro"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.0")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("net.logstash.logback:logstash-logback-encoder:8.1")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "mockito-core")
    }
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test") {
        exclude(module = "mockito-core")
    }
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.mockk:mockk:1.14.2")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("de.flapdoodle.embed:de.flapdoodle.embed.mongo.spring4x:4.24.0")
    testImplementation("org.springframework.boot:spring-boot-data-mongodb-test")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

// The Kotlin Gradle plugin force-aligns kotlin-compiler-embeddable to the project's
// Kotlin version (2.1.21) on every configuration, including the lint tool classpaths.
// That breaks detekt 1.23.8 (built against 2.0.21, rejects the bump) and ktlint 1.0.1
// (built against 1.9.10, references the HEADER_KEYWORD token that 2.1.21 removed).
// Pin each tool's bundled compiler back to the version it ships with so the rulesets
// run unchanged.
configurations.named("detekt") {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin" && requested.name == "kotlin-compiler-embeddable") {
            useVersion("2.0.21")
        }
    }
}
configurations.matching { it.name.startsWith("ktlint") }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin" && requested.name == "kotlin-compiler-embeddable") {
            useVersion("1.9.10")
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
