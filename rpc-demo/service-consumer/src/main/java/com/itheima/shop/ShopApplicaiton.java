package com.itheima.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @description
 * @author: ts
 * @create:2021-05-10 11:34
 */
@SpringBootApplication(scanBasePackages = {"com.itheima.shop","com.itheima.rpc"})
// @ComponentScan({"com.itheima.shop","com.itheima.rpc"})
public class ShopApplicaiton {

    public static void main(String[] args) {
        SpringApplication.run(ShopApplicaiton.class,args);
    }
}
