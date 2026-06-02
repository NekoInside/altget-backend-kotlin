package ltd.guimc.web.altget.service.alt

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import ltd.guimc.web.altget.entity.db.alt.AltConsumptionRecord
import ltd.guimc.web.altget.mapper.db.alt.AltConsumptionRecordMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
class AltConsumptionRecordService : ServiceImpl<AltConsumptionRecordMapper, AltConsumptionRecord>() {

    private val logger = LoggerFactory.getLogger(AltConsumptionRecordService::class.java)

    /**
     * Record alt consumption by incrementing the count for the current hour slot.
     * This runs in a new transaction to ensure the consumption record is persisted
     * even if the outer transaction rolls back.
     *
     * @param channel The source channel of the consumed alts
     * @param count   Number of alts consumed
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun recordConsumption(channel: String, count: Int) {
        if (count <= 0) return
        val hourSlot = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS)
        baseMapper.incrementConsumption(channel, hourSlot, count)
    }

    /**
     * Delete all consumption records older than the given number of days.
     *
     * @param days Records with [hourSlot] before (now - days) will be deleted
     * @return Number of rows deleted
     */
    @Transactional
    fun cleanupOldRecords(days: Long = 30): Int {
        val cutoff = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).minusDays(days)
        val deleted = baseMapper.deleteOlderThan(cutoff)
        if (deleted > 0) {
            logger.info("Cleaned up {} alt consumption records older than {} days (cutoff: {})", deleted, days, cutoff)
        }
        return deleted
    }
}
