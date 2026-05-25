package ltd.guimc.web.altget.entity.user

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName

@TableName
class CoreAuth {
    @TableId(type = IdType.AUTO)
    var userId: Long = 0

    var username: String? = null

    var password: String? = null

    var email: String? = null
}