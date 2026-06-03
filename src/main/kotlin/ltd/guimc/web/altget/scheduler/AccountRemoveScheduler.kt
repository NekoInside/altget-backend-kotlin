package ltd.guimc.web.altget.scheduler

import ltd.guimc.web.altget.enum.EnumUserRole
import ltd.guimc.web.altget.service.user.CoreAuthService
import ltd.guimc.web.altget.service.user.UserDetailsService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Component
class AccountRemoveScheduler(
    private val userDetailsService: UserDetailsService,
    private val coreAuthService: CoreAuthService
) {
    private val logger = LoggerFactory.getLogger(AccountRemoveScheduler::class.java)

    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.MINUTES)
    fun removeScheduler() {
        logger.debug("AccountRemoveScheduler triggered")
        val userDetails = userDetailsService.list()
        val toRemove = userDetails.filter {
            it.userRole == EnumUserRole.UNVERIFY && it.registerTime.isBefore(LocalDateTime.now().minusMinutes(10))
        }
        if (toRemove.isEmpty()) {
            logger.debug("No unverified accounts to remove")
            return
        }
        logger.info("Removing {} unverified accounts", toRemove.size)
        toRemove.forEach {
            coreAuthService.removeAccount(it.userId!!)
        }
    }
}