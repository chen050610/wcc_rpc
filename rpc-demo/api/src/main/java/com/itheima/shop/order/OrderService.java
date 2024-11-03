package com.itheima.shop.order;

/**
 * @description
 * @author: ts
 * @create:2021-05-10 10:55
 */
public interface OrderService {

    /**
     * 获取订单描述信息
     * @param userId
     * @param orderNo
     * @return
     */
    String getOrder(String userId, String orderNo);
}
