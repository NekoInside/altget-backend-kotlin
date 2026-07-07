package ltd.guimc.web.altget.entity.db.alt

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import java.time.LocalDateTime

/**
 * An alt account that has been consumed (fetched) from the pool.
 *
 * Mirrors the [AltCategory] data fields and additionally records who fetched it,
 * from where, through which channel, and when.
 */
@TableName("used_alt_category")
class UsedAltCategory {
    @TableId(type = IdType.AUTO)
    var id: Long? = null

    var username: String? = null

    var password: String? = null

    /** Original alt pool channel this alt belonged to (e.g., "default", "pre-processed"). */
    var channel: String? = "default"

    /** The user who fetched this alt, or null for anonymous fetches. */
    var userId: Int? = null

    /** IP address the fetch request originated from. */
    @TableField("operation_ip")
    var operationIp: String? = null

    /** How this alt was fetched: "web", "freeapi", or "paidapi". */
    var fetchMethod: String = "web"

    /** When this alt was fetched (consumed). */
    @TableField("fetch_time")
    var fetchTime: LocalDateTime = LocalDateTime.now()

    /** When this alt was originally added to the pool. */
    var createdAt: LocalDateTime? = null
}
