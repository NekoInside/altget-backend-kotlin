package ltd.guimc.web.altget.entity.db.alt

import com.baomidou.mybatisplus.annotation.FieldFill
import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import java.time.LocalDateTime

@TableName
class AltCategory {
    @TableId(type = IdType.AUTO)
    var id: Long? = null

    var username: String? = null

    var password: String? = null

    @TableField(fill = FieldFill.INSERT)
    var createdAt: LocalDateTime? = null

    var channel: String? = "default"
}