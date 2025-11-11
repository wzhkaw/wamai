package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.entity.User;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface UserMapper {


    /*
    * 根据openid查找用户
    * */
    @Select("select * from sky_take_out.user where openid = #{openid}")
    User getByOpenId(String openid);

    /*
    * 插入数据
    * */
    @AutoFill(value = OperationType.INSERT)
    void insert(User user);

    /*
    * 根据动态条件查询用户数量（新用户/总用户）
    * */
    Integer countByMap(Map map);



}
