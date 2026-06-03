package ltd.guimc.web.altget.config

import ltd.guimc.web.altget.component.CurrentUserIdArgumentResolver
import ltd.guimc.web.altget.component.RealIPArgumentResolver
import ltd.guimc.web.altget.component.RequestInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val requestInterceptor: RequestInterceptor,
    private val currentUserIdArgumentResolver: CurrentUserIdArgumentResolver,
    private val realIPArgumentResolver: RealIPArgumentResolver
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(requestInterceptor)
    }

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(currentUserIdArgumentResolver)
        resolvers.add(realIPArgumentResolver)
    }
}
