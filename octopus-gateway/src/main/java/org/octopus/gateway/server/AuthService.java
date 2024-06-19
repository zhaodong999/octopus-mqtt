package org.octopus.gateway.server;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class AuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);

    private AuthService(){}

    public static CompletableFuture<AuthResult> auth(String id, String name, String password) {
        LOGGER.info("mqtt conn: {}\t{}\t{}", id, name, password);
        return CompletableFuture.supplyAsync(() -> new AuthResult(MqttConnectReturnCode.CONNECTION_ACCEPTED));
    }
}
