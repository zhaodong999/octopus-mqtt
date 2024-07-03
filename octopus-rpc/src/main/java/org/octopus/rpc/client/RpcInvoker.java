package org.octopus.rpc.client;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import org.octopus.proto.rpc.Rpc;
import org.octopus.rpc.cluster.BalanceType;
import org.octopus.rpc.cluster.RpcClusterFactory;
import org.octopus.rpc.exception.RpcClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;


public class RpcInvoker {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcInvoker.class);

    private RpcInvoker() {

    }

    public static <P extends Message, R extends Message> CompletableFuture<R> invoke(String service, String method, P param, Class<R> resultClass, String requestId, String balanceId) throws RpcClientException {
        // 构造请求
        Rpc.RpcRequest rpcRequest = Rpc.RpcRequest.newBuilder()
                .setService(service)
                .setMethod(method)
                .addArgs(Any.pack(param))
                .build();

        // 调用远程服务
        RpcClient rpcClient = RpcClusterFactory.getRpcClient(service, BalanceType.HASH, balanceId);
        CompletableFuture<Rpc.RpcResponse> respFuture = rpcClient.call(rpcRequest);

        //异常处理
        respFuture.exceptionally(e -> {
            LOGGER.error("{}\t{}", requestId, e.getMessage());
            return null;
        });

        //转换返回值
        return respFuture.thenApplyAsync(rpcResponse -> {
            if (rpcResponse == null) {
                throw new CompletionException(new RpcClientException("rpc response is null"));
            }

            LOGGER.info("rpc response: {}", rpcResponse.getStatus().name());
            //rpc调用出错
            if (rpcResponse.getStatus() != Rpc.RpcStatus.OK) {
                LOGGER.error("{}\trpc error: {}\t{}", requestId, rpcResponse.getStatus(), rpcResponse.getReason());
                throw new CompletionException(rpcResponse.getStatus().name(), new RpcClientException(rpcResponse.getReason()));
            }

            try {
                return rpcResponse.getResult().unpack(resultClass);
            } catch (InvalidProtocolBufferException e) {
                throw new CompletionException(e);
            }
        });
    }
}
