package com.itheima.rpc.client.cluster.lb;

import com.itheima.rpc.annotation.HrpcLoadBalance;
import com.itheima.rpc.client.cluster.LoadBalanceStrategy;
import com.itheima.rpc.provider.ServiceProvider;
import org.apache.commons.lang3.RandomUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author chenw
 * @version 1.0
 * @description: TODO
 * @date 2024/11/3 下午10:43
 */

//轮询
@HrpcLoadBalance(strategy = "polling")
public class PollingLoadBalanceStrategy implements LoadBalanceStrategy {
    //全局的指针
    AtomicInteger nextCount = new AtomicInteger(0);
    @Override
    public ServiceProvider select(List<ServiceProvider> serviceProviders) {
        int index = incrementAndGet(serviceProviders.size());
        return serviceProviders.get(index);
    }

    //进行cas的操作
    private int incrementAndGet(int size) {
        //cas的操作 防止线程安全问题
        for (;;){
            int cur = nextCount.intValue();
            int next = (cur+1) % size;
            if (nextCount.compareAndSet(cur,next)){
                //如果cas成功的话就会返回
                return cur;
            }
        }
    }
}
