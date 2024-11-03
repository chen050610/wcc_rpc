package com.itheima.rpc.netty.handler;

import com.itheima.rpc.data.RpcResponse;
import com.itheima.rpc.netty.request.RequestPromise;
import com.itheima.rpc.netty.request.RpcRequestHolder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @author chenw
 * @version 1.0
 * @description: TODO
 * @date 2024/11/3 下午7:58
 */

@Slf4j
public class RpcResponseHandler extends SimpleChannelInboundHandler<RpcResponse> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse response) throws Exception {
        log.info("接收到响应的结果{}",response);
        RequestPromise promise = RpcRequestHolder.getRequestPromise(response.getRequestId());
        promise.setSuccess(response);
    }
}
