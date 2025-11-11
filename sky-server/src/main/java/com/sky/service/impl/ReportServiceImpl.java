package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.alibaba.fastjson.util.JavaBeanInfo.build;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    /*
     * 统计指定区间内的营业额数据
     * */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {

        List<LocalDate> dateList = new ArrayList();//创建集合存入时间段中的每一个日期
        dateList.add(begin);
        while (!begin.equals(end)) {//将日期中的每一天都加入集合中
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Double> turnoverList = new ArrayList<>();//用于存放每天的营业额
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);//算出当天的开始时间00：00
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);//算出当天的结束时间23：599999

            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);//订单状态为已完成
            //select sum(amount) from orders where order_time > begin and order_time < end and status = 5
            Double turnover = orderMapper.sumByMap(map);//计算出当天的营业额
            turnover = turnover == null ? 0.0 : turnover;//如果营业额为0，将null转换为0.0
            turnoverList.add(turnover);
        }

        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))//用，分割每个日期（格式是前端要求返回的
                .turnoverList(StringUtils.join(turnoverList, ","))//同样用，分割
                .build();
    }

    /*
    * 统计指定时间区间内的用户数据
    * */
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {

        List<LocalDate> dateList = new ArrayList();//创建集合存入时间段中的每一个日期
        dateList.add(begin);
        while (!begin.equals(end)) {//将日期中的每一天都加入集合中
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> newUserList = new ArrayList<>();//存放当天用户数量
        List<Integer> totalUserList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("end", endTime);
            //select count(id) from user where create_time < end
            Integer totalUser = userMapper.countByMap(map);//查询总用户数量

            map.put("begin", beginTime);
            //select count(id) from user where create_time > begin and create_time < end
            Integer newUser = userMapper.countByMap(map);//查询新用户数量

            newUserList.add(newUser);
            totalUserList.add(totalUser);
        }


        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))//用，分隔数据返回给前端
                .newUserList(StringUtils.join(newUserList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .build();
    }

    /*
     * 统计指定时间区间内的订单数据
     * */
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {

        List<LocalDate> dateList = new ArrayList();//创建集合存入时间段中的每一个日期
        dateList.add(begin);
        while (!begin.equals(end)) {//将日期中的每一天都加入集合中
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> orderCountList = new ArrayList<>();//存储每日订单数
        List<Integer> validOrderCountList = new ArrayList<>();//存储每日有效订单数

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);

            //select count(id) from orders where order_time > begin and order_time < end
            Integer orderCount = orderMapper.countByMap(map);//每日订单数
            map.put("status", Orders.COMPLETED);//订单状态为已完成
            //select count(id) from orders where order_time > begin and order_time < end and status = 5 //已完成
            Integer validOrderCount = orderMapper.countByMap(map);//每日有效订单数

            orderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);
        }

        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();//获取订单总数量
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();//获取订单总有效数量
        Double orderComppletionRate = 0.0;
        if(totalOrderCount != 0){
            orderComppletionRate = validOrderCount.doubleValue() / totalOrderCount;//计算订单完成率
        }

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))//用，分隔数据返回给前端
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .orderCompletionRate(orderComppletionRate)
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .build();
    }

    /*
    * 统计指定时间区间销量排行top10
    * */
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {

        //转换localdate对象为localdatetime对象
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        //得到top10的数据：菜名和数量
        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop(beginTime,endTime);
        //取出菜名并用，分隔
        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameList = StringUtils.join(names, ",");
        //取出数量并用，分隔
        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numberList = StringUtils.join(numbers, ",");

        //返回VO对象
        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }

}
