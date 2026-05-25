package ltd.guimc.web.altget.mapper.user

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import ltd.guimc.web.altget.entity.user.UserApi
import org.apache.ibatis.annotations.Mapper

@Mapper
interface UserApiMapper : BaseMapper<UserApi> {
}