package ltd.guimc.web.altget.entity.user

import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import ltd.guimc.web.altget.enum.EnumApiLimitLevel

@TableName
class UserApi {
    @TableId
    var userId: Long? = null

    var apiKey: String? = null

    var limitLevel: EnumApiLimitLevel = EnumApiLimitLevel.LEVEL_LOW
}