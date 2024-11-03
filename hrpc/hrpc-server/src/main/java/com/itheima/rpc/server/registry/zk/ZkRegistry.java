package com.itheima.rpc.server.registry.zk;

import com.itheima.rpc.annotation.HrpcService;
import com.itheima.rpc.server.config.RpcServerConfiguration;
import com.itheima.rpc.server.registry.RpcRegister;
import com.itheima.rpc.spring.SpringBeanFactory;
import com.itheima.rpc.util.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @author chenw
 * @version 1.0
 * @description: TODO
 * @date 2024/11/3 上午9:42
 */

@Component
@Slf4j
@DependsOn("springBeanFactory")  //只有springBeanFactory完成初始化以后
public class ZkRegistry implements RpcRegister {
    @Resource
    private ServerZKit serverZKit;  //zk sdk
    @Resource
    private RpcServerConfiguration rpcServerConfiguration; //configure

    @Override
    public void serverRegister() {
        // 根据自定义的注解获取 bean
        Map<String, Object> annotationClass
                = SpringBeanFactory.getBeanListByAnnotationClass(HrpcService.class);
        if (annotationClass!=null && !annotationClass.isEmpty()){
            //创建root节点 /rpc
            serverZKit.createRootNode();
            //get ip
            String ip = IpUtil.getRealIp();
            // 根据注解中的 HrpcService.interfaceClasss属性注册接口
            for (Object bean : annotationClass.values()) {
                //获取Bean 上注解
                HrpcService hrpcService = bean.getClass().getAnnotation(HrpcService.class);
                //拿到interfaceClass属性
                Class<?> interfaceClass = hrpcService.interfaceClass();
                //接口的名称
                String interfaceName = interfaceClass.getName();
                //创建 代表接口的子节点
                serverZKit.createPersistentNode(interfaceName);
                //在接口节点创建该提供者的的临时子节点
                String providerNode = ip + ":" + rpcServerConfiguration.getRpcPort();
                //前面的根节点在方法的里面
                serverZKit.createNode(interfaceName+"/"+providerNode);
                log.info("服务{}-----{}完成了注册",interfaceName,providerNode);
            }
        }
    }
}
