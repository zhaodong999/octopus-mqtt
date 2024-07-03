package org.octopus.gateway.server;

import com.google.protobuf.ByteString;
import org.octopus.proto.service.auth.Authservice;
import org.octopus.rpc.client.RpcInvoker;
import org.octopus.rpc.exception.RpcClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class MqttRpcClientInvoker {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttRpcClientInvoker.class);
    
    private MqttRpcClientInvoker() {
    }

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
}
