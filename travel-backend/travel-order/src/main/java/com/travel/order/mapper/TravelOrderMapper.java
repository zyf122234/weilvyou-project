package com.travel.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.order.entity.TravelOrder;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface TravelOrderMapper extends BaseMapper<TravelOrder> {

    @Delete("""
            DELETE FROM travel_order
            WHERE status = #{status}
              AND update_time <= #{deadline}
            """)
    int deleteCanceledBefore(@Param("status") Integer status, @Param("deadline") LocalDateTime deadline);
}
