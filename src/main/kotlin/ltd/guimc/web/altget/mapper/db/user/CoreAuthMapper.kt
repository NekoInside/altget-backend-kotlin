package ltd.guimc.web.altget.mapper.db.user

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import ltd.guimc.web.altget.entity.db.user.CoreAuth
import org.apache.ibatis.annotations.Mapper

@Mapper
interface CoreAuthMapper : BaseMapper<CoreAuth> {
}