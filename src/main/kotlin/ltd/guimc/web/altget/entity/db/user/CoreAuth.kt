package ltd.guimc.web.altget.entity.db.user

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName

@TableName
class CoreAuth {
    @TableId(type = IdType.AUTO)
    var userId: Int = 0

    var username: String? = null

    var srpSalt: String? = null

    var srpVerifier: String? = null

    var email: String? = null
}