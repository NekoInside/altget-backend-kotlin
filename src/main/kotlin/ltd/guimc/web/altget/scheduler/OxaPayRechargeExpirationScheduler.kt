package ltd.guimc.web.altget.scheduler

import ltd.guimc.web.altget.service.coin.OxaPayRechargeService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class OxaPayRechargeExpirationScheduler(
    private val oxaPayRechargeService: OxaPayRechargeService,
) {
    private val logger = LoggerFactory.getLogger(OxaPayRechargeExpirationScheduler::class.java)

    @Scheduled(fixedDelay = 1, initialDelay = 1, timeUnit = TimeUnit.MINUTES)
    fun markExpiredOrders() {
        val expiredCount = oxaPayRechargeService.markExpiredOrders()
        if (expiredCount > 0) {
            logger.info("Marked {} OxaPay recharge orders as expired", expiredCount)
        }
    }
}
