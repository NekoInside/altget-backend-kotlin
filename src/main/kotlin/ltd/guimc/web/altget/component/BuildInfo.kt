package ltd.guimc.web.altget.component

import org.springframework.stereotype.Component
import java.util.*

/**
 * Reads and exposes build metadata generated at compile time by the Gradle `generateBuildInfo` task.
 *
 * The properties file (`build-info.properties`) is written into the classpath during the build
 * and contains git commit hash, branch name, build time, Kotlin/Spring Boot versions, etc.
 */
@Component
class BuildInfo {

    private val properties: Properties = Properties()

    init {
        try {
            val stream = javaClass.getResourceAsStream("/build-info.properties")
            if (stream != null) {
                properties.load(stream)
            } else {
                // Fallback when running in IDE without build
                properties.setProperty("git.commit.id", "development")
                properties.setProperty("git.branch", "development")
                properties.setProperty("git.commit.time", "unknown")
                properties.setProperty("build.time", "unknown")
                properties.setProperty("build.kotlin.version", "unknown")
                properties.setProperty("build.spring.boot.version", "unknown")
                properties.setProperty("build.java.version", System.getProperty("java.version", "unknown"))
                properties.setProperty("build.java.vendor", System.getProperty("java.vendor", "unknown"))
                properties.setProperty("build.java.vm", System.getProperty("java.vm.name", "unknown"))
                properties.setProperty("build.java.runtime", System.getProperty("java.runtime.version", "unknown"))
            }
        } catch (e: Exception) {
            // Ignore — build info is non-critical
            properties.setProperty("git.commit.id", "unknown")
            properties.setProperty("git.branch", "unknown")
            properties.setProperty("git.commit.time", "unknown")
            properties.setProperty("build.time", "unknown")
            properties.setProperty("build.kotlin.version", "unknown")
            properties.setProperty("build.spring.boot.version", "unknown")
            properties.setProperty("build.java.version", "unknown")
            properties.setProperty("build.java.vendor", "unknown")
            properties.setProperty("build.java.vm", "unknown")
            properties.setProperty("build.java.runtime", "unknown")
        }
    }

    /** Full 8-character git commit hash. */
    val commitId: String get() = properties.getProperty("git.commit.id", "unknown")

    /** Git branch name at build time. */
    val branch: String get() = properties.getProperty("git.branch", "unknown")

    /** ISO-8601 timestamp of the last git commit. */
    val commitTime: String get() = properties.getProperty("git.commit.time", "unknown")

    /** ISO-8601 timestamp of when the build was performed. */
    val buildTime: String get() = properties.getProperty("build.time", "unknown")

    /** Kotlin version used for compilation. */
    val kotlinVersion: String get() = properties.getProperty("build.kotlin.version", "unknown")

    /** Spring Boot version used for the project. */
    val springBootVersion: String get() = properties.getProperty("build.spring.boot.version", "unknown")

    /** Java version used at build time. */
    val javaVersion: String get() = properties.getProperty("build.java.version", "unknown")

    /** Java vendor at build time. */
    val javaVendor: String get() = properties.getProperty("build.java.vendor", "unknown")

    /** Java VM name at build time. */
    val javaVm: String get() = properties.getProperty("build.java.vm", "unknown")

    /** Java runtime version at build time. */
    val javaRuntime: String get() = properties.getProperty("build.java.runtime", "unknown")

    /** Return all properties as an immutable map. */
    fun toMap(): Map<String, String> {
        val map = LinkedHashMap<String, String>()
        properties.stringPropertyNames().forEach { key ->
            map[key] = properties.getProperty(key)
        }
        return Collections.unmodifiableMap(map)
    }
}
