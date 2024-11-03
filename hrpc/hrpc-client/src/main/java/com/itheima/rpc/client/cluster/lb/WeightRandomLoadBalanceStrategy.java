package com.itheima.rpc.client.cluster.lb;

import com.itheima.rpc.annotation.HrpcLoadBalance;
import com.itheima.rpc.client.cluster.LoadBalanceStrategy;
import com.itheima.rpc.provider.ServiceProvider;
import org.apache.commons.lang3.RandomUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author chenw
 * @version 1.0
 * @description: TODO
 * @date 2024/11/3 下午10:43
 */

//使用权重
@HrpcLoadBalance(strategy = "polling")
public class WeightRandomLoadBalanceStrategy implements LoadBalanceStrategy {
    @Override
    public ServiceProvider select(List<ServiceProvider> serviceProviders) {
        ArrayList<ServiceProvider> newList = new ArrayList<>();
        for (ServiceProvider serviceProvider : serviceProviders) {
            int weight = serviceProvider.getWeight();
            for (int i = 0; i < weight; i++) {
                newList.add(serviceProvider);
            }
        }
        int index = RandomUtils.nextInt(0,newList.size());
        return newList.get(index);
    }
}
