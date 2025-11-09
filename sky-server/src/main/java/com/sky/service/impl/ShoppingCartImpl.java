package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ShoppingCartImpl implements ShoppingCartService {


    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    /*
    * 添加购物车
    * */
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {


        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO,shoppingCart);

        //取得当前用户id
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);

        //判断当前添加的商品是否在购物车中已经存在了
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);

        //如果存在了只需要将数量加1
        if(list != null && list.size()>0){//在这里查找到的数据最多只可能是一条数据
            ShoppingCart cart = list.get(0);
            cart.setNumber(cart.getNumber()+1);//update from shoppingcart set number = ? where id = ?
            shoppingCartMapper.updateNumberById(cart);
        }else{
            //不存在就插入一条数据进数据库

            //判断要插入的是菜品还是套餐
            Long dishId = shoppingCartDTO.getDishId();
            if(dishId!=null){
                //添加菜品
                Dish dish = dishMapper.getById(dishId);
                shoppingCart.setName(dish.getName());//设置属性
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());

                //插入数据到购物车

            }else{
                //添加套餐
                Long setmealId = shoppingCart.getSetmealId();//获取setmealid
                Setmeal setmeal = setmealMapper.getById(setmealId);//根据setmealid查询setmeal

                shoppingCart.setName(setmeal.getName());//设置属性
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());

            }
            //相同属性
            shoppingCart.setNumber(1);//数量为1
            shoppingCart.setCreateTime(LocalDateTime.now());

            //插入数据进购物车
            shoppingCartMapper.insert(shoppingCart);
        }

    }

    /*
    * 查看购物车
    * */
    public List<ShoppingCart> showShoppingCart() {

        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        return list;

    }

    /*
    * 清空购物车
    * */
    public void cleanShoppingCart() {
        Long userId = BaseContext.getCurrentId();
        shoppingCartMapper.deleteByUserId(userId);

    }


}
