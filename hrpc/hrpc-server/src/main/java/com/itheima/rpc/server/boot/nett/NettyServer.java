package com.itheima.rpc.server.boot.nett;

import com.itheima.rpc.netty.codec.FrameDecoder;
import com.itheima.rpc.netty.codec.FramerEncoder;
import com.itheima.rpc.netty.codec.RpcRequestDecoder;
import com.itheima.rpc.netty.codec.RpcResponseEncoder;
import com.itheima.rpc.netty.handler.RpcRequestHandler;
import com.itheima.rpc.server.boot.RpcServer;
import com.itheima.rpc.server.config.RpcServerConfiguration;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.NettyRuntime;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.UnorderedThreadPoolEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.net.InetSocketAddress;
import java.rmi.ServerError;

/**
 * @author chenw
 * @version 1.0
 * @description: TODO
 * @date 2024/11/3 上午10:31
 */

@Component
@Slf4j
public class NettyServer implements RpcServer {
    @Resource
    private RpcServerConfiguration rpcServerConfiguration;

    @Override
    public void start() {
        NioEventLoopGroup boss = new NioEventLoopGroup(1,new DefaultThreadFactory("boss"));
        NioEventLoopGroup worker = new NioEventLoopGroup(0,new DefaultThreadFactory("worker"));
        UnorderedThreadPoolEventExecutor business =
                new UnorderedThreadPoolEventExecutor(NettyRuntime.availableProcessors() * 2, new DefaultThreadFactory("business"));
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(boss,worker)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childOption(ChannelOption.TCP_NODELAY,true)
                    .childOption(ChannelOption.SO_KEEPALIVE,true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            //add first encoder
                            pipeline.addLast("FramerEncoder",new FramerEncoder());
                            //add second encoder
                            pipeline.addLast("secondEncoder",new RpcResponseEncoder()); //将response对象转为ByteBuf
                            //add fist decoder
                            pipeline.addLast("FrameDecoder",new FrameDecoder());
                            //add second decoder
                            pipeline.addLast("secondDecoder",new RpcRequestDecoder()); //将ByteBuf转为Request对象
                            //add etc handler
                            pipeline.addLast(business,"requestHandler",new RpcRequestHandler());
                        }
                    });
            ChannelFuture channelFuture = serverBootstrap.bind(new InetSocketAddress(rpcServerConfiguration.getRpcPort())).sync();
            channelFuture.channel().closeFuture().addListener((ChannelFutureListener) future -> {
                boss.shutdownGracefully();
                worker.shutdownGracefully();
                business.shutdownGracefully();
            });
        } catch (Exception e){
            log.error("NettyServer start error , msg = {}",e.getMessage());
            boss.shutdownGracefully();
            worker.shutdownGracefully();
            business.shutdownGracefully();
        }
    }
}
