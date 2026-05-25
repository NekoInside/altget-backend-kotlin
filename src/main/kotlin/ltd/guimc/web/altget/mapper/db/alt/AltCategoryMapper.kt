package ltd.guimc.web.altget.mapper.db.alt

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import ltd.guimc.web.altget.entity.db.alt.AltCategory
import org.apache.ibatis.annotations.Mapper

@Mapper
interface AltCategoryMapper : BaseMapper<AltCategory> {
}