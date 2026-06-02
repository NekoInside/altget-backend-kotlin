package ltd.guimc.web.altget.scheduler

import ltd.guimc.web.altget.enum.EnumUserRole
import ltd.guimc.web.altget.service.user.CoreAuthService
import ltd.guimc.web.altget.service.user.UserDetailsService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Component
class AccountRemoveScheduler(
    private val userDetailsService: UserDetailsService,
    private val coreAuthService: CoreAuthService
) {
    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.MINUTES)
    fun removeScheduler() {
        val userDetails = userDetailsService.list()
        userDetails.filter {
            it.userRole == EnumUserRole.UNVERIFY && it.registerTime.minusMinutes(10) > LocalDateTime.now()
        }.forEach {
            coreAuthService.removeAccount(it.userId!!)
        }
    }
}