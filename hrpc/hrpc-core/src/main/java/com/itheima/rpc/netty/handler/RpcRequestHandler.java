package com.itheima.rpc.netty.handler;

import com.itheima.rpc.data.RpcRequest;
import com.itheima.rpc.data.RpcResponse;
import com.itheima.rpc.spring.SpringBeanFactory;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Repeatable;
import java.lang.reflect.Method;

/**
 * @author chenw
 * @version 1.0
 * @description: TODO
 * @date 2024/11/3 上午10:57
 */

@Slf4j
@ChannelHandler.Sharable
public class RpcRequestHandler extends SimpleChannelInboundHandler<RpcRequest> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest request) throws Exception {
        log.info("RpcRequestHandler收到请求:{}",request);
        //有请求就有响应
        RpcResponse res = new RpcResponse();
        res.setRequestId(request.getRequestId());
        //todo:业务调用方法返回数据
        //接口名称
        String interfaceName = request.getClassName();
        String methodName = request.getMethodName();
        //参数的类型和参数
        Class<?>[] parameterTypes = request.getParameterTypes();
        Object[] parameters = request.getParameters();
        try {
            //从容器的里面获取Bean 找到目标Bean
            Object bean = SpringBeanFactory.getBean(Class.forName(interfaceName));//根据接口获取bean
            //拿到bean中的方法
            Method method = bean.getClass().getMethod(methodName, parameterTypes);
            //执行方法获取结果
            Object result = method.invoke(bean, parameters);
            res.setResult(result);
        } catch (Throwable e){
            res.setCause(e);
            log.error("RpcRequestHandler执行失败,msg={}",e.getMessage());
        } finally {
            //将response写回去
            ctx.channel().writeAndFlush(res);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("服务端出现异常,msg={}",cause.getMessage());
        super.exceptionCaught(ctx, cause);
    }
}
