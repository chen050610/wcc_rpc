package com.itheima.rpc.client.cluster.lb;

import com.itheima.rpc.annotation.HrpcLoadBalance;
import com.itheima.rpc.client.cluster.LoadBalanceStrategy;
import com.itheima.rpc.provider.ServiceProvider;
import com.itheima.rpc.util.IpUtil;
import org.apache.commons.lang3.RandomUtils;

import java.util.List;

/**
 * @author chenw
 * @version 1.0
 * @description: TODO
 * @date 2024/11/3 下午10:43
 */

//随机
@HrpcLoadBalance(strategy = "random")
public class RandomLoadBalanceStrategy implements LoadBalanceStrategy {
    @Override
    public ServiceProvider select(List<ServiceProvider> serviceProviders) {
        int index = RandomUtils.nextInt(0, serviceProviders.size());
        return serviceProviders.get(index);
    }
}
