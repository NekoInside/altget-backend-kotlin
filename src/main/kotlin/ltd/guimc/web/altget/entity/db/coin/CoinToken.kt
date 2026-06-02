package ltd.guimc.web.altget.entity.db.coin

import com.baomidou.mybatisplus.annotation.FieldFill
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import java.time.LocalDateTime
import java.util.UUID

@TableName
class CoinToken {
    @TableId
    var id: String = UUID.randomUUID().toString()

    var coinAmount: Int = 0

    var createdBy: Int? = null

    @TableField(fill = FieldFill.INSERT)
    var createdAt: LocalDateTime? = null

    var redeemedBy: Int? = null

    var redeemedAt: LocalDateTime? = null

    var isUsed: Boolean = false
}