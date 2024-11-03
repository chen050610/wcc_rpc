package com.itheima.rpc.server.boot;

import com.itheima.rpc.server.registry.RpcRegister;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author chenw
 * @version 1.0
 * @description: TODO
 * @date 2024/11/3 上午9:00
 */

@Component
public class RpcServerRunner {

    /** 
     * @description: rpc 程序的入口 完成 rpc server 端的
     * @param:
     * @return: void 
     * @author chenw
     * @date: 2024/11/3 上午9:01
     */
    /*
    * 1.完成接口服务的注册
    *   q1:那些接口需要对外注册
    *       rpc server 框架提供注解 开发者使用注解 标注到要对外暴露的接口实现 rpc server 扫描注解 进行相关的操作
    *   q2:如何注册 ---> 向注册中心写数据(zookeeper)
    *     答案 基于zookeeper的api完成数据的写入 引入zk的sdk来完成即可(curator)
    *
    * 2.基于netty编写一个服务端的程序
    *       core1:编写handler
    *           1.处理数据入栈的handler:协议解析 数据反序列化 处理请求(调用业务)
    *           2.出口数据出栈的handler:数据反序列化 协议编码
    * */
    @Resource
    private RpcRegister rpcRegister;
    @Resource
    private RpcServer rpcServer;

    public void run() {
        //server register
        rpcRegister.serverRegister();
        //start server by netty
        rpcServer.start();

    }
}
