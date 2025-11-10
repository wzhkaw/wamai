package com.sky.service;

import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.vo.OrderSubmitVO;

public interface OrderService {

    /*
    * 用户下单
    * */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /*
    * 历史订单查询
    * */
    PageResult pageQuery4User(int page, int pageSize, Integer status);
}
