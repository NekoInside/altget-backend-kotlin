package ltd.guimc.web.altget.service.coin.oxapay

import cn.hutool.http.HttpUtil
import cn.hutool.json.JSONUtil
import ltd.guimc.web.altget.config.OxaPayProperties
import ltd.guimc.web.altget.entity.db.coin.OxaPayRechargeOrder
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Component
class OxaPayClient(private val properties: OxaPayProperties) {
    data class CreatedInvoice(
        val trackId: String,
        val paymentUrl: String,
        val expiredAt: LocalDateTime,
    )

    fun createInvoice(order: OxaPayRechargeOrder): CreatedInvoice {
        check(properties.merchantApiKey.isNotBlank()) { "OxaPay merchant API key is not configured" }
        check(properties.callbackUrl.isNotBlank()) { "OxaPay callback URL is not configured" }
        check(properties.invoiceLifetimeMinutes in 15..2880) { "OxaPay invoice lifetime must be between 15 and 2880 minutes" }

        val payload = linkedMapOf<String, Any>(
            "amount" to order.usdAmount,
            "currency" to "USD",
            "lifetime" to properties.invoiceLifetimeMinutes,
            "fee_paid_by_payer" to 0,
            "under_paid_coverage" to 0,
            "mixed_payment" to false,
            "callback_url" to properties.callbackUrl,
            "order_id" to order.id,
            "description" to "${order.coinAmount} coins recharge",
            "sandbox" to properties.sandbox,
        )
        if (properties.returnUrl.isNotBlank()) payload["return_url"] = properties.returnUrl

        val response = try {
            HttpUtil.createPost("${properties.apiBaseUrl.trimEnd('/')}/payment/invoice")
                .header("merchant_api_key", properties.merchantApiKey)
                .header("Content-Type", "application/json")
                .timeout(properties.timeoutMillis)
                .body(JSONUtil.toJsonStr(payload))
                .execute()
        } catch (e: Exception) {
            throw OxaPayException("Failed to connect to OxaPay", e)
        }

        try {
            val responseBody = response.body()
            if (!response.isOk) {
                throw OxaPayException("OxaPay invoice request failed with HTTP ${response.status}")
            }
            val root = try {
                JSONUtil.parseObj(responseBody)
            } catch (e: Exception) {
                throw OxaPayException("OxaPay returned an invalid response", e)
            }
            if (root.getInt("status") != 200) {
                val errorMessage = root.getJSONObject("error")?.getStr("message") ?: root.getStr("message")
                throw OxaPayException("OxaPay invoice request was rejected: ${errorMessage ?: "unknown error"}")
            }
            val data = root.getJSONObject("data") ?: throw OxaPayException("OxaPay response is missing invoice data")
            val trackId = data.getStr("track_id")?.takeIf { it.isNotBlank() }
                ?: throw OxaPayException("OxaPay response is missing track_id")
            val paymentUrl = data.getStr("payment_url")?.takeIf { it.isNotBlank() }
                ?: throw OxaPayException("OxaPay response is missing payment_url")
            val expiredAt = data.getLong("expired_at")
                ?: throw OxaPayException("OxaPay response is missing expired_at")
            return CreatedInvoice(
                trackId = trackId,
                paymentUrl = paymentUrl,
                expiredAt = LocalDateTime.ofInstant(Instant.ofEpochSecond(expiredAt), ZoneOffset.UTC),
            )
        } finally {
            response.close()
        }
    }
}

class OxaPayException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}
