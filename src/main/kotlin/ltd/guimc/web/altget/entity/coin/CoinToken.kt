package ltd.guimc.web.altget.entity.coin

import com.baomidou.mybatisplus.annotation.FieldFill
import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import java.time.LocalDateTime

@TableName
class CoinToken {
    @TableId
    var id: String? = null

    var coinAmount: Int? = null

    var createdBy: Int? = null

    @TableField(fill = FieldFill.INSERT)
    var createdAt: LocalDateTime? = null

    var redeemedBy: Int? = null

    var redeemedAt: LocalDateTime? = null

    var isUsed: Boolean = false
}