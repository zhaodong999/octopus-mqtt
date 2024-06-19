package org.octopus.gateway.server;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;

public class AuthResult {

    public AuthResult(MqttConnectReturnCode code) {
        this.code = code;
    }

    private MqttConnectReturnCode code;

    public MqttConnectReturnCode getCode() {
        return code;
    }
}
