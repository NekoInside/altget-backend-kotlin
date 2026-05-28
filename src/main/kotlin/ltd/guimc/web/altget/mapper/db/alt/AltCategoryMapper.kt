package ltd.guimc.web.altget.mapper.db.alt

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import ltd.guimc.web.altget.entity.db.alt.AltCategory
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Select

@Mapper
interface AltCategoryMapper : BaseMapper<AltCategory> {
    @Select("""
        SELECT * from alt_category
        WHERE channel = #{channel}
        ORDER BY createdAt DESC
        LIMIT #{count} FOR UPDATE SKIP LOCKED
    """)
    fun popupByChannel(channel: String, count: Int): List<AltCategory>
}