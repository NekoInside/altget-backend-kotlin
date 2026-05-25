package ltd.guimc.web.altget.utils

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HexUtils {
    fun hmacSha256(key: String, data: String): String {
        val hmacSha256 = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(key.toByteArray(), "HmacSHA256")
        hmacSha256.init(secretKeySpec)
        val hash = hmacSha256.doFinal(data.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}