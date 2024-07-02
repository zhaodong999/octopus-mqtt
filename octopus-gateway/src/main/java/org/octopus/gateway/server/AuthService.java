package org.octopus.gateway.server;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.octopus.proto.rpc.Rpc;
import org.octopus.proto.service.auth.Authservice;
import org.octopus.rpc.client.RpcClient;
import org.octopus.rpc.cluster.BalanceType;
import org.octopus.rpc.cluster.RpcClusterFactory;
import org.octopus.rpc.exception.RpcClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class AuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);

    private AuthService() {
    }

    public static CompletableFuture<Authservice.AuthResult> auth(String device, String name, byte[] password) throws RpcClientException {
        LOGGER.info("mqtt conn: {}\t{}\t{}", device, name, password);

        // 构造请求参数
        Authservice.UserMessage userMessage = Authservice.UserMessage.newBuilder()
                .setDevice(device)
                .setName(name)
                .setPassword(ByteString.copyFrom(password))
                .build();

        // 构造请求
        Rpc.RpcRequest rpcRequest = Rpc.RpcRequest.newBuilder()
                .setService("authService")
                .setMethod("auth")
                .addArgs(Any.pack(userMessage))
                .build();

        // 调用远程服务
        RpcClient rpcClient = RpcClusterFactory.getRpcClient("authService", BalanceType.HASH, device);
        CompletableFuture<Rpc.RpcResponse> respFuture = rpcClient.call(rpcRequest);
        respFuture.exceptionally(e -> {
            LOGGER.error("{}\t{}", device, e.getMessage());
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
                LOGGER.error("{}\trpc error: {}\t{}", device, rpcResponse.getStatus(), rpcResponse.getReason());
                throw new CompletionException(rpcResponse.getStatus().name(), new RpcClientException(rpcResponse.getReason()));
            }

            try {
                return rpcResponse.getResult().unpack(Authservice.AuthResult.class);
            } catch (InvalidProtocolBufferException e) {
                throw new CompletionException(e);
            }
        });
    }
}
