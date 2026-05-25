package ltd.guimc.web.altget.service

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl

open class EasyServiceImpl<T> : ServiceImpl<BaseMapper<T>, T>() {
}