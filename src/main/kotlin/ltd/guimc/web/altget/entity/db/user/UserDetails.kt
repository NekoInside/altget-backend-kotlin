package ltd.guimc.web.altget.entity.db.user

import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import java.time.LocalDateTime

@TableName
class UserDetails {
    @TableId
    var userId: Int? = null

    var lastLoginTime: LocalDateTime = LocalDateTime.now()

    var lastLoginIp: String = ""

    var lastLoginGeo: String = ""

    var totalAltFetched: Int = 0

    var dailyAltFetched: Int = 0

    var totalWebFetched: Int = 0

    var dailyWebFetched: Int = 0

    var totalUserApiFetched: Int = 0

    var dailyUserApiFetched: Int = 0

    var totalPaidApiFetched: Int = 0

    var dailyPaidApiFetched: Int = 0
}