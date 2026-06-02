package ltd.guimc.web.altget.scheduler

import ltd.guimc.web.altget.service.alt.AltConsumptionRecordService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class AltConsumptionCleanupScheduler(
    private val altConsumptionRecordService: AltConsumptionRecordService
) {
    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
    fun cleanupOldConsumptionRecords() {
        altConsumptionRecordService.cleanupOldRecords(30)
    }
}
