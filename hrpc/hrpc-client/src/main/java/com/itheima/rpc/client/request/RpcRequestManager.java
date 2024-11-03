package com.itheima.rpc.client.request;

import com.itheima.rpc.cache.ServiceProviderCache;
import com.itheima.rpc.client.cluster.LoadBalanceStrategy;
import com.itheima.rpc.client.cluster.StartegyProvider;
import com.itheima.rpc.data.RpcRequest;
import com.itheima.rpc.data.RpcResponse;
import com.itheima.rpc.exception.RpcException;
import com.itheima.rpc.netty.codec.FrameDecoder;
import com.itheima.rpc.netty.codec.FramerEncoder;
import com.itheima.rpc.netty.codec.RpcRequestEncoder;
import com.itheima.rpc.netty.codec.RpcResponseDecoder;
import com.itheima.rpc.netty.handler.RpcResponseHandler;
import com.itheima.rpc.netty.request.ChannelMapping;
import com.itheima.rpc.netty.request.RequestPromise;
import com.itheima.rpc.netty.request.RpcRequestHolder;
import com.itheima.rpc.provider.ServiceProvider;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.protostuff.Rpc;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author chenw
 * @version 1.0
 * @description: TODO
 * @date 2024/11/3 下午7:28
 */

@Slf4j
@Component
public class RpcRequestManager {
    @Resource
    private ServiceProviderCache cache;
    @Resource
    private StartegyProvider startegyProvider;

    public RpcResponse sendMessage(RpcRequest request) {
        //根据当前的接口 获取当前的接口的提供者
        String className = request.getClassName();
        List<ServiceProvider> providers = cache.get(className);
        if (providers==null || providers.isEmpty()){
            log.info("接口{}没有提供者",request.getClassName());
            throw new RpcException("接口"+request.getClassName()+"没有提供者");
        }
        //todo:负载均衡 lb
        LoadBalanceStrategy strategy = startegyProvider.getStrategy();
        ServiceProvider provider = strategy.select(providers);
        //发送网络请求
        return requestByNetty(request,provider);

    }

    private RpcResponse requestByNetty(RpcRequest request, ServiceProvider provider) {
        //先查找对provider的连接是否存在 不存在则建立 存在直接使用channel
        Channel channel;
        if (!RpcRequestHolder.channelExist(provider.getServerIp(),provider.getRpcPort())){
            NioEventLoopGroup group = new NioEventLoopGroup();
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            //first encoder
                            pipeline.addLast("FramerEncoder",new FramerEncoder());
                            //second encoder
                            pipeline.addLast("RpcRequestEncoder",new RpcRequestEncoder()); //request --> byteBuf
                            //first decoder
                            pipeline.addLast("FrameDecoder",new FrameDecoder());
                            //second decoder
                            pipeline.addLast("RpcResponseDecoder",new RpcResponseDecoder());  //byteBuf -- response
                            //etc handler
                            pipeline.addLast("RpcResponseHandler",new RpcResponseHandler());  //response
                        }
                    });
            try {
                ChannelFuture future = bootstrap.connect(provider.getServerIp(), provider.getRpcPort()).sync();
                if (future.isSuccess()){
                    channel = future.channel();
                    //保存
                    RpcRequestHolder.addChannelMapping(new ChannelMapping(provider.getServerIp(),provider.getRpcPort(),channel));
                }
            } catch (Exception e){
                log.error("can's connect provider={}",provider);
                throw new RpcException("can's connect provider"+provider.toString());
            }
        }
        /*NioEventLoopGroup group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        //first encoder
                        pipeline.addLast("FramerEncoder",new FramerEncoder());
                        //second encoder
                        pipeline.addLast("RpcRequestEncoder",new RpcRequestEncoder()); //request --> byteBuf
                        //first decoder
                        pipeline.addLast("FrameDecoder",new FrameDecoder());
                        //second decoder
                        pipeline.addLast("RpcResponseDecoder",new RpcResponseDecoder());  //byteBuf -- response
                        //etc handler
                        pipeline.addLast("RpcResponseHandler",new RpcResponseHandler());  //response
                    }
                });*/
        try {
            channel = RpcRequestHolder.getChannel(provider.getServerIp(),provider.getRpcPort());
            //发送数据
            RequestPromise promise = new RequestPromise(channel.eventLoop()); //将promise和执行获取响应结果的handler关联
            RpcRequestHolder.addRequestPromise(request.getRequestId(),promise);  //每次的promise都和请求id建立映射关系
            channel.writeAndFlush(request);
            //获取结果 等待获取结果 使用promise堵塞的获取结果
            RpcResponse response = (RpcResponse) promise.get();
            RpcRequestHolder.removeRequestPromise(request.getRequestId());
            return response;
        } catch (Exception e) {
            log.error("rpc call failed , msg = {}",e.getMessage());
            throw new RpcException(e);
        }
    }

}
