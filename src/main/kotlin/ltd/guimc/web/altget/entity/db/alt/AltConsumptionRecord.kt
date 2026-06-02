package ltd.guimc.web.altget.entity.db.alt

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import java.time.LocalDateTime

/**
 * Records alt account consumption counts aggregated per hour per channel.
 * Each row stores the number of alts consumed in a given hour for a given channel.
 */
@TableName("alt_consumption_record")
class AltConsumptionRecord {
    @TableId(type = IdType.AUTO)
    var id: Long? = null

    /** Source channel (e.g., "default", "pre-processed") */
    var channel: String = "default"

    /** The start of the hour time slot (minutes/seconds truncated to 00:00) */
    var hourSlot: LocalDateTime? = null

    /** Number of alts consumed in this hour slot */
    var consumedCount: Int = 0
}
