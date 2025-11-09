package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.OrderService;
import com.sky.vo.OrderSubmitVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;


    /*
    * 用户下单
    * */
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {

        //处理业务异常情况（地址没有，商品没有）
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if(addressBook == null){
            //抛出业务异常
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);//用户地址为空，不能下单
        }

        Long userId = BaseContext.getCurrentId();//获取当前用户id
        ShoppingCart shoppingCart = new ShoppingCart();//封装到shoppingcart里头
        shoppingCart.setUserId(userId);
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);//使用方法查看购物车里面有没有内容
        if(list == null || list.size() == 0){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);//没有就抛出业务异常
        }

        //向订单表插入一条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,orders);//属性拷贝
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);//支付状态:未支付
        orders.setStatus(Orders.PENDING_PAYMENT);//订单状态:待付款
        orders.setNumber(String.valueOf(System.currentTimeMillis()));//订单号
        orders.setConsignee(addressBook.getConsignee());//收货人
        orders.setUserId(userId);//用户id

        orderMapper.insert(orders);

        List<OrderDetail> orderDetails = new ArrayList<>();//订单明细集合
        //向订单明细表插入n条数据
        for (ShoppingCart cart : list) {
            OrderDetail orderDetail = new OrderDetail();//创建orderdetail对象
            BeanUtils.copyProperties(cart,orderDetail);//属性拷贝
            orderDetail.setOrderId(orders.getId());//设置当前订单明细关联的订单id

            orderDetails.add(orderDetail);//将该订单明细加入订单明细集合
        }
        orderDetailMapper.insertBatch(orderDetails);//批量插入至订单明细表

        //清空用户的购物车数据
        shoppingCartMapper.deleteByUserId(userId);

        //封装为vo对象并返回
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();
        return orderSubmitVO;
    }




}
