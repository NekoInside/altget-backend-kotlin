package ltd.guimc.web.altget.service.alt

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import ltd.guimc.web.altget.entity.db.alt.UsedAltCategory
import ltd.guimc.web.altget.mapper.db.alt.UsedAltCategoryMapper
import ltd.guimc.web.altget.service.interfaces.IPageService
import org.springframework.stereotype.Service

@Service
class UsedAltCategoryService : ServiceImpl<UsedAltCategoryMapper, UsedAltCategory>(), IPageService<UsedAltCategory>
