package ltd.guimc.web.altget.controller

import ltd.guimc.web.altget.annotations.CurrentUserId
import ltd.guimc.web.altget.entity.response.ResponseBase
import ltd.guimc.web.altget.service.coin.CoinTokenService
import ltd.guimc.web.altget.service.coin.UserCoinService
import ltd.guimc.web.altget.service.user.CoreAuthService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class CoinController(
    private val userCoinService: UserCoinService,
    private val coreAuthService: CoreAuthService,
    private val coinTokenService: CoinTokenService
) {
    @GetMapping("/api/coins/transfer")
    fun transfer(@CurrentUserId userId: Int?, targetUserName: String, credits: Int): ResponseBase<String> {
        if (userId == null) return ResponseBase(401, "Unauthorized")
        val userCoin = userCoinService.getById(userId)
        if (userCoin.balance < credits) return ResponseBase(401, "Not enough credits")
        val targetUser = coreAuthService.getByUsername(targetUserName) ?: return ResponseBase(404, "Target user not found")
        userCoinService.transfer(userId, targetUser.userId, credits)
        return ResponseBase("Transfer successful")
    }

    @GetMapping("/api/coins/redeem")
    fun redeem(@CurrentUserId userId: Int?, token: String): ResponseBase<String> {
        if (userId == null) return ResponseBase(401, "Unauthorized")
        val coinToken = coinTokenService.getById(token) ?: return ResponseBase(401, "No valid token found")
        if (coinToken.isUsed) return ResponseBase(401, "Token already redeemed")
        return if (coinTokenService.redeemTokenForUser(token, userId)) {
            ResponseBase("Redeem successful")
        } else {
            ResponseBase(404, "Token could not be redeemed")
        }
    }

    @GetMapping("/api/coins/me")
    fun getMyCoins(@CurrentUserId userId: Int?): ResponseBase<Int> {
        if (userId == null) return ResponseBase(401, "Unauthorized")
        val userCoin = userCoinService.getById(userId) ?: return ResponseBase(404, "User coin data not found")
        return ResponseBase(userCoin.balance)
    }
}