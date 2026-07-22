package ltd.guimc.web.altget.controller

import ltd.guimc.web.altget.annotations.CurrentUserId
import ltd.guimc.web.altget.entity.request.coin.OxaPayRechargeRequest
import ltd.guimc.web.altget.entity.response.ResponseBase
import ltd.guimc.web.altget.entity.response.coin.OxaPayRechargeResponse
import ltd.guimc.web.altget.service.coin.OxaPayRechargeService
import ltd.guimc.web.altget.service.coin.oxapay.OxaPayCallback
import ltd.guimc.web.altget.service.coin.oxapay.OxaPayException
import ltd.guimc.web.altget.service.coin.oxapay.OxaPayWebhookVerifier
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class OxaPayRechargeController(
    private val rechargeService: OxaPayRechargeService,
    private val webhookVerifier: OxaPayWebhookVerifier,
) {
    private val log = LoggerFactory.getLogger(OxaPayRechargeController::class.java)

    @PostMapping("/api/coins/recharges/oxapay")
    fun create(
        @CurrentUserId userId: Int?,
        @RequestBody request: OxaPayRechargeRequest,
    ): ResponseBase<OxaPayRechargeResponse> {
        if (userId == null) return ResponseBase(401, "Unauthorized")
        val amount = request.usdAmount ?: return ResponseBase(400, "usdAmount is required")
        return try {
            ResponseBase(OxaPayRechargeResponse.from(rechargeService.create(userId, amount)))
        } catch (e: IllegalArgumentException) {
            ResponseBase(400, e.message ?: "Invalid recharge amount")
        } catch (e: OxaPayException) {
            ResponseBase(502, e.message ?: "OxaPay invoice creation failed")
        } catch (e: IllegalStateException) {
            ResponseBase(503, e.message ?: "OxaPay is not configured")
        }
    }

    @GetMapping("/api/coins/recharges/{orderId}")
    fun get(
        @CurrentUserId userId: Int?,
        @PathVariable orderId: String,
    ): ResponseBase<OxaPayRechargeResponse> {
        if (userId == null) return ResponseBase(401, "Unauthorized")
        val order = rechargeService.getForUser(orderId, userId)
            ?: return ResponseBase(404, "Recharge order not found")
        return ResponseBase(OxaPayRechargeResponse.from(order))
    }

    @PostMapping(
        "/api/coins/recharges/oxapay/callback",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.TEXT_PLAIN_VALUE],
    )
    fun callback(
        @RequestHeader(name = "HMAC", required = false) signature: String?,
        @RequestBody rawBody: ByteArray,
    ): ResponseEntity<String> {
        if (!webhookVerifier.verify(rawBody, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid signature")
        }
        val callback = try {
            OxaPayCallback.parse(rawBody)
        } catch (e: Exception) {
            return ResponseEntity.badRequest().body("invalid callback")
        }
        if (!callback.status.equals("Paid", ignoreCase = true)) return ResponseEntity.ok("ok")

        return try {
            rechargeService.settlePaidCallback(callback)
            ResponseEntity.ok("ok")
        } catch (e: Exception) {
            log.warn("Failed to settle OxaPay callback: orderId={}, trackId={}", callback.orderId, callback.trackId, e)
            ResponseEntity.badRequest().body("callback rejected")
        }
    }
}
