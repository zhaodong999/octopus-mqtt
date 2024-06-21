package org.octopus.gateway.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.octopus.gateway.netty.GatewayHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

public class MqttServer implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttServer.class);
    private final int port;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;

    public MqttServer(int port) {
        this.port = port;
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
    }

    public void start() throws InterruptedException {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new MqttDecoder());
                ch.pipeline().addLast(MqttEncoder.INSTANCE);
                ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
                ch.pipeline().addLast(GatewayHandler.INSTANCE);
            }
        });

        Channel channel = bootstrap.bind(port).sync().channel();
        LOGGER.info("start gateway with port: {}", port);
        channel.closeFuture().sync();
    }

    @Override
    public void close() throws IOException {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();

        LOGGER.info("gateway server stop");
    }

}
