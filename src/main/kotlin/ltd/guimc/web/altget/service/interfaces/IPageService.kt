package ltd.guimc.web.altget.service.interfaces

import com.baomidou.mybatisplus.core.conditions.Wrapper
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.metadata.IPage
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import com.baomidou.mybatisplus.extension.service.IService

/**
 * 通用分页服务接口
 *
 * 扩展自 MyBatis-Plus 的 [IService]，提供通用分页查询能力。
 * 支持通过传入查询器（[com.baomidou.mybatisplus.core.conditions.query.QueryWrapper] /
 * [com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper]）进行过滤。
 *
 * @param T 实体类型
 */
interface IPageService<T> : IService<T> {

    /**
     * 分页查询（不带过滤条件）
     *
     * @param page 当前页码（从 1 开始）
     * @param size 每页记录数
     * @return MyBatis-Plus 分页对象，包含 [IPage.records]、[IPage.total]、[IPage.pages] 等
     */
    fun getPage(page: Int, size: Int): IPage<T> {
        return getPage(page, size, null)
    }

    /**
     * 分页查询（带过滤条件）
     *
     * @param page    当前页码（从 1 开始）
     * @param size    每页记录数
     * @param wrapper 查询条件构造器，支持 [com.baomidou.mybatisplus.core.conditions.query.QueryWrapper]、
     *                [com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper] 等；
     *                传入 null 或省略表示无条件查询全部
     * @return MyBatis-Plus 分页对象，包含 [IPage.records]、[IPage.total]、[IPage.pages] 等
     */
    fun getPage(page: Int, size: Int, wrapper: Wrapper<T>?): IPage<T> {
        val pageObj = Page<T>(page.toLong(), size.toLong())
        return page(pageObj, wrapper ?: QueryWrapper())
    }
}
