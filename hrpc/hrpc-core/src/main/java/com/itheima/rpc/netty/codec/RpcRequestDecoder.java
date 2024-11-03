package com.itheima.rpc.netty.codec;

import com.itheima.rpc.data.RpcRequest;
import com.itheima.rpc.util.ProtostuffUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Properties;

/**
 * @author chenw
 * @version 1.0
 * @description: 将ByteBuf转为客户端请求对象  服务端接受请求
 * @date 2024/11/3 上午10:51
 */
@Component
@Slf4j
public class RpcRequestDecoder extends MessageToMessageDecoder<ByteBuf> {
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf msg , List<Object> out) throws Exception {
       try {
           int length = msg.readableBytes();
           byte[] bytes = new byte[length];
           msg.readBytes(bytes);
           //将客户端的请求ByteBuf转为请求对象
           RpcRequest request = ProtostuffUtil.deserialize(bytes, RpcRequest.class);
           out.add(request);
       } catch(Exception e){
           log.error("RpcRequestDecoder decode error, msg = {}",e.getMessage());
           throw new RuntimeException(e);
       }
    }
}
