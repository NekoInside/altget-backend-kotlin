package ltd.guimc.web.altget.enum

import com.baomidou.mybatisplus.annotation.EnumValue

enum class EnumOxaPayRechargeStatus(
    @EnumValue
    val value: String
) {
    PENDING("PENDING"),
    CREATE_FAILED("CREATE_FAILED"),
    EXPIRED("EXPIRED"),
    PAID_AFTER_EXPIRATION("PAID_AFTER_EXPIRATION"),
    PAID("PAID"),
}
