package com.itheima.rpc.netty.codec;

import com.itheima.rpc.data.RpcResponse;
import com.itheima.rpc.util.ProtostuffUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author chenw
 * @version 1.0
 * @description: 服务发送消息的二次编码 RpcResponse对象变为ByteBuf
 * @date 2024/11/3 上午11:07
 */
@Slf4j
public class RpcResponseEncoder extends MessageToMessageEncoder<RpcResponse> {
    @Override
    protected void encode(ChannelHandlerContext ctx, RpcResponse response, List<Object> out) throws Exception {
        try {
            //序列化
            byte[] bytes = ProtostuffUtil.serialize(response);
            ByteBuf buf = ctx.alloc().buffer();
            buf.writeBytes(bytes);
            //传递给下一个的出栈处理器
            out.add(buf);
        } catch (Throwable e){
            log.error("RpcResponseEncoder encoder error , msg = {}",e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
