package com.itheima.rpc.client.boot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * @author chenw
 * @version 1.0
 * @description: TODO
 * @date 2024/11/3 下午6:15
 */

@Configuration
public class RpcBootstrap {
    @Autowired
    private RpcClient rpcClient;

    @PostConstruct
    public void initRpcClient(){
        rpcClient.run();
    }
}
