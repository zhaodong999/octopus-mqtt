package org.octopus.rpc.client;

import io.netty.channel.nio.NioEventLoopGroup;
import org.octopus.proto.rpc.Rpc;
import org.octopus.rpc.EndPoint;
import org.octopus.rpc.exception.RpcClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * 只需要实现异步调用，不需要同步调用
 * 每个RpcClient实例跟主机绑定
 * 增加一个调用超时处理
 * 延迟建立连接，未必所有服务都要调用
 * <li>
 * 暂时是单连接处理，以后变成连接池
 * </>
 */
public class RpcClient implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcClient.class);

    private final EndPoint endPoint;

    private RpcConnection rpcConnection;

    private final NioEventLoopGroup nioEventLoopGroup;

    public RpcClient(EndPoint endPoint, NioEventLoopGroup nioEventLoopGroup) {
        this.endPoint = endPoint;
        this.nioEventLoopGroup = nioEventLoopGroup;
    }

    public void connect() {
        rpcConnection = new RpcConnection(endPoint, nioEventLoopGroup);
        try {
            rpcConnection.connect();
        } catch (InterruptedException e) {
            LOGGER.error("rpc client connect err", e);
        }
    }

    public CompletableFuture<Rpc.RpcResponse> call(Rpc.RpcRequest request) throws RpcClientException {
        if (!rpcConnection.available()) {
            throw new RpcClientException("rpc connection not available");
        }
        CompletableFuture<Rpc.RpcResponse> callBack = new CompletableFuture<>();
        rpcConnection.send(request, callBack);
        return callBack;
    }

    public EndPoint getEndPoint() {
        return endPoint;
    }

    public void callOneway(Rpc.RpcRequest request) throws RpcClientException {
        if (!rpcConnection.available()) {
            throw new RpcClientException("rpc connection not available");
        }

        rpcConnection.sendNoResponse(request);
    }

    @Override
    public void close() throws IOException {
        rpcConnection.close();
    }
}
