package ltd.guimc.web.altget.entity.coin

import com.baomidou.mybatisplus.annotation.FieldFill
import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import ltd.guimc.web.altget.enum.EnumTransactionType
import java.time.LocalDateTime

@TableName
class CoinTtransactionHistory {
    @TableId(type = IdType.AUTO)
    var id: Long = 0

    var userId: Int = 0

    var amount: Int = 0

    var transactionType: EnumTransactionType? = null

    var relatedTokenId: String? = null

    @TableField(fill = FieldFill.INSERT)
    var createdAt: LocalDateTime? = null
}