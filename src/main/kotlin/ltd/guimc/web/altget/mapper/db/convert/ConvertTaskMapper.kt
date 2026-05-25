package ltd.guimc.web.altget.mapper.db.convert

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import ltd.guimc.web.altget.entity.db.convert.ConvertTask
import org.apache.ibatis.annotations.Mapper

@Mapper
interface ConvertTaskMapper : BaseMapper<ConvertTask> {
}