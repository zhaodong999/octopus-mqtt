package org.octopus.rpc.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.octopus.rpc.RpcDecoder;
import org.octopus.rpc.RpcEncoder;
import org.octopus.rpc.cluster.RpcServiceLocator;
import org.octopus.rpc.service.RpcProxyManager;
import org.octopus.rpc.util.IpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

public class RpcServer implements Closeable{

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServer.class);

    private final int port;

    private final RpcProxyManager rpcProxyManager;

    private final RpcServiceLocator rpcServiceLocator;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public RpcServer(int port, RpcProxyManager rpcProxyManager, RpcServiceLocator rpcServiceLocator) {
        this.port = port;
        this.rpcProxyManager = rpcProxyManager;
        this.rpcServiceLocator = rpcServiceLocator;
    }

    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new RpcEncoder());
                        ch.pipeline().addLast(new RpcDecoder());
                        ch.pipeline().addLast(new IdleStateHandler(45, 0, 0));
                        ch.pipeline().addLast(new RpcServerHandler(rpcProxyManager));
                    }
                });

        ChannelFuture channelFuture = bootstrap.bind(port).sync();
        channelFuture.addListener(f -> {
            if (f.isSuccess()) {
                publishService();
                LOGGER.info("rpc server start complete, bind port: {}", port);
            }
        });

    }

    public void close() throws IOException {
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }

        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
    }

    private void publishService() {
        rpcServiceLocator.loadServices();
        rpcServiceLocator.registerInstance(rpcProxyManager.getServiceNames(), IpUtils.getIp(), port);
        LOGGER.info("publish service complete");
    }

}
