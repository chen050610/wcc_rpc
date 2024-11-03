package com.itheima.rpc.server.boot;

import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * @author chenw
 * @version 1.0
 * @description: TODO
 * @date 2024/11/3 上午8:53
 */

@Configuration
public class RpcServerBootstrap {

    @Resource
    private RpcServerRunner rpcServerRunner;
    /*
    * rpc server的程序入口
    * */
    @PostConstruct
    public void initRpcServer(){
        //入口
        rpcServerRunner.run();
    }

}
