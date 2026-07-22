package ltd.guimc.web.altget.service.cookie

import ltd.guimc.web.altget.enum.EnumTransactionType
import ltd.guimc.web.altget.service.coin.CoinTransactionHistoryService
import ltd.guimc.web.altget.service.coin.UserCoinService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CookieConvertBillingService(
    private val userCoinService: UserCoinService,
    private val coinTransactionHistoryService: CoinTransactionHistoryService,
) {
    @Transactional
    fun charge(userId: Int) {
        if (!userCoinService.subtractBalance(userId, 1)) {
            throw CookieConvertBalanceException()
        }
        coinTransactionHistoryService.logTransaction(userId, -1, EnumTransactionType.COOKIE_CONVERT)
    }

    @Transactional
    fun refund(userId: Int) {
        check(userCoinService.addBalance(userId, 1)) { "Unable to refund cookie conversion coin" }
        coinTransactionHistoryService.logTransaction(userId, 1, EnumTransactionType.COOKIE_CONVERT)
    }
}

class CookieConvertBalanceException : RuntimeException("余额不足")
