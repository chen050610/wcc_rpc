package com.itheima.shop.controller.order;

import com.itheima.rpc.annotation.HrpcRemote;
import com.itheima.shop.order.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description
 * @author: ts
 * @create:2021-05-10 11:30
 */
@RestController
@RequestMapping("/order")
public class OrderController {


    @HrpcRemote
    private OrderService orderService;

    /**
     * 获取订单信息
     * @param userId
     * @param orderNo
     * @return
     */
    @GetMapping("/getOrder")
    public String getOrder(String userId,String orderNo) {
        return orderService.getOrder(userId,orderNo);
    }

}
