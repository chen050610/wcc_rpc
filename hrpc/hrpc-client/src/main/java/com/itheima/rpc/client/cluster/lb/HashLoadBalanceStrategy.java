package com.itheima.rpc.client.cluster.lb;

import com.itheima.rpc.annotation.HrpcLoadBalance;
import com.itheima.rpc.client.cluster.LoadBalanceStrategy;
import com.itheima.rpc.provider.ServiceProvider;
import com.itheima.rpc.util.IpUtil;

import java.util.List;

/**
 * @author chenw
 * @version 1.0
 * @description: TODO
 * @date 2024/11/3 下午10:43
 */

//客户端hash
@HrpcLoadBalance("hash")
public class HashLoadBalanceStrategy implements LoadBalanceStrategy {
    static String ip = IpUtil.getRealIp();
    @Override
    public ServiceProvider select(List<ServiceProvider> serviceProviders) {
        int hashIp = ip.hashCode();
        int index = Math.abs(hashIp % serviceProviders.size());
        return serviceProviders.get(index);
    }
}
