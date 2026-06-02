package ltd.guimc.web.altget.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig {

    @Bean("customStringRedisTemplate")
    fun stringRedisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, String> {
        val template = RedisTemplate<String, String>()
        template.setConnectionFactory(connectionFactory)
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = StringRedisSerializer()
        template.hashKeySerializer = StringRedisSerializer()
        template.hashValueSerializer = StringRedisSerializer()
        template.afterPropertiesSet()
        return template
    }

    @Bean("objectRedisTemplate")
    fun objectRedisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.setConnectionFactory(connectionFactory)
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = StringRedisSerializer()
        template.hashKeySerializer = StringRedisSerializer()
        template.hashValueSerializer = StringRedisSerializer()
        template.afterPropertiesSet()
        return template
    }
}
