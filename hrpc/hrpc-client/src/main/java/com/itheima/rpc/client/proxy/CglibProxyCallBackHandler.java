package com.itheima.rpc.client.proxy;

import com.itheima.rpc.client.request.RpcRequestManager;
import com.itheima.rpc.data.RpcRequest;
import com.itheima.rpc.data.RpcResponse;
import com.itheima.rpc.exception.RpcException;
import com.itheima.rpc.spring.SpringBeanFactory;
import com.itheima.rpc.util.RequestIdUtil;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

/**
 * @description
 * @author: ts
 * @create:2021-05-12 00:11
 */
@Slf4j
public class CglibProxyCallBackHandler implements MethodInterceptor {


    public Object intercept(Object o, Method method, Object[] parameters, MethodProxy methodProxy) throws Throwable {
        log.info("代理调用拦截 method={}",method.getName());
        //rpc获取调用的结果 然后返回结果
        //1.封装请求
        String requestId = RequestIdUtil.requestId(); //雪花算法生成唯一的请求id
        RpcRequest request = RpcRequest.builder()
                .requestId(requestId)
                .className(method.getDeclaringClass().getName()) //获取方法的接口名称
                .methodName(method.getName())
                .parameterTypes(method.getParameterTypes())
                .parameters(parameters)
                .build();
        //2.发送请求获取响应
        RpcRequestManager requestManager = SpringBeanFactory.getBean(RpcRequestManager.class);
        if (requestManager == null){
            throw new RpcException("spring ioc exception");
        }
        RpcResponse response =  requestManager.sendMessage(request);
        //3.返回业务结果
        return response.getResult();
    }
}
