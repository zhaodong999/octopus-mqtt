package org.octopus.gateway.server;

import com.google.protobuf.ByteString;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import org.octopus.gateway.tracker.MqttMsgLogger;
import org.octopus.proto.gateway.Server;
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
     * @param publishMsg mqtt发布消息实体
     * @param clientId   终端标识
     */

    public static void forwardMsgOneway(MqttPublishMessage publishMsg, Server.ClientMessage clientMessage, String clientId) {
        Rpc.RpcRequest rpcRequest = Rpc.RpcRequest.newBuilder()
                .setService(clientMessage.getService())
                .setMethod(clientMessage.getMethod())
                .addArgs(clientMessage.getBody())
                .build();

        // 打印日志
        MqttMsgLogger.receivePubLog(publishMsg, clientMessage, clientId);
        try {
            RpcClient rpcClient = RpcClusterFactory.getRpcClient(clientMessage.getService(), BalanceType.HASH, clientId);
            rpcClient.callOneway(rpcRequest);
        } catch (RpcClientException e) {
            LOGGER.error("rpc client send msg err", e);
        }

    }
}
