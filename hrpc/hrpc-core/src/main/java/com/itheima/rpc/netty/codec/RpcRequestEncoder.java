package com.itheima.rpc.netty.codec;

import com.itheima.rpc.data.RpcRequest;
import com.itheima.rpc.data.RpcResponse;
import com.itheima.rpc.util.ProtostuffUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

// RpcRequest --> ByteBuf
@Slf4j
public class RpcRequestEncoder extends MessageToMessageEncoder<RpcRequest> {
    @Override
    protected void encode(ChannelHandlerContext ctx, RpcRequest request, List<Object> out) throws Exception {
        try {
            //序列化
            byte[] bytes = ProtostuffUtil.serialize(request);
            ByteBuf buf = ctx.alloc().buffer();
            buf.writeBytes(bytes);
            //传递给下一个的出栈处理器
            out.add(buf);
        } catch (Throwable e){
            log.error("RpcRequestEncoder encoder error , msg = {}",e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
