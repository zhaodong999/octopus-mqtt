package org.octopus.gateway.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;

public class MqttRespUtil {

    private MqttRespUtil() {

    }

    public static void sendConnAckMessage(MqttConnectReturnCode code, ChannelHandlerContext ctx) {
        ctx.writeAndFlush(MqttMessageBuilders.connAck().returnCode(code).build());
    }

    public static void sendPubAckMessage(int packetId, ChannelHandlerContext ctx) {
        ctx.writeAndFlush(MqttMessageBuilders.pubAck().packetId(packetId).build());
    }

    public static void sendPubRelMessage(int packetId, ChannelHandlerContext ctx) {
        MqttFixedHeader rceFixedHeader =
                new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttMessage pubRecMsg = MqttMessageFactory.newMessage(rceFixedHeader, MqttMessageIdVariableHeader.from(packetId), null);
        ctx.writeAndFlush(pubRecMsg);
    }
}
