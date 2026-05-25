package ltd.guimc.web.altget

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class AltgetBackendKotlinApplication

fun main(args: Array<String>) {
    runApplication<AltgetBackendKotlinApplication>(*args)
}
