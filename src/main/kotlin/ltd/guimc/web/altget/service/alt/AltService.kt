package ltd.guimc.web.altget.service.alt

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import ltd.guimc.web.altget.entity.db.alt.AltCategory
import ltd.guimc.web.altget.mapper.db.alt.AltCategoryMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AltService : ServiceImpl<AltCategoryMapper, AltCategory>() {
    @Transactional(rollbackFor = [Exception::class])
    fun fetchAlt(count: Int = 1, channel: String = "default"): List<AltCategory> {
        val popupData = baseMapper.popupByChannel(channel, count)
        removeBatchByIds(popupData.map { it.id })
        return popupData
    }
}