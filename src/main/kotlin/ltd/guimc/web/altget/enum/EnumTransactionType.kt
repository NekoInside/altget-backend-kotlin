package ltd.guimc.web.altget.enum

import com.baomidou.mybatisplus.annotation.EnumValue

enum class EnumTransactionType(
    @EnumValue
    val value: String,
    val transactionTypeName: String
) {
    TOKEN_REDEEM("TOKEN_REDEEM", "兑换Token"),
    ADMIN_ADD("ADMIN_ADD", "管理员添加"),
    ADMIN_SUBTRACT("ADMIN_SUBTRACT", "管理员扣除"),
    TRANSFER_SENT("TRANSFER_SENT", "转账发送"),
    TRANSFER_RECEIVED("TRANSFER_RECEIVED", "转账接收"),
    PAID_USER_API_FETCH("PAID_USER_API_FETCH", "付费API调用"),
    SAUTH_CONVERT("SAUTH_CONVERT", "Sauth转换"),
    OXAPAY_RECHARGE("OXAPAY_RECHARGE", "OxaPay充值"),
}
