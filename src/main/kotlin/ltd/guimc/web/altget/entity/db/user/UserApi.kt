package ltd.guimc.web.altget.entity.db.user

import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import ltd.guimc.web.altget.enum.EnumApiLimitLevel
import java.util.UUID

@TableName
class UserApi {
    @TableId
    var userId: Int = 0

    var apiKey: String = UUID.randomUUID().toString()

    var limitLevel: EnumApiLimitLevel = EnumApiLimitLevel.LEVEL_LOW
}