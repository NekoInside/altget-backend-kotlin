package ltd.guimc.web.altget.config

import com.yubico.webauthn.CredentialRepository
import com.yubico.webauthn.RegisteredCredential
import com.yubico.webauthn.data.ByteArray
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor
import ltd.guimc.web.altget.service.passkey.PasskeyCredentialService
import ltd.guimc.web.altget.service.user.CoreAuthService
import org.springframework.stereotype.Component
import java.util.Optional

/**
 * Spring-managed [CredentialRepository] that resolves credentials from the database.
 *
 * The Yubico [com.yubico.webauthn.RelyingParty] calls these methods internally
 * during [com.yubico.webauthn.RelyingParty.finishAssertion] to look up the
 * stored public key needed for signature verification.
 */
@Component
class PasskeyCredentialRepository(
    private val passkeyCredentialService: PasskeyCredentialService,
    private val coreAuthService: CoreAuthService
) : CredentialRepository {

    private val log = org.slf4j.LoggerFactory.getLogger(PasskeyCredentialRepository::class.java)

    /**
     * Returns the set of credential IDs registered for a given username.
     * Used by [com.yubico.webauthn.RelyingParty.startAssertion] when a username
     * hint is provided, so the client can filter available credentials.
     */
    override fun getCredentialIdsForUsername(username: String): MutableSet<PublicKeyCredentialDescriptor> {
        val user = coreAuthService.getByUsername(username)
        if (user == null) {
            log.warn("getCredentialIdsForUsername: user not found for username='{}'", username)
            return mutableSetOf()
        }
        val credentials = passkeyCredentialService.getCredentialsByUserId(user.userId)
        log.info("getCredentialIdsForUsername: found {} credential(s) for username='{}'", credentials.size, username)
        return credentials.map { cred ->
            PublicKeyCredentialDescriptor.builder()
                .id(ByteArray.fromBase64Url(cred.credentialId))
                .build()
        }.toMutableSet()
    }

    /**
     * Returns the user handle for a given username.
     * Used by [com.yubico.webauthn.RelyingParty.startAssertion] to populate
     * the allowCredentials user handle.
     */
    override fun getUserHandleForUsername(username: String): Optional<ByteArray> {
        val user = coreAuthService.getByUsername(username)
        if (user == null) {
            log.warn("getUserHandleForUsername: user not found for username='{}'", username)
            return Optional.empty()
        }
        val userHandle = passkeyCredentialService.getOrCreateUserHandle(user.userId)
        log.info("getUserHandleForUsername: userHandle='{}' for username='{}'", userHandle, username)
        return Optional.of(ByteArray.fromBase64Url(userHandle))
    }

    /**
     * Resolves a username from a user handle (used for discoverable credentials).
     * Called by the Yubico library after signature verification to determine
     * which user authenticated.
     */
    override fun getUsernameForUserHandle(userHandle: ByteArray): Optional<String> {
        val handleB64 = userHandle.base64Url
        val credential = passkeyCredentialService.getByUserHandle(handleB64)
        if (credential == null) {
            log.warn("getUsernameForUserHandle: no credential found for userHandle='{}'", handleB64)
            return Optional.empty()
        }
        val user = coreAuthService.getById(credential.userId)
        if (user == null) {
            log.warn("getUsernameForUserHandle: user not found for userId={}", credential.userId)
            return Optional.empty()
        }
        log.info("getUsernameForUserHandle: resolved username='{}' for userHandle='{}'", user.username, handleB64)
        return Optional.ofNullable(user.username)
    }

    /**
     * Primary credential lookup called by [com.yubico.webauthn.RelyingParty.finishAssertion].
     * Returns the registered credential (with public key) so the server can verify
     * the assertion signature.
     */
    override fun lookup(credentialId: ByteArray, userHandle: ByteArray): Optional<RegisteredCredential> {
        val idB64 = credentialId.base64Url
        log.info("lookup: searching for credentialId='{}'", idB64)
        val entity = lookupEntity(idB64)
        if (entity == null) {
            log.warn("lookup: credential NOT FOUND for credentialId='{}'", idB64)
            return Optional.empty()
        }
        log.info("lookup: credential FOUND for credentialId='{}', userId={}", idB64, entity.userId)
        return Optional.of(toRegisteredCredential(entity))
    }

    /**
     * Fallback credential lookup by credential ID alone.
     */
    override fun lookupAll(credentialId: ByteArray): MutableSet<RegisteredCredential> {
        val idB64 = credentialId.base64Url
        log.info("lookupAll: searching for credentialId='{}'", idB64)
        val entity = lookupEntity(idB64)
        if (entity == null) {
            log.warn("lookupAll: credential NOT FOUND for credentialId='{}'", idB64)
            return mutableSetOf()
        }
        log.info("lookupAll: credential FOUND for credentialId='{}'", idB64)
        return mutableSetOf(toRegisteredCredential(entity))
    }

    private fun lookupEntity(credentialIdB64: String) =
        passkeyCredentialService.getByCredentialId(credentialIdB64)

    private fun toRegisteredCredential(entity: ltd.guimc.web.altget.entity.db.passkey.PasskeyCredentialEntity): RegisteredCredential {
        return RegisteredCredential.builder()
            .credentialId(ByteArray.fromBase64Url(entity.credentialId))
            .userHandle(ByteArray.fromBase64Url(entity.userHandle))
            .publicKeyCose(ByteArray.fromBase64Url(entity.publicKeyCose))
            .signatureCount(entity.signatureCount)
            .build()
    }
}
