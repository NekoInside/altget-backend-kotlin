package ltd.guimc.web.altget.mapper.user

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import ltd.guimc.web.altget.entity.user.UserOauth
import org.apache.ibatis.annotations.Mapper

@Mapper
interface UserOauthMapper : BaseMapper<UserOauth> {
}