package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

/*
* 数据统计相关接口
* */
@RestController
@RequestMapping("admin/report")
@Api(tags = "数据统计相关接口")
@Slf4j
public class ReportController {

    @Autowired
    private ReportService reportService;

    /*
    * 营业额统计
    * */
    //TurnoverReportVO封装日期和营业额
    @GetMapping("/turnoverStatistics")
    @ApiOperation(value = "营业额统计")
    public Result<TurnoverReportVO> turnoverStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){
        log.info("统计营业额数据:{},{}", begin, end);
        return Result.success(reportService.getTurnoverStatistics(begin, end));
    }

    /*
    * 用户统计
    * */
    @GetMapping("/userStatistics")
    @ApiOperation(value = "用户统计")
    //userReportVO封装日期和用户总量和新增用户
    public Result<UserReportVO> userStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){
        log.info("统计用户数据:{},{}", begin, end);
        return Result.success(reportService.getUserStatistics(begin, end));
    }

    /*
     * 订单统计
     * */
    @GetMapping("/ordersStatistics")
    @ApiOperation(value = "订单统计")
    //orderReportVO封装日期和每日订单数和每日有效订单数，总订单数和总有效订单数和订单完成率
    public Result<OrderReportVO> ordersStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){
        log.info("统计订单数据:{},{}", begin, end);
        return Result.success(reportService.getOrderStatistics(begin, end));
    }

    /*
     * 销量排行top10
     * */
    @GetMapping("/top10")
    @ApiOperation(value = "销量排行top10")
    //SalesTop10ReportVO封装商品名称列表和销量列表
    public Result<SalesTop10ReportVO> salesTop10(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){
        log.info("销量排行top10数据:{},{}", begin, end);
        return Result.success(reportService.getSalesTop10(begin, end));
    }

    /*
    * 导出运营数据报表
    * */
    @GetMapping("/export")
    @ApiOperation("导出运营数据报表")
    public void export(HttpServletResponse response){
        reportService.exportBusinessData(response);
    }
}
