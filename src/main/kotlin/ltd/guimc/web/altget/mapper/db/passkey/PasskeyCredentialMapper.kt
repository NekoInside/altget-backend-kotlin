package ltd.guimc.web.altget.mapper.db.passkey

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import ltd.guimc.web.altget.entity.db.passkey.PasskeyCredentialEntity
import org.apache.ibatis.annotations.Mapper

@Mapper
interface PasskeyCredentialMapper : BaseMapper<PasskeyCredentialEntity>
