package ltd.guimc.web.altget.entity.response.user

data class CoinTokenResponse(
    val id: String,
    val coinAmount: Int,
    val createdBy: Int?,
    val createdAt: String?,
    val redeemedBy: Int?,
    val redeemedAt: String?,
    val isUsed: Boolean
)
