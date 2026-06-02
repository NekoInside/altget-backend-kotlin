package ltd.guimc.web.altget.component

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RedisSessionComponent(
    @Qualifier("customStringRedisTemplate")
    private val stringRedisTemplate: RedisTemplate<String, String>
) {

    companion object {
        private const val SESSION_KEY_PREFIX = "user:session:"
        private val SESSION_TTL: Duration = Duration.ofHours(24)
    }

    /**
     * Store user session ID in Redis.
     * @param userId the user ID
     * @param sessionId the session ID to store
     */
    fun saveSession(userId: Int, sessionId: String) {
        val key = sessionKey(userId)
        stringRedisTemplate.opsForValue().set(key, sessionId, SESSION_TTL)
    }

    /**
     * Get user session ID from Redis.
     * @param userId the user ID
     * @return the session ID, or null if not found
     */
    fun getSession(userId: Int): String? {
        val key = sessionKey(userId)
        return stringRedisTemplate.opsForValue().get(key)
    }

    /**
     * Delete user session ID from Redis.
     * @param userId the user ID
     * @return true if the key was deleted, false otherwise
     */
    fun deleteSession(userId: Int): Boolean {
        val key = sessionKey(userId)
        return stringRedisTemplate.delete(key)
    }

    /**
     * Refresh the TTL of an existing session.
     * @param userId the user ID
     * @return true if the TTL was renewed, false if the session does not exist
     */
    fun refreshSession(userId: Int): Boolean {
        val key = sessionKey(userId)
        return stringRedisTemplate.expire(key, SESSION_TTL)
    }

    /**
     * Check if a user session exists.
     * @param userId the user ID
     * @return true if the session exists, false otherwise
     */
    fun existsSession(userId: Int): Boolean {
        val key = sessionKey(userId)
        return stringRedisTemplate.hasKey(key)
    }

    private fun sessionKey(userId: Int): String = "$SESSION_KEY_PREFIX$userId"
}
