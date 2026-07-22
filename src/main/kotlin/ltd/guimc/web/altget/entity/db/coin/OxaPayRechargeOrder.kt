package ltd.guimc.web.altget.entity.db.coin

import com.baomidou.mybatisplus.annotation.FieldFill
import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import ltd.guimc.web.altget.enum.EnumOxaPayRechargeStatus
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@TableName("oxapay_recharge_order")
class OxaPayRechargeOrder {
    @TableId(type = IdType.INPUT)
    var id: String = UUID.randomUUID().toString()

    var userId: Int = 0

    var trackId: String? = null

    var usdAmount: BigDecimal = BigDecimal.ZERO

    var cnyAmount: BigDecimal = BigDecimal.ZERO

    var coinAmount: Int = 0

    var status: EnumOxaPayRechargeStatus = EnumOxaPayRechargeStatus.PENDING

    var paymentUrl: String? = null

    var expiredAt: LocalDateTime? = null

    var paidAt: LocalDateTime? = null

    @TableField(fill = FieldFill.INSERT)
    var createdAt: LocalDateTime? = null

    @TableField(fill = FieldFill.INSERT_UPDATE)
    var updatedAt: LocalDateTime? = null
}
