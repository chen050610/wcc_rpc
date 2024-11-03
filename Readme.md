# 手写RPC框架

* 目标

  * 深刻理解`netty`

  ## 环境的搭建

* 项目结构的分析

创建`hrpc`框架项目，完成底层`rpc`相关逻辑
创建用户项目`rpc-demo`(承载业务代码，使用`hrpc`完成`rpc`调)

![image-20241103084201628](https://hututu345.oss-cn-beijing.aliyuncs.com/typora/image-20241103084201628.png)

* 整体架构

![image-20241103084220866](https://hututu345.oss-cn-beijing.aliyuncs.com/typora/image-20241103084220866.png)

* 服务端的实现

![image-20241103084625713](./assets/image-20241103084625713.png)

* 客户端的实现

![image-20241103084649970](./assets/image-20241103084649970.png)

* `zookeeper`中数据的结构

![image-20241103092709178](./assets/image-20241103092709178.png)

## 服务端的实现

* 代码实现

编写注解 用于注册实现类

```java
@Component
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HrpcService {

    /**
     * 等同于@Component的value
     * @return
     */
    @AliasFor(annotation = Component.class)
    String value() default "";

    /**
     * 服务接口Class
     * @return
     */
    Class<?> interfaceClass() default void.class;

    /**
     * 服务接口名称
     * @return
     */
    String interfaceName() default "";

    /**
     * 服务版本号
     * @return
     */
    String version() default "";

    /**
     * 服务分组
     * @return
     */
    String group() default "";
}
```

使用注解‘

```java
@HrpcService(interfaceClass = OrderService.class)
public class OrderServiceImpl implements OrderService {

    @Autowired
    private RpcServerConfiguration serverConfiguration;

    @Override
    public String getOrder(String userId, String orderNo) {
        return serverConfiguration.getServerPort() +"---"+serverConfiguration.getRpcPort()+"---Congratulations, The RPC call succeeded,orderNo is "+orderNo +",userId is " +userId;
    }
}
```

**因为注解基于Component 所以spring容器能够扫描到 并且放入到容器里面 我们只需要从spring容器获取他即可**

* 可以工具类 根据注解获取对应的类 `getBeanListByAnnotationClass`方法

```java
@Component
public class SpringBeanFactory implements ApplicationContextAware {

    /**
     * ioc容器
     */
    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    /*public static ApplicationContext getApplicationContext() {
        return context;
    }*/

    /**
     * 根据Class获取bean
     * @param cls
     * @param <T>
     * @return
     */
    public static   <T> T getBean(Class<T> cls) {
        return context.getBean(cls);
    }

    /**
     * 根据beanName获取bean
     * @param beanName
     * @return
     */
    public static Object getBean(String beanName) {
        return context.getBean(beanName);
    }

    /***
     * 获取有指定注解的对象
     * @param annotationClass
     * @return
     */
    public static Map<String, Object> getBeanListByAnnotationClass(Class<? extends Annotation> annotationClass) {
         return context.getBeansWithAnnotation(annotationClass);
    }

    /**
     * 向容器注册单例bean
     * @param bean
     */
    public static void registerSingleton(Object bean) {
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) context.getAutowireCapableBeanFactory();
        // 让bean完成Spring初始化过程中所有增强器检验，只是不重新创建bean
        beanFactory.applyBeanPostProcessorsAfterInitialization(bean,bean.getClass().getName());
        //将bean以单例的形式入驻到容器中，此时通过bean.getClass().getName()或bean.getClass()都可以拿到放入Spring容器的Bean
        beanFactory.registerSingleton(bean.getClass().getName(),bean);
    }
}
```

* 操作`zooKeeper`客户端将服务注册到`zookeeper`中

**工具类**

```java
/**
 * Zookeeper连接操作接口
 */
@Component
public class ServerZKit {

    @Autowired
    private ZkClient zkClient;

    @Autowired
    private RpcServerConfiguration rpcServerConfiguration;

    /***
     * 根节点创建
     */
    public void createRootNode() {
        boolean exists = zkClient.exists(rpcServerConfiguration.getZkRoot());
        if (!exists) {
            zkClient.createPersistent(rpcServerConfiguration.getZkRoot());
        }
    }

    /***
     * 创建其他节点
     * @param path
     */
    public void createPersistentNode(String path) {
        String pathName = rpcServerConfiguration.getZkRoot() + "/" + path;
        boolean exists = zkClient.exists(pathName);
        if (!exists) {
            zkClient.createPersistent(pathName);
        }
    }

    /***
     * 创建节点
     * @param path
     */
    public void createNode(String path) {
        String pathName = rpcServerConfiguration.getZkRoot() + "/" + path;
        boolean exists = zkClient.exists(pathName);
        if (!exists) {
            zkClient.createEphemeral(pathName);
        }
    }
}

```

我们在服务的提供者的配置文件定义`zk`的地址 我们在`hrpc-server`需要读取配置 

![image-20241103094528203](https://hututu345.oss-cn-beijing.aliyuncs.com/typora/image-20241103094528203.png)

定义配置类 读取配置

```java
@Data
@Component
public class RpcServerConfiguration {

    /**
     * ZK根节点名称
     */
    @Value("${rpc.server.zk.root}")
    private String zkRoot;

    /**
     * ZK地址信息
     */
    @Value("${rpc.server.zk.addr}")
    private String zkAddr;


    /**
     * RPC通讯端口
     */
    @Value("${rpc.network.port}")
    private int rpcPort;

    /**
     * Spring Boot 服务端口
     */
    @Value("${server.port}")
    private int serverPort;

    /**
     * ZK连接超时时间配置
     */
    @Value("${rpc.server.zk.timeout:10000}")
    private int connectTimeout;
}
```

### 服务注册功能的实现

分别定义注册的接口和实现

```java
public interface RpcRegister {

    /** 
     * @description: 完成服务的注册
     * @param:  
     * @return: void 
     * @author chenw
     * @date: 2024/11/3 上午9:42
     */ 
    void serverRegister();
}
```

```java
@Component
public class ZkRegistry implements RpcRegister {
    @Resource
    private ServerZKit serverZKit;  //zk sdk

    @Override
    public void serverRegister() {

    }
}
```

基于`zk`完成服务的注册

```java
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
```

![image-20241103100547970](./assets/image-20241103100547970.png)

在我们的项目启动就会

![image-20241103100950662](./assets/image-20241103100950662.png)

测试 我们启动服务的提供者 `server-provider`

日志输出

![image-20241103102719293](./assets/image-20241103102719293.png)

查看`zookeeper`

![image-20241103102738252](./assets/image-20241103102738252.png)

**目前完成了服务的注册**

### 基于netty编写一个服务端的程序

定义一次编解码器 使用`netty`提供的`LengthFieldBased`

**解码器**

```java
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
```

**编码器**

```java
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
```

二次解码和编码器 

解码 `ByteBuf` ---- `RpcRequest`我们使用的反序列化方式是`Protostuff`

```java
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

```

编码 `Response` ---- `ByteBuf`我们将方法的返回封装成`Repsonse`对象

```java
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
```

业务的`handler `调用方法 写返回内容

```java
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
```

`netty` 服务端代码

```java
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
                            pipeline.addLast("FrameDecoder",new FramerEncoder());
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

```

**以上服务端的代码就已经完成了**

## 客户端的实现

### 实现服务的发现和监听变更

**配置类**

```java
@Data
@Component
public class RpcClientConfiguration {

    @Value("${rpc.client.zk.root}")
    private String zkRoot;

    @Value("${rpc.client.zk.addr}")
    private String zkAddr;

    @Value("${server.port}")
    private String znsClientPort;

    @Value("${rpc.client.api.package}")
    private String rpcClientApiPackage;

    @Value("${rpc.cluster.strategy}")
    private String rpcClientClusterStrategy;

    @Value("${rpc.client.zk.timeout}")
    private Integer connectTimeout;
}
```

客户端需要服务的发现 然后保存在本地的中 

**缓存的工具类**

```java
@Component
public class DefaultServiceProviderCache implements ServiceProviderCache {

    @Autowired
    private LoadingCache<String, List<ServiceProvider>> cache;

    @Override
    public void put(String key, List<ServiceProvider> value) {
        cache.put(key,value);
    }

    @Override
    public List<ServiceProvider> get(String key) {
        try {
            return cache.get(key);
        } catch (ExecutionException e) {
            return Lists.newArrayListWithCapacity(0);
        }
    }

    @Override
    public void evict(String key) {
        cache.invalidate(key);
    }

    @Override
    public void update(String key, List<ServiceProvider> value) {
        evict(key);
        put(key,value);
    }
}
```

编写启动类

![image-20241103184407581](./assets/image-20241103184407581.png)

**zk的工具包**

```java
@Component
public class ClientZKit {
    @Autowired
    private RpcClientConfiguration configuration;
    @Autowired
    private ZkClient zkClient;
    @Autowired
    private ServiceProviderCache cache;
    /**
     * 服务订阅接口
     * @param serviceName
     */
    public void subscribeZKEvent(String serviceName) {
        // 1. 组装服务节点信息
        String path = configuration.getZkRoot() + "/" + serviceName;
        // 2. 订阅服务节点（监听节点变化）
        zkClient.subscribeChildChanges(path, new IZkChildListener() {
            @Override
            public void handleChildChange(String parentPath, List<String> list) throws Exception {
                // 3. 判断获取的节点信息，是否为空
                if (CollectionUtils.isNotEmpty(list)) {
                    // 4. 将服务端获取的信息， 转换为服务记录对象
                    List<ServiceProvider> providerServices = convertToProviderService(serviceName, list);
                    // 5. 更新缓存信息 记得要改
                    cache.update(serviceName,providerServices);
                }
            }
        });
    }


    /**
     * 获取所有服务列表：所有的服务接口信息
     * @return
     */
    public List<String> getServiceList() {
        String path = configuration.getZkRoot();
        List<String> children = zkClient.getChildren(path);
        return children;
    }

    /**
     *  根据服务名称获取服务节点完整信息
     * @param serviceName
     * @return
     */
    public List<ServiceProvider> getServiceInfos(String serviceName) {
        String path = configuration.getZkRoot() + "/" + serviceName;
        List<String> children = zkClient.getChildren(path);
        List<ServiceProvider> providerServices = convertToProviderService(serviceName,children);
        return providerServices;
    }

    /**
     * 将拉取的服务节点信息转换为服务记录对象
     *
     * @param serviceName
     * @param list
     * @return
     */
    private List<ServiceProvider> convertToProviderService(String serviceName, List<String> list) {
        if (CollectionUtils.isEmpty(list)) {
            return Lists.newArrayListWithCapacity(0);
        }
        // 将服务节点信息转换为服务记录对象
        List<ServiceProvider> providerServices = list.stream().map(v -> {
            String[] serviceInfos = v.split(":");
            return ServiceProvider.builder()
                    .serviceName(serviceName)
                    .serverIp(serviceInfos[0])
                    .rpcPort(Integer.parseInt(serviceInfos[1]))
                    .build();
        }).collect(Collectors.toList());
        return providerServices;
    }
}

```

**实现服务的发现和监听**

```java
@Component
@Slf4j
public class ZKServiceDiscovery implements RpcServiceDiscovery {
    @Resource
    private ClientZKit clientZKit;
    @Resource
    private ServiceProviderCache cache;

    @Override
    public void serviceDiscovery() {
        //拉取所有的服务列表 获取根节点下所有的children
        List<String> serviceList = clientZKit.getServiceList();
        if (serviceList!=null && !serviceList.isEmpty()){
            for (String service : serviceList) {
                //service就是接口的全限定名字
                //获取每个服务下面的ip
                List<ServiceProvider> providers = clientZKit.getServiceInfos(service);
                //将接口以及对应的提供者列表的信息进行存储 -- cache
                cache.put(service,providers); //key = 接口名称 value = providers的列表
                log.info("订阅的接口{},提供者列表{}", service,providers);
                //订阅接口的变更 使用zk的监听机制 调用zk的api监听 如果变更 就会触发回调 在回调的里面我们修改缓存
                clientZKit.subscribeZKEvent(service);
            }
        }
    }
}
```

**测试**

![image-20241103184638722](./assets/image-20241103184638722.png)

![image-20241103184653268](./assets/image-20241103184653268.png)

### 生成代理

**注解类**

代码就会将家有注解的`HrpcRemote`生成代理

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HrpcRemote {

    String value() default "";

    /**
     * 服务接口Class
     * @return
     */
    Class<?> interfaceClass() default void.class;

    /**
     * 服务接口名称
     * @return
     */
    String interfaceName() default "";

    /**
     * 服务版本号
     * @return
     */
    String version() default "";

    /**
     * 服务分组
     * @return
     */
    String group() default "";
}
```

客户端的代理实现方式

```java
//因为我们将注解是加在contoller的bean的这个属性上的 我们应该怎么样的扫描 并且将代理填充给orderService
//如果是在配置文件 指定类的字段 就会繁琐了
@HrpcRemote
private OrderService orderService;
//我们可以利用bean的生命周期
//添加一个bean的后置处理器BeanPostProcessor 检查bean所有的字段上是否有自定义注解
```

**自定义注解标注到接口类型的字段上**

**编写一个BeanPostProcessor，检查bean的字段上是否有自定义注解**

**如果有则为其接口生成代理并注入到该字段上**

**代理方案选择cglib**

### 后置处理器的实现和代理对象的注入

生成代理对象的工具类

```java
@Slf4j
public class CglibProxyCallBackHandler implements MethodInterceptor {


    public Object intercept(Object o, Method method, Object[] parameters, MethodProxy methodProxy) throws Throwable {
        log.info("代理调用拦截 method={}",method.getName());
        //todo:rpc获取调用的结果 然后返回结果
        return "hello";
    }
}
```

```java
@Component
@Slf4j
public class RequestProxyFactory implements ProxyFactory{

    /**
     * 创建新的代理实例-CGLib动态代理
     * @param cls
     * @param <T>
     * @return
     */
    public  <T> T newProxyInstance(Class<T> cls) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(cls);
        enhancer.setCallback(new CglibProxyCallBackHandler());
        return (T) enhancer.create();
    
```

后置处理器的实现

```java

@Slf4j
@Component
public class RpcAnnotationProcessor implements BeanPostProcessor {

    @Resource
    private RequestProxyFactory proxyFactory;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        //对bean进扩展 查看字段中是否有自定义的注解
        //获取该bean的所有的field
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            try {
                HrpcRemote annotation = field.getAnnotation(HrpcRemote.class);
                if (annotation!=null){
                    if (!field.isAccessible()){
                        field.setAccessible(true);
                    }
                    //基于该field进行代理的生成
                    Object proxy = proxyFactory.newProxyInstance(field.getType());
                    if (proxy!=null){
                        //生成的代理注入到属性上
                        field.set(bean,proxy);
                    }
                }
            } catch (Exception e){
                log.error("failed to inject proxy , field={}",field.getName());
            }
        }
        return bean;
    }
}

```

测试  启动服务端 代理生效

![image-20241103192133654](https://hututu345.oss-cn-beijing.aliyuncs.com/typora/image-20241103192133654.png)

### 代理对象发送请求并且获取响应

代码如下

* 封装请求对象 发送请求

```java
@Slf4j
public class CglibProxyCallBackHandler implements MethodInterceptor {


    public Object intercept(Object o, Method method, Object[] parameters, MethodProxy methodProxy) throws Throwable {
        log.info("代理调用拦截 method={}",method.getName());
        //rpc获取调用的结果 然后返回结果
        //1.封装请求
        String requestId = RequestIdUtil.requestId(); //雪花算法生成唯一的请求id
        RpcRequest request = RpcRequest.builder()
                .requestId(requestId)
                .className(method.getDeclaringClass().getName()) //获取方法的接口名称
                .methodName(method.getName())
                .parameterTypes(method.getParameterTypes())
                .parameters(parameters)
                .build();
        //2.发送请求获取响应
        RpcRequestManager requestManager = SpringBeanFactory.getBean(RpcRequestManager.class);
        if (requestManager == null){
            throw new RpcException("spring ioc exception");
        }
        RpcResponse response =  requestManager.sendMessage(request);
        //3.返回业务结果
        return response.getResult();
    }
}
```

* 发送消息代码实现

自定义二次编解码实现

`Rpcrequest --- ByteBuf`  出栈处理器

```java
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
```

`ByteBuf ---- Response`入栈处理器

```java
@Slf4j
public class RpcResponseDecoder extends MessageToMessageDecoder<ByteBuf> {
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf msg, List<Object> out) throws Exception {
        try {
            int length = msg.readableBytes();
            byte[] bytes = new byte[length];
            msg.readBytes(bytes);
            //将客户端的请求ByteBuf转为请求对象
            RpcResponse response = ProtostuffUtil.deserialize(bytes, RpcResponse.class);
            out.add(response);
        } catch(Exception e){
            log.error("RpcRequestDecoder decode error, msg = {}",e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
```

client的实现

```java
@Slf4j
@Component
public class RpcRequestManager {
    @Resource
    private ServiceProviderCache cache;

    public RpcResponse sendMessage(RpcRequest request) {
        //根据当前的接口 获取当前的接口的提供者
        String className = request.getClassName();
        List<ServiceProvider> providers = cache.get(className);
        if (providers==null || providers.isEmpty()){
            log.info("接口{}没有提供者",request.getClassName());
            throw new RpcException("接口"+request.getClassName()+"没有提供者");
        }
        //todo:负载均衡 lb
        ServiceProvider provider = providers.get(0); //先获取第一个测试
        //发送网络请求
        return requestByNetty(request,provider);

    }

    private RpcResponse requestByNetty(RpcRequest request, ServiceProvider provider) {
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
            //建立连接
            ChannelFuture future = bootstrap.connect(provider.getServerIp(), provider.getRpcPort()).sync();
            if (future.isSuccess()){
                //发送数据
                Channel channel = future.channel();
                RequestPromise promise = new RequestPromise(channel.eventLoop()); //将promise和执行获取响应结果的handler关联
                RpcRequestHolder.addRequestPromise(request.getRequestId(),promise);  //每次的promise都和请求id建立映射关系
                channel.writeAndFlush(request);
                //获取结果 等待获取结果 使用promise堵塞的获取结果
                RpcResponse response = (RpcResponse) promise.get();
                RpcRequestHolder.removeRequestPromise(request.getRequestId());
                return response;
            }
        } catch (Exception e) {
            log.error("rpc call failed , msg = {}",e.getMessage());
            throw new RpcException(e);
        }
        return new RpcResponse();
    }

}
```

因为我们需要获取异步执行完的结果 而且是在tomcat的线程获取`eventloop`线程的结果  需要在tomcat线程获取入栈处理器的`RpcResponse`对象

```java
 //发送数据
                Channel channel = future.channel();
                RequestPromise promise = new RequestPromise(channel.eventLoop()); //将promise和执行获取响应结果的handler关联
                RpcRequestHolder.addRequestPromise(request.getRequestId(),promise);  //每次的promise都和请求id建立映射关系
                channel.writeAndFlush(request);
                //获取结果 等待获取结果 使用promise堵塞的获取结果
                RpcResponse response = (RpcResponse) promise.get();
                RpcRequestHolder.removeRequestPromise(request.getRequestId());
                return response;
```



```java
@Slf4j
public class RpcResponseHandler extends SimpleChannelInboundHandler<RpcResponse> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse response) throws Exception {
        log.info("接收到响应的结果{}",response);
        RequestPromise promise = RpcRequestHolder.getRequestPromise(response.getRequestId());
        promise.setSuccess(response);
    }
}
```

**现在已经实现我们的rpc框架**

我们还可以进一步的优化啊

## 优化

### 连接的复用

1.一个客户端和某一个服务端只需要建立一次连接，后续所有发往该服务端的请求都基于该连接即可
2.客户端保存与所有服务端的连接(channel)
3.发送请求时如果与服务端的连接已存在则直接使用，不存在则建立连接

我们可以构建一个映射 `key = ip  value = 客户端连接的channel`

代码的改造

```java

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

```

### 负载均衡的实现

常见的负载均衡的算法

`hash`:比如根据消费端`ip`的`hashcode`，对提供者节点个数求余，得到该节点，适用于消费者多提供者少的场景

`random`:在所有服务节点中随机选择

`polling`:所有服务节点依次轮询【节点也可以带权重】

`weight random`:在苕点权重的基础上随机选择

`hash`的实现

```java
public class HashLoadBalanceStrategy implements LoadBalanceStrategy {
    static String ip = IpUtil.getRealIp();
    @Override
    public ServiceProvider select(List<ServiceProvider> serviceProviders) {
        int hashIp = ip.hashCode();
        int index = Math.abs(hashIp % serviceProviders.size());
        return serviceProviders.get(index);
    }
}
```

`random`的实现

```java
public class RandomLoadBalanceStrategy implements LoadBalanceStrategy {
    @Override
    public ServiceProvider select(List<ServiceProvider> serviceProviders) {
        int index = RandomUtils.nextInt(0, serviceProviders.size());
        return serviceProviders.get(index);
    }
}
```

`轮询`的实现

```java
public class PollingLoadBalanceStrategy implements LoadBalanceStrategy {
    //全局的指针
    AtomicInteger nextCount = new AtomicInteger(0);
    @Override
    public ServiceProvider select(List<ServiceProvider> serviceProviders) {
        int index = incrementAndGet(serviceProviders.size());
        return serviceProviders.get(index);
    }

    //进行cas的操作
    private int incrementAndGet(int size) {
        //cas的操作 防止线程安全问题
        for (;;){
            int cur = nextCount.intValue();
            int next = (cur+1) % size;
            if (nextCount.compareAndSet(cur,next)){
                //如果cas成功的话就会返回
                return cur;
            }
        }
    }
}
```

`权重`的实现

```java
//使用权重
public class WeightRandomLoadBalanceStrategy implements LoadBalanceStrategy {
    @Override
    public ServiceProvider select(List<ServiceProvider> serviceProviders) {
        ArrayList<ServiceProvider> newList = new ArrayList<>();
        for (ServiceProvider serviceProvider : serviceProviders) {
            int weight = serviceProvider.getWeight();
            for (int i = 0; i < weight; i++) {
                newList.add(serviceProvider);
            }
        }
        int index = RandomUtils.nextInt(0,newList.size());
        return newList.get(index);
    }
}
```

用户自定义负载均衡的算法

用户可以在配置文件指定负载均衡的算法

![image-20241103230508575](./assets/image-20241103230508575.png)

然后框架在选择就会使用对应的算法

自定义注解

```java
@Component
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HrpcLoadBalance {

    @AliasFor(annotation = Component.class)
    String value() default "";

    /**
     * lb策略
     * @return
     */
    String strategy() default "random";
}
```

标记在负载均衡的算法的实现上

![image-20241103230636972](./assets/image-20241103230636972.png)

```java
@Component
public class DefaultStrategyProvider implements StartegyProvider, ApplicationContextAware {
    @Resource
    private RpcClientConfiguration configuration;

    LoadBalanceStrategy loadBalanceStrategy;


    @Override
    public LoadBalanceStrategy getStrategy() {
        return loadBalanceStrategy;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, Object> map =
                applicationContext.getBeansWithAnnotation(HrpcLoadBalance.class);
        for (Object bean : map.values()) {
            HrpcLoadBalance annotation = bean.getClass().getAnnotation(HrpcLoadBalance.class);
            if (annotation.strategy().equalsIgnoreCase(configuration.getRpcClientClusterStrategy())){
                loadBalanceStrategy = (LoadBalanceStrategy) bean;
                break;
            }
        }
    }
}
```

在负载均衡的引入

```java
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
```











