package ltd.guimc.web.altget.service.passkey

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import ltd.guimc.web.altget.entity.db.passkey.PasskeyCredentialEntity
import ltd.guimc.web.altget.mapper.db.passkey.PasskeyCredentialMapper
import org.springframework.stereotype.Service

@Service
class PasskeyCredentialService : ServiceImpl<PasskeyCredentialMapper, PasskeyCredentialEntity>() {

    /**
     * Get a passkey credential by its WebAuthn credential ID (base64url).
     */
    fun getByCredentialId(credentialId: String): PasskeyCredentialEntity? {
        return lambdaQuery()
            .eq(PasskeyCredentialEntity::credentialId, credentialId)
            .one()
    }

    /**
     * Get all passkey credentials for a user.
     */
    fun getCredentialsByUserId(userId: Int): List<PasskeyCredentialEntity> {
        return lambdaQuery()
            .eq(PasskeyCredentialEntity::userId, userId)
            .list()
    }

    /**
     * Get or create a stable user handle for a user.
     * Returns the base64url-encoded user handle string.
     */
    fun getOrCreateUserHandle(userId: Int): String {
        val existing = getCredentialsByUserId(userId)
        if (existing.isNotEmpty()) {
            return existing.first().userHandle
        }
        // Generate a new user handle: 64 random bytes, base64url encoded
        val randomBytes = ByteArray(64)
        java.security.SecureRandom().nextBytes(randomBytes)
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)
    }

    /**
     * Update the signature counter for a passkey credential.
     */
    fun updateSignatureCount(id: Int, newCount: Long) {
        val entity = getById(id) ?: return
        entity.signatureCount = newCount
        updateById(entity)
    }

    /**
     * Delete a passkey credential, verifying it belongs to the given user.
     * Returns true if deleted, false if not found or not owned by the user.
     */
    fun deleteCredential(id: Int, userId: Int): Boolean {
        val entity = getById(id) ?: return false
        if (entity.userId != userId) return false
        return removeById(id)
    }
}
