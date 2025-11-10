package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {


    /*
    * 插入订单数据
    * */
    void insert(Orders orders);

    /*
    * 分页条件查询并按下单时间排序
    * */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);


    /*
    * 根据订单状态和下单时间查询订单
    * */
    @Select("select * from sky_take_out.orders where status = #{status} and order_time < #{orderTime}")
    List<Orders> getByStatusAndOrderTimeLT(Integer status, LocalDateTime orderTime);

    /*
    * 修改订单
    * */
    void update(Orders orders);
}
