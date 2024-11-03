package com.itheima.rpc.client.boot;

import com.itheima.rpc.client.discovery.RpcServiceDiscovery;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author chenw
 * @version 1.0
 * @description: TODO
 * @date 2024/11/3 下午6:16
 */

@Component
public class RpcClient {
    /*
    * 服务发现
    *   基于zk的sdk从zk中获取数据存入缓存
    *   设置监听--->zk中的数据发生变更后通知，将最新的数据更新到缓存
    * */

    @Resource
    private RpcServiceDiscovery rpcServiceDiscovery;
    public void run() {
        //service discovery  服务的发现和监听变更
        rpcServiceDiscovery.serviceDiscovery();
        //代理生成

    }
}
