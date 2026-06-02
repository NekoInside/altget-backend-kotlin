package ltd.guimc.web.altget.entity.db.passkey

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import java.time.LocalDateTime

@TableName("passkey_credential")
class PasskeyCredentialEntity {
    @TableId(type = IdType.AUTO)
    var id: Int = 0

    var userId: Int = 0

    var credentialId: String = ""

    var publicKeyCose: String = ""

    var signatureCount: Long = 0

    var userHandle: String = ""

    var credentialName: String = "My Passkey"

    var discoverable: Boolean = false

    var createdAt: LocalDateTime = LocalDateTime.now()
}
