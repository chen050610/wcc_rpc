package com.itheima.rpc.client.spring;

import com.itheima.rpc.annotation.HrpcRemote;
import com.itheima.rpc.client.proxy.RequestProxyFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Field;

/**
 * @author chenw
 * @version 1.0
 * @description: TODO
 * @date 2024/11/3 下午7:00
 */

@Slf4j
@Component
public class RpcAnnotationProcessor implements BeanPostProcessor {

    @Resource
    private RequestProxyFactory proxyFactory;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        //对bean进扩展 查看字段中是否有自定义的注解
        //获取该bean的所有的field
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            try {
                HrpcRemote annotation = field.getAnnotation(HrpcRemote.class);
                if (annotation!=null){
                    if (!field.isAccessible()){
                        field.setAccessible(true);
                    }
                    //基于该field进行代理的生成
                    Object proxy = proxyFactory.newProxyInstance(field.getType());
                    if (proxy!=null){
                        //生成的代理注入到属性上
                        field.set(bean,proxy);
                    }
                }
            } catch (Exception e){
                log.error("failed to inject proxy , field={}",field.getName());
            }
        }
        return bean;
    }
}
