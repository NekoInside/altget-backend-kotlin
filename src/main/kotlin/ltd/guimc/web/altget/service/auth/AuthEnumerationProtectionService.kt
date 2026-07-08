package ltd.guimc.web.altget.service.auth

import com.nimbusds.srp6.SRP6CryptoParams
import com.nimbusds.srp6.SRP6ServerSession
import ltd.guimc.web.altget.component.JwtTokenComponent
import ltd.guimc.web.altget.config.SiteProperities
import ltd.guimc.web.altget.entity.request.auth.RegisterRequest
import org.springframework.stereotype.Service
import java.math.BigInteger
import java.security.MessageDigest
import java.util.Locale

@Service
class AuthEnumerationProtectionService(
    private val jwtTokenComponent: JwtTokenComponent,
    private val siteProperities: SiteProperities
) {
    fun simulateRegister(request: RegisterRequest) {
        val normalizedUsername = request.username.lowercase(Locale.ROOT)
        val pseudoSaltHex = generateFakeSalt(normalizedUsername + ":register")
        val pseudoVerifierHex = derivePseudoVerifierHex(request.email, normalizedUsername)
        generatePseudoSrpChallenge(normalizedUsername, pseudoSaltHex, pseudoVerifierHex)
    }

    fun simulateForgotPassword(email: String) {
        jwtTokenComponent.generatePasswordResetToken(email.lowercase(Locale.ROOT))
        val pseudoUsername = email.lowercase(Locale.ROOT)
        val pseudoSaltHex = generateFakeSalt(pseudoUsername + ":forgot")
        val pseudoVerifierHex = derivePseudoVerifierHex(email, pseudoUsername)
        generatePseudoSrpChallenge(pseudoUsername, pseudoSaltHex, pseudoVerifierHex)
    }

    private fun generateFakeSalt(subject: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = subject + siteProperities.fakeAccountSalt
        return digest.digest(input.toByteArray(Charsets.UTF_8)).toHexString()
    }

    private fun derivePseudoVerifierHex(seed: String, subject: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val material = buildString {
            append(seed)
            append(':')
            append(subject)
            append(':')
            append(siteProperities.fakeAccountSalt)
        }
        val hash = digest.digest(material.toByteArray(Charsets.UTF_8))
        return hash.toHexString().padStart(512, '0')
    }

    private fun generatePseudoSrpChallenge(username: String, saltHex: String, verifierHex: String): String {
        val config = SRP6CryptoParams.getInstance(2048, "SHA-256")
        val serverSession = SRP6ServerSession(config)
        val salt = BigInteger(saltHex, 16)
        val verifier = BigInteger(verifierHex, 16)
        return serverSession.step1(username, salt, verifier).toString(16)
    }

    private fun ByteArray.toHexString(): String = joinToString(separator = "") { "%02x".format(it) }
}