package io.netty.example.reactor;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Reactor - 单线程模式
 * <p>
 * accept、read、decode、process、encode、write，全部在一个线程中处理
 *
 * @author wang.yubin
 * @since 2023/12/7
 */
public class SingleThreadMode {

    private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(SingleThreadMode.class);
    private static final String CLOSE_FLAG = "bye";
    private static final int SERVER_PORT = 1543;

    public static void main(String[] args) throws InterruptedException {

        final ServerBootstrap server = new ServerBootstrap();
        try {

            // 记录服务器的channel，方便后续关闭
            final Channel[] channels = new Channel[1];

            // 统计链接数
            final AtomicInteger clientCount = new AtomicInteger();

            server

                // channel模式
                .channel(NioServerSocketChannel.class)

                // 配置线程池
                .group(new NioEventLoopGroup(1))

                // 日志记录
                .handler(new LoggingHandler())

                // 配置处理器：编解码处理器、业务处理器
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch
                            .pipeline()
                            .addLast(new StringDecoder())
                            .addLast(new StringEncoder())
                            .addLast(new SimpleChannelInboundHandler<String>() {

                                @Override
                                public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
                                    super.channelRegistered(ctx);

                                    // 自增链接数
                                    clientCount.incrementAndGet();
                                }

                                @Override
                                public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
                                    super.channelUnregistered(ctx);

                                    // 自减链接数
                                    clientCount.decrementAndGet();
                                }

                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, String msg)
                                    throws InterruptedException {
                                    LOGGER.info("接收到消息：[{}]", msg);

                                    ChannelFuture writeFuture = ctx.writeAndFlush("received :" + msg);

                                    // 如果消息是bye，关闭客户端后并关闭服务端
                                    if (CLOSE_FLAG.equalsIgnoreCase(msg)) {

                                        // 关闭通道，会导致客户端相关端口被关闭，并等待执行完成
                                        writeFuture.addListener(ChannelFutureListener.CLOSE).sync();

                                        LOGGER.info("与对应客户端的通道关闭完成，剩余客户端：[{}]", clientCount.get());

                                        // 剩余0个客户端，关闭服务端
                                        if (clientCount.get() <= 0) {
                                            channels[0].close();

                                            LOGGER.info("剩余零个客户端，关闭服务端完成");
                                        }
                                    }
                                }
                            });
                    }
                });

            // 绑定端口，阻塞线程等待关闭
            (channels[0] = server.bind(SERVER_PORT).sync().channel()).closeFuture().sync();
        } finally {
            server.config().group().shutdownGracefully();
        }
    }

    static class Client {

        @SuppressWarnings("unused")
        public static final Supplier<Supplier<String>> CONSOLE_MSG_SUPPLIER = () -> {
            // 读取控制台输入
            final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            return () -> {
                try {
                    return reader.readLine();
                } catch (IOException e) {
                    LOGGER.error("读取消息异常：", e);
                    return CLOSE_FLAG;
                }
            };
        };

        @SuppressWarnings("AlibabaUndefineMagicConstant")
        public static void main(String[] args) throws IOException, InterruptedException {

            // todo 这里是单线程模型，想要实现的是多个客户端发送消息给server，server一直用同一个线程在进行accept、read、decode、process、encode和write
            //  同样的，多线程是在server的childHandler中增加多线程处理，每个线程都处理一个请求
            //  主从模式则是指定workerGroup，并在childHandler中使用多线程处理
            //  变异主从模式（Netty默认）则是将workerGroup和childHandler中的线程池合并，只用workerGroup进行数据处理
            for (int i = 0; i < 5; i++) {

                // 初始化消息提供者
                LinkedList<String> msgList =
                    Stream.concat(IntStream.range(0, 5).boxed(), Stream.of(CLOSE_FLAG)).map(String::valueOf)
                          // .map(v -> v + "\r")
                          .collect(LinkedList::new, LinkedList::add, LinkedList::addAll);
                Supplier<String> numSupplier = msgList::pop;

                Supplier<String> consoleSupplier = CONSOLE_MSG_SUPPLIER.get();

                String num = numSupplier.get();
                System.out.println(num);
                String consoleStr = consoleSupplier.get();
                System.out.println(consoleStr.equals(num));

                // 初始化客户端并发送请求  TODO 莫名其妙的，从控制器读取的数据能正常的发送到server，但是从msgList中获取到的数据就无法发送到server，两者数据是完全一致的，不懂是哪里的问题
                // initClientSendMsg(CONSOLE_MSG_SUPPLIER.get());
                // initClientSendMsg(numSupplier);
            }
        }

        static void initClientSendMsg(Supplier<String> msgSupplier) throws InterruptedException {
            Bootstrap client = new Bootstrap();

            try {
                client
                    .channel(NioSocketChannel.class)
                    .group(new NioEventLoopGroup(1))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch
                                .pipeline()
                                .addLast(new StringEncoder())
                                .addLast(new StringDecoder())
                                .addLast(new SimpleChannelInboundHandler<String>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
                                        LOGGER.info("读取到服务器响应：[{}]", msg);
                                    }
                                });
                        }
                    });
                Channel channel = client.connect(new InetSocketAddress(SERVER_PORT)).channel();

                // 向服务器发送消息
                while (true) {

                    // 读取数据
                    String msg = msgSupplier.get();

                    // 发送数据
                    ChannelFuture writeFuture = channel.writeAndFlush(msg);
                    LOGGER.info("写入消息：[{}]", msg);

                    // 如果是关闭数据，则先等待写操作完成
                    if (CLOSE_FLAG.equals(msg)) {

                        LOGGER.info("active:[{}]", channel.isActive());

                        writeFuture.sync();

                        LOGGER.info("关闭的写消息写入完成");

                        // 中断循环
                        break;
                    }
                }

                // 关闭通道
                channel.close();

                LOGGER.info("通道关闭完成");
            } finally {
                client.config().group().shutdownGracefully();
            }
        }
    }

}
