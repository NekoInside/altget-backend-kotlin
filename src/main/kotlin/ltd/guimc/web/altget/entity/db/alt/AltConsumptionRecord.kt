package ltd.guimc.web.altget.entity.db.alt

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import java.time.LocalDateTime

/**
 * Records a single alt fetch event.
 *
 * Each row corresponds to one fetch operation and stores where it originated
 * ([source] / "from"), which alt pool it drew from ([channel]), how many alts
 * were consumed ([count]), and when it happened ([time]).
 *
 * Stored in a dedicated table (`alt_consumption_event`) rather than the legacy
 * pre-aggregated `alt_consumption_record` table, so that arbitrary time-bucket
 * granularities can be derived at query time.
 */
@TableName("alt_consumption_event")
class AltConsumptionRecord {
    @TableId(type = IdType.AUTO)
    var id: Long? = null

    /** Where the fetch originated: "webfetch", "freeapifetch", or "paidapifetch". */
    var source: String = "webfetch"

    /** The alt pool channel the fetch drew from (e.g., "default", "pre-processed"). */
    var channel: String = "default"

    /** The user who triggered the fetch, or null for anonymous fetches. */
    var userId: Int? = null

    /** Number of alts consumed in this fetch event. */
    var count: Int = 0

    /** When the fetch event happened. */
    @TableField("event_time")
    var time: LocalDateTime = LocalDateTime.now()
}
