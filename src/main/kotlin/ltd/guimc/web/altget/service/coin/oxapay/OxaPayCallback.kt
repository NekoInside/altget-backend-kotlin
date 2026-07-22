package ltd.guimc.web.altget.service.coin.oxapay

import cn.hutool.json.JSONUtil
import java.math.BigDecimal
import java.nio.charset.StandardCharsets

data class OxaPayCallback(
    val trackId: String,
    val status: String,
    val type: String,
    val amount: BigDecimal,
    val currency: String,
    val orderId: String,
) {
    companion object {
        fun parse(rawBody: ByteArray): OxaPayCallback {
            val json = JSONUtil.parseObj(String(rawBody, StandardCharsets.UTF_8))
            return OxaPayCallback(
                trackId = json.requiredString("track_id"),
                status = json.requiredString("status"),
                type = json.requiredString("type"),
                amount = json.getBigDecimal("amount") ?: throw IllegalArgumentException("Missing callback field: amount"),
                currency = json.requiredString("currency"),
                orderId = json.requiredString("order_id"),
            )
        }

        private fun cn.hutool.json.JSONObject.requiredString(name: String): String =
            getStr(name)?.takeIf { it.isNotBlank() } ?: throw IllegalArgumentException("Missing callback field: $name")
    }
}
