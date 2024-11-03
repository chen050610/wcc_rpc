package com.itheima.rpc.client.discovery.zk;

import com.google.common.cache.LoadingCache;
import com.itheima.rpc.cache.ServiceProviderCache;
import com.itheima.rpc.client.discovery.RpcServiceDiscovery;
import com.itheima.rpc.provider.ServiceProvider;
import com.sun.jmx.defaults.ServiceName;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author chenw
 * @version 1.0
 * @description: TODO
 * @date 2024/11/3 下午6:20
 */

@Component
@Slf4j
public class ZKServiceDiscovery implements RpcServiceDiscovery {
    @Resource
    private ClientZKit clientZKit;
    @Resource
    private ServiceProviderCache cache;

    @Override
    public void serviceDiscovery() {
        //拉取所有的服务列表 获取根节点下所有的children
        List<String> serviceList = clientZKit.getServiceList();
        if (serviceList!=null && !serviceList.isEmpty()){
            for (String service : serviceList) {
                //service就是接口的全限定名字
                //获取每个服务下面的ip
                List<ServiceProvider> providers = clientZKit.getServiceInfos(service);
                //将接口以及对应的提供者列表的信息进行存储 -- cache
                cache.put(service,providers); //key = 接口名称 value = providers的列表
                log.info("订阅的接口{},提供者列表{}", service,providers);
                //订阅接口的变更 使用zk的监听机制 调用zk的api监听 如果变更 就会触发回调 在回调的里面我们修改缓存
                clientZKit.subscribeZKEvent(service);
            }
        }
    }
}
