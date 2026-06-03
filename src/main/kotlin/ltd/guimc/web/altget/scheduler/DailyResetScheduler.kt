package ltd.guimc.web.altget.scheduler

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import ltd.guimc.web.altget.entity.db.user.UserDetails
import ltd.guimc.web.altget.service.user.UserApiService
import ltd.guimc.web.altget.service.user.UserDetailsService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DailyResetScheduler(
    private val userDetailsService: UserDetailsService,
    private val userApiService: UserApiService
) {
    private val logger = LoggerFactory.getLogger(DailyResetScheduler::class.java)

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    fun dailyReset() {
        logger.info("Daily reset scheduler triggered")

        // Step 1: Check each user's dailyUserApiFetched against the lower limit level threshold,
        // and downgrade limitLevel if usage is below the lower level's limit.
        val allUsers = userDetailsService.list()
        var downgradedCount = 0

        for (user in allUsers) {
            val userApi = userApiService.getById(user.userId!!) ?: continue
            val currentLevel = userApi.limitLevel
            val lowerLevel = currentLevel.getLowerLimitLevel()

            // -1 means no check (LEVEL_UNLIMITED)
            if (lowerLevel.limitDau == -1L) continue
            // Already at the lowest possible level, no downgrade needed
            if (currentLevel == lowerLevel) continue

            // If daily usage is below the lower level's threshold, downgrade
            if (user.dailyUserApiFetched < lowerLevel.limitDau) {
                userApi.limitLevel = lowerLevel
                userApiService.updateById(userApi)
                downgradedCount++
                logger.info(
                    "Downgraded user {} API limit from {} to {} (daily fetched: {})",
                    user.userId, currentLevel, lowerLevel, user.dailyUserApiFetched
                )
            }
        }

        logger.info("Downgraded {} users' API limit levels", downgradedCount)

        // Step 2: Reset all daily counters to zero for every user
        val updateWrapper = UpdateWrapper<UserDetails>()
            .set("daily_alt_fetched", 0)
            .set("daily_web_fetched", 0)
            .set("daily_user_api_fetched", 0)
            .set("daily_paid_api_fetched", 0)
        val updatedRows = userDetailsService.update(updateWrapper)

        logger.info("Daily counters reset for {} users", updatedRows)
    }
}
