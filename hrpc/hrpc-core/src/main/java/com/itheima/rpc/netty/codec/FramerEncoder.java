package com.itheima.rpc.netty.codec;

import io.netty.handler.codec.LengthFieldPrepender;
import org.springframework.stereotype.Component;

/**
 * @author chenw
 * @version 1.0
 * @description: 一次编码的出栈处理器
 * @date 2024/11/3 上午10:44
 */
@Component
public class FramerEncoder extends LengthFieldPrepender {
    public FramerEncoder() {
        super(4);
    }
}
