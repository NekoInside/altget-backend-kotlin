package ltd.guimc.web.altget.mapper.user

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import ltd.guimc.web.altget.entity.user.CoreAuth
import org.apache.ibatis.annotations.Mapper

@Mapper
interface CoreAuthMapper : BaseMapper<CoreAuth> {
}