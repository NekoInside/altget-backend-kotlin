package ltd.guimc.web.altget.service.passkey

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

/**
 * Caches WebAuthn challenge JSON strings in Redis with a short TTL.
 */
@Service
class WebAuthnChallengeCacheService(
    @Qualifier("stringRedisTemplate")
    private val stringRedisTemplate: RedisTemplate<String, String>
) {

    companion object {
        private const val KEY_PREFIX = "webauthn:challenge:"
        private val TTL: Duration = Duration.ofMinutes(5)
    }

    /**
     * Store a challenge JSON string.
     */
    fun storeChallenge(challengeId: String, serializedJson: String) {
        stringRedisTemplate.opsForValue().set("$KEY_PREFIX$challengeId", serializedJson, TTL)
    }

    /**
     * Consume (get and delete) a challenge JSON string.
     * Returns null if the challenge does not exist or has expired.
     */
    fun consumeChallenge(challengeId: String): String? {
        val key = "$KEY_PREFIX$challengeId"
        val value = stringRedisTemplate.opsForValue().getAndDelete(key)
        return value
    }
}
