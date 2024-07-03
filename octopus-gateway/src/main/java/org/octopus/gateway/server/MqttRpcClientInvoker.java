package org.octopus.gateway.server;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.buffer.ByteBuf;
import org.octopus.gateway.exception.GatewayException;
import org.octopus.proto.rpc.Rpc;
import org.octopus.proto.service.auth.Authservice;
import org.octopus.rpc.client.RpcClient;
import org.octopus.rpc.client.RpcInvoker;
import org.octopus.rpc.cluster.BalanceType;
import org.octopus.rpc.cluster.RpcClusterFactory;
import org.octopus.rpc.exception.RpcClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * 处理所有转发后端rpc服务的请求
 */
public class MqttRpcClientInvoker {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttRpcClientInvoker.class);

    private MqttRpcClientInvoker() {
    }

    /**
     * 处理mqtt 连接请求，转发到后端服务上
     *
     * @param device   设备id
     * @param name     用户名
     * @param password 密码
     * @return 认证结果
     * @throws RpcClientException 处理调用异常
     */
    public static CompletableFuture<Authservice.AuthResult> auth(String device, String name, byte[] password) throws RpcClientException {
        LOGGER.info("mqtt conn: {}\t{}\t{}", device, name, password);

        // 构造请求参数
        Authservice.UserMessage userMessage = Authservice.UserMessage.newBuilder()
                .setDevice(device)
                .setName(name)
                .setPassword(ByteString.copyFrom(password))
                .build();

        return RpcInvoker.invoke("authService", "auth", userMessage, Authservice.AuthResult.class, device, device);
    }

    /**
     * 转发mqtt消息
     *
     * @param payload  mqtt消息内容
     * @param clientId 终端标识
     */
    public static void forwardMsgOneway(ByteBuf payload, String clientId) throws GatewayException {
        byte[] body = new byte[payload.capacity()];
        payload.readBytes(body);

        Rpc.RpcRequest request = null;
        try {
            request = Rpc.RpcRequest.parseFrom(body);
        } catch (InvalidProtocolBufferException e) {
            throw new GatewayException(e.getMessage());
        }

        String serviceName = request.getService();
        LOGGER.info("rpc client send: {}\t{}\t{}", request.getService(), request.getMethod(), clientId);

        try {
            RpcClient rpcClient = RpcClusterFactory.getRpcClient(serviceName, BalanceType.HASH, clientId);
            rpcClient.callOneway(request);
        } catch (RpcClientException e) {
            LOGGER.error("rpc client send msg err", e);
        }
    }
}
