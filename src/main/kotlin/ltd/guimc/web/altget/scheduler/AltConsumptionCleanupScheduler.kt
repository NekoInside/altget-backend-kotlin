package ltd.guimc.web.altget.scheduler

import ltd.guimc.web.altget.service.alt.AltConsumptionRecordService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class AltConsumptionCleanupScheduler(
    private val altConsumptionRecordService: AltConsumptionRecordService
) {
    private val logger = LoggerFactory.getLogger(AltConsumptionCleanupScheduler::class.java)

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
    fun cleanupOldConsumptionRecords() {
        logger.debug("AltConsumptionCleanupScheduler triggered")
        val deleted = altConsumptionRecordService.cleanupOldRecords(30)
        logger.debug("Cleanup finished, {} records deleted", deleted)
    }
}
