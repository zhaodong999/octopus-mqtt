package org.octopus.rpc.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.octopus.rpc.*;
import org.octopus.rpc.cluster.RpcClientManager;
import org.octopus.rpc.exception.RpcClientException;
import org.octopus.proto.rpc.Rpc;
import org.octopus.rpc.util.IdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class RpcConnection implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcConnection.class);

    private final Bootstrap bootstrap;

    private final EndPoint endPoint;

    private Channel channel;

    private long period = 2;

    public RpcConnection(EndPoint endPoint, NioEventLoopGroup nioEventLoopGroup) {
        this.endPoint = endPoint;

        bootstrap = new Bootstrap();
        bootstrap.channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .group(nioEventLoopGroup)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new RpcEncoder());
                        ch.pipeline().addLast(new RpcDecoder());
                        ch.pipeline().addLast(new IdleStateHandler(60, 30, 0));
                        ch.pipeline().addLast(new RpcClientHandler());
                    }
                });

    }

    public void connect() throws InterruptedException {
        ChannelFuture channelFuture = bootstrap.connect(endPoint.getHost(), endPoint.getPort()).sync();
        channelFuture.addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                LOGGER.info("rpc client connect over: {}:{}", endPoint.getHost(), endPoint.getPort());
                channel = f.channel();
                addCloseListener();
            } else {
                LOGGER.error("rpc client connect err", f.cause());
                channel = null;
                //2,4,8,16,32,大于一分钟要重新从2开始
                period = period > 60 ? 2 : (long) Math.pow(period, 2);
                Thread.sleep(period * 1000);
                connect();
            }
        });
    }

    public void addCloseListener() {
        if (channel != null) {
            channel.closeFuture().addListeners((ChannelFutureListener) future -> RpcClientManager.removeRpcClient(endPoint));
        }
    }

    public void send(Rpc.RpcRequest rpcRequest, CompletableFuture<Rpc.RpcResponse> callBack) throws RpcClientException {
        if (channel == null || !channel.isWritable()) {
            throw new RpcClientException("channel is not writable");
        }

        RpcMsg rpcMsg = new RpcMsg(ProtoCommand.REQUEST);
        long id = IdUtils.getUniqueIdBySnakeflow();
        VariableHeader variableHeader = new VariableHeader(id, SerializeType.PROTO);
        rpcMsg.setVariableHeader(variableHeader);
        rpcMsg.setPayLoad(rpcRequest.toByteArray());

        channel.writeAndFlush(rpcMsg).addListener(f -> {
            if (f.isSuccess()) {
                RequestHolder.put(id, callBack);
                LOGGER.info("send request trackerId:\t{}", id);
            } else {
                callBack.completeExceptionally(f.cause());
                LOGGER.error("send request err trackerId:\t{}", id, f.cause());
            }
        });
    }


    public boolean available() {
        return channel != null && channel.isWritable();
    }

    public void sendNoResponse(Rpc.RpcRequest rpcRequest) {
        RpcMsg rpcMsg = new RpcMsg(ProtoCommand.REQUEST);
        long id = IdUtils.getUniqueIdBySnakeflow();
        VariableHeader variableHeader = new VariableHeader(id, SerializeType.PROTO);
        rpcMsg.setVariableHeader(variableHeader);
        rpcMsg.setPayLoad(rpcRequest.toByteArray());

        channel.writeAndFlush(rpcMsg);
    }

    @Override
    public void close() throws IOException {
        if (channel != null) {
            channel.close();
        }
    }
}
