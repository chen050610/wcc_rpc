package com.itheima.rpc.netty.codec;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.springframework.stereotype.Component;

/**
 * @author chenw
 * @version 1.0
 * @description: 一次解码的入栈处理器
 * @date 2024/11/3 上午10:43
 */
@Component
public class FrameDecoder extends LengthFieldBasedFrameDecoder {

    public FrameDecoder() {
        super(Integer.MAX_VALUE, 0, 4, 0, 4);
    }
}
