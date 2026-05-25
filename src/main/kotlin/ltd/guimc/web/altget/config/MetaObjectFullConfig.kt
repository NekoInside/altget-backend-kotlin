package ltd.guimc.web.altget.config

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler
import org.apache.ibatis.reflection.MetaObject
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class MetaObjectFullConfig : MetaObjectHandler {

    override fun insertFill(metaObject: MetaObject) {
        this.strictInsertFill(metaObject, "createdAt", { LocalDateTime.now() }, LocalDateTime::class.java)
        this.strictInsertFill(metaObject, "updatedAt", { LocalDateTime.now() }, LocalDateTime::class.java)
    }

    override fun updateFill(metaObject: MetaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", { LocalDateTime.now() }, LocalDateTime::class.java)
    }
}