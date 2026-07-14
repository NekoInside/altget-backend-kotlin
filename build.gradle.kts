import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.*

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "ltd.guimc.web"
version = "0.0.1-SNAPSHOT"
description = "altget-backend-kotlin"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.boot.starter.jdbc)
    implementation(libs.spring.boot.starter.mail)
    implementation(libs.spring.boot.starter.thymeleaf)
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.kotlin.reflect)
    implementation(libs.mybatis.plus.spring.boot4.starter)
    implementation(libs.mybatis.plus.jsqlparser)
    implementation(libs.spring.security.webauthn)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.yubico.webauthn.server.core)
    implementation(libs.srp6a)
    implementation(libs.hutool)
    runtimeOnly(libs.mariadb.java.client)
    testImplementation(libs.spring.boot.starter.data.redis.test)
    testImplementation(libs.spring.boot.starter.jdbc.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.mybatis.spring.boot.starter.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// ---------------------------------------------------------------------------
// Generate build-info.properties with git commit hash and build metadata
// ---------------------------------------------------------------------------
val generatedResources = layout.buildDirectory.dir("generated/resources/build-info")

val generateBuildInfo by tasks.registering {
    outputs.dir(generatedResources)
    doLast {
        val outputDir = generatedResources.get().asFile
        outputDir.mkdirs()

        val props = Properties()

        // Helper: run a command and return stdout as a trimmed string
        fun executeCommand(vararg args: String): String {
            return try {
                val process = ProcessBuilder(*args)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .start()
                process.inputStream.bufferedReader().readText().trim()
            } catch (e: Exception) {
                "unknown"
            }
        }

        // Git commit hash (first 8 characters)
        props["git.commit.id"] = executeCommand("git", "rev-parse", "--short=8", "HEAD")

        // Git branch name
        props["git.branch"] = executeCommand("git", "rev-parse", "--abbrev-ref", "HEAD")

        // Git commit time (ISO-8601)
        props["git.commit.time"] = executeCommand("git", "log", "-1", "--format=%cI")

        // Build timestamp
        props["build.time"] = Instant.now().toString()

        // Kotlin version
        props["build.kotlin.version"] = libs.versions.kotlin.get()

        // Spring Boot version
        props["build.spring.boot.version"] = libs.versions.spring.boot.get()

        // Java runtime information
        props["build.java.version"] = System.getProperty("java.version", "unknown")
        props["build.java.vendor"] = System.getProperty("java.vendor", "unknown")
        props["build.java.vm"] = System.getProperty("java.vm.name", "unknown")
        props["build.java.runtime"] = System.getProperty("java.runtime.version", "unknown")

        val propsFile = File(outputDir, "build-info.properties")
        propsFile.outputStream().use { props.store(it, "Altget Backend Build Information") }

        logger.lifecycle("Build info generated: ${propsFile.absolutePath}")
    }
}

sourceSets {
    main {
        resources {
            srcDir(generateBuildInfo)
        }
    }
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar> {
    archiveFileName.set("app.jar")
}
