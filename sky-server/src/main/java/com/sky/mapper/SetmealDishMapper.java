package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /*
    * 根据菜品id查找套餐id
    * */
    List<Long> getSetmealDishIds(List<Long> dishIds);
}
