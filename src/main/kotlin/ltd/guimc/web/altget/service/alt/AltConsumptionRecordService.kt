package ltd.guimc.web.altget.service.alt

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import ltd.guimc.web.altget.entity.db.alt.AltConsumptionRecord
import ltd.guimc.web.altget.mapper.db.alt.AltConsumptionRecordMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AltConsumptionRecordService : ServiceImpl<AltConsumptionRecordMapper, AltConsumptionRecord>() {

    /**
     * Record a single fetch event in its own transaction so the consumption is
     * persisted even if the outer transaction rolls back.
     *
     * @param source  Where the fetch originated ("webfetch", "freeapifetch", "paidapifetch")
     * @param channel The alt pool channel the fetch drew from
     * @param userId  The user who triggered the fetch, or null for anonymous fetches
     * @param count   Number of alts consumed
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun recordFetch(source: String, channel: String, userId: Int?, count: Int) {
        if (count <= 0) return
        save(AltConsumptionRecord().apply {
            this.source = source
            this.channel = channel
            this.userId = userId
            this.count = count
            this.time = LocalDateTime.now()
        })
    }
}
