package ltd.guimc.web.altget.controller

import ltd.guimc.web.altget.annotations.CurrentUserId
import ltd.guimc.web.altget.entity.response.ResponseBase
import ltd.guimc.web.altget.service.coin.UserCoinService
import ltd.guimc.web.altget.service.user.CoreAuthService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController("/api/coins")
class CoinController(private val userCoinService: UserCoinService, private val coreAuthService: CoreAuthService) {
    @GetMapping("/transfer")
    fun transfer(@CurrentUserId userId: Int?, targetUserName: String, credits: Int): ResponseBase<String> {
        if (userId == null) return ResponseBase(401, "Unauthorized")
        val userCoin = userCoinService.getById(userId)
        if (userCoin.balance < credits) return ResponseBase(401, "Not enough credits")
        val targetUser = coreAuthService.getByUsername(targetUserName) ?: return ResponseBase(404, "Target user not found")
        userCoinService.transfer(userId, targetUser.userId, credits)
        return ResponseBase("Transfer successful")
    }
}