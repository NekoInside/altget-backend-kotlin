package ltd.guimc.web.altget.entity.user

import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName

@TableName
class UserOauth {
    @TableId
    var userId: Int? = null

    var githubId: String? = null

    var telegramId: String? = null

    var discordId: String? = null
}