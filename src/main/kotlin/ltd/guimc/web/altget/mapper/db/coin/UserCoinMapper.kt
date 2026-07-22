package ltd.guimc.web.altget.mapper.db.coin

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import ltd.guimc.web.altget.entity.db.coin.UserCoin
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Update

@Mapper
interface UserCoinMapper : BaseMapper<UserCoin> {
    @Update(
        """UPDATE user_coin
           SET balance = balance + #{amount}, updated_at = CURRENT_TIMESTAMP
           WHERE user_id = #{userId} AND balance <= #{maxBalance}"""
    )
    fun addBalance(
        @Param("userId") userId: Int,
        @Param("amount") amount: Long,
        @Param("maxBalance") maxBalance: Long,
    ): Int

    @Update(
        """UPDATE user_coin
           SET balance = balance - #{amount}, updated_at = CURRENT_TIMESTAMP
           WHERE user_id = #{userId} AND balance >= #{amount}"""
    )
    fun subtractBalance(
        @Param("userId") userId: Int,
        @Param("amount") amount: Long,
    ): Int
}
