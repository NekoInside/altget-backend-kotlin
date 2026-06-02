package ltd.guimc.web.altget.config

import com.yubico.webauthn.CredentialRepository
import com.yubico.webauthn.RegisteredCredential
import com.yubico.webauthn.data.ByteArray
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor
import java.util.Optional

/**
 * Minimal [CredentialRepository] implementation.
 *
 * The RelyingParty builder requires a CredentialRepository, but this application
 * resolves credentials directly via [ltd.guimc.web.altget.service.passkey.PasskeyCredentialService]
 * rather than through the Yubico CredentialRepository interface.
 */
class PasskeyCredentialRepository : CredentialRepository {
    override fun getCredentialIdsForUsername(username: String): MutableSet<PublicKeyCredentialDescriptor> {
        return mutableSetOf()
    }

    override fun getUserHandleForUsername(username: String): Optional<ByteArray> {
        return Optional.empty()
    }

    override fun getUsernameForUserHandle(userHandle: ByteArray): Optional<String> {
        return Optional.empty()
    }

    override fun lookup(credentialId: ByteArray, userHandle: ByteArray): Optional<RegisteredCredential> {
        return Optional.empty()
    }

    override fun lookupAll(credentialId: ByteArray): MutableSet<RegisteredCredential> {
        return mutableSetOf()
    }
}
