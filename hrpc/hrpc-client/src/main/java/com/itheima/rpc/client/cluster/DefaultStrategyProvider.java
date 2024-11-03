package com.itheima.rpc.client.cluster;

import com.itheima.rpc.annotation.HrpcLoadBalance;
import com.itheima.rpc.client.config.RpcClientConfiguration;
import io.protostuff.Rpc;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @author chenw
 * @version 1.0
 * @description: TODO
 * @date 2024/11/3 下午11:08
 */
@Component
public class DefaultStrategyProvider implements StartegyProvider, ApplicationContextAware {
    @Resource
    private RpcClientConfiguration configuration;

    LoadBalanceStrategy loadBalanceStrategy;


    @Override
    public LoadBalanceStrategy getStrategy() {
        return loadBalanceStrategy;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, Object> map =
                applicationContext.getBeansWithAnnotation(HrpcLoadBalance.class);
        for (Object bean : map.values()) {
            HrpcLoadBalance annotation = bean.getClass().getAnnotation(HrpcLoadBalance.class);
            if (annotation.strategy().equalsIgnoreCase(configuration.getRpcClientClusterStrategy())){
                loadBalanceStrategy = (LoadBalanceStrategy) bean;
                break;
            }
        }
    }
}
