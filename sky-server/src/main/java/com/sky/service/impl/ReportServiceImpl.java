package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;

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
        if (totalOrderCount != 0) {
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
        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop(beginTime, endTime);
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

    /*
     * 导出运营数据报表
     * */
    public void exportBusinessData(HttpServletResponse response) {

        //查询数据库，获取营业数据--近30天的数据
        LocalDate dateBgein = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);


        BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(dateBgein, LocalTime.MIN),
                LocalDateTime.of(dateEnd, LocalTime.MAX));

        //通过poi将数据写入excel中
        InputStream in = this.getClass()
                .getClassLoader()
                .getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            //基本模板文件创建一个新的excel文件
            XSSFWorkbook excel = new XSSFWorkbook(in);

            //获取表格文件的sheet页
            XSSFSheet sheet = excel.getSheet("Sheet1");

            //填充数据--时间
            sheet.getRow(1).getCell(1).setCellValue("时间:" + dateBgein + "至" + dateEnd);

            //获得第4行
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());//新用户

            //获得第5行
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());

            //填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = dateBgein.plusDays(i);
                //查询某一天的营业数据
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN),
                        LocalDateTime.of(date, LocalTime.MAX));

                //获取某一行
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());//日期
                row.getCell(2).setCellValue(businessData.getTurnover());//营业额
                row.getCell(3).setCellValue(businessData.getValidOrderCount());//有效订单数
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());//订单完成率
                row.getCell(5).setCellValue(businessData.getUnitPrice());//平均客单价
                row.getCell(6).setCellValue(businessData.getNewUsers());//新增用户数

            }

            //通过输出流将excel文件下载客户端浏览器中
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            //关流
            out.close();
            excel.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
