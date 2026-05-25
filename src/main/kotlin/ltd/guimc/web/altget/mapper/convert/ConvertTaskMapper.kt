package ltd.guimc.web.altget.mapper.convert

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import ltd.guimc.web.altget.entity.convert.ConvertTask
import org.apache.ibatis.annotations.Mapper

@Mapper
interface ConvertTaskMapper : BaseMapper<ConvertTask> {
}