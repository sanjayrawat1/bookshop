plugins {
    java
    id("org.springframework.boot") version "3.0.4"
    id("io.spring.dependency-management") version "1.1.0"
    id("com.diffplug.spotless") version "6.16.0"
}

group = "com.github.sanjayrawat1.bookshop"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_19

description = "API gateway and cross-cutting concerns."

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

extra["springCloudVersion"] = "2022.0.1"
extra["testcontainersVersion"] = "1.17.6"
extra["otelVersion"] = "1.24.0"

dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-reactor-resilience4j")
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    implementation("org.springframework.session:spring-session-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.retry:spring-retry")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    compileOnly("org.projectlombok:lombok")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    runtimeOnly("io.github.resilience4j:resilience4j-micrometer")
    runtimeOnly("io.opentelemetry.javaagent:opentelemetry-javaagent:${property("otelVersion")}")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.testcontainers:junit-jupiter")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
        mavenBom("org.testcontainers:testcontainers-bom:${property("testcontainersVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.build {
    dependsOn("spotlessApply")
}

tasks.bootBuildImage {
    imageName.set(project.name)
    environment.set(environment.get() + mapOf("BP_JVM_VERSION" to "19"))
    docker {
        publishRegistry {
            url.set(project.findProperty("registryUrl").toString())
            username.set(project.findProperty("registryUsername").toString())
            password.set(project.findProperty("registryToken").toString())
        }
    }
}

springBoot {
    buildInfo()
}

spotless {
    java {
        toggleOffOn()
        importOrder()
        removeUnusedImports()
        cleanthat()
        prettier(mapOf("prettier" to "2.8.4", "prettier-plugin-java" to "2.0.0"))
            .config(mapOf("parser" to "java", "printWidth" to 160, "tabWidth" to 4, "useTabs" to false))
        formatAnnotations()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
    format("misc") {
        target("*.gradle", "*.md", ".gitignore")
        trimTrailingWhitespace()
        indentWithSpaces()
        endWithNewline()
    }
}
