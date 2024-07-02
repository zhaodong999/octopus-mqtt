package org.octopus.gateway.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;

public class MqttRespUtil {

    private MqttRespUtil(){

    }

    public static void sendConnAckMessage(MqttConnectReturnCode code, ChannelHandlerContext ctx) {
        ctx.writeAndFlush(MqttMessageBuilders.connAck().returnCode(code).build());
    }
}
