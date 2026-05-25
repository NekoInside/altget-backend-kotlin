package ltd.guimc.web.altget.service.alt

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import ltd.guimc.web.altget.entity.alt.AltCategory
import ltd.guimc.web.altget.mapper.alt.AltCategoryMapper
import org.springframework.stereotype.Service

@Service
class AltService : ServiceImpl<AltCategoryMapper, AltCategory>() {
}