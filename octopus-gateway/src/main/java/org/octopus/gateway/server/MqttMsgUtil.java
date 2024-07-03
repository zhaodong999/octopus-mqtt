package org.octopus.gateway.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import org.octopus.gateway.netty.AttrKey;
import org.octopus.gateway.tracker.MqttMsgLogger;
import org.octopus.proto.gateway.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.octopus.gateway.netty.AttrKey.SERVER_MSG_ID;

public class MqttMsgUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttMsgUtil.class);

    private MqttMsgUtil() {

    }

    public static void sendConnAckMsg(MqttConnectReturnCode code, ChannelHandlerContext ctx) {
        ctx.writeAndFlush(MqttMessageBuilders.connAck().returnCode(code).build());
    }

    /**
     * 向客户端发送pubAck消息
     *
     * @param packetId 消息id
     * @param ctx      netty上下文
     * @param clientId 客户端标识
     */
    public static void sendPubAckMsg(int packetId, ChannelHandlerContext ctx, String clientId) {
        ChannelFuture channelFuture = ctx.writeAndFlush(MqttMessageBuilders.pubAck().packetId(packetId).build());
        channelFuture.addListener(future -> {
            if (future.isSuccess()) {
                MqttMsgLogger.sendPubAckLog(packetId, clientId);
            }
        });
    }

    /**
     * 从服务端接收到消息，转发到客户端
     *
     * @param ctx           netty上下文
     * @param serverMessage 服务端消息
     */
    public static void sendPubMsg(ChannelHandlerContext ctx, Server.ServerMessage serverMessage) {
        Map<String, MqttSubscriptionOption> topics = ctx.channel().attr(AttrKey.TOPICS).get();
        if (!topics.containsKey(serverMessage.getTopic())) {
            LOGGER.error("client not sub this topic :{}", serverMessage.getTopic());
            return;
        }

        MqttSubscriptionOption clientSubOption = topics.get(serverMessage.getTopic());
        int qos = clientSubOption.qos().value() > serverMessage.getQos() ? clientSubOption.qos().value() : serverMessage.getQos();


        int packetId = ctx.channel().attr(SERVER_MSG_ID).get().getAndIncrement();
        ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
        buffer.writeBytes(serverMessage.toByteArray());

        //create publishMessage
        MqttPublishMessage publishMessage = MqttMessageBuilders.publish()
                .messageId(packetId)
                .qos(MqttQoS.valueOf(qos))
                .topicName(serverMessage.getTopic())
                .payload(buffer)
                .build();

        if (!ctx.channel().isWritable()) {
            LOGGER.error("channel can't write, check send ratio");
            return;
        }

        //send to client
        ctx.writeAndFlush(publishMessage).addListener(future -> {
            if (future.isSuccess()) {
                MqttMsgLogger.sendPubLog(packetId, serverMessage);
            }
        });
    }

    public static void sendPubRelMessage(int packetId, ChannelHandlerContext ctx) {
        MqttFixedHeader rceFixedHeader =
                new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttMessage pubRecMsg = MqttMessageFactory.newMessage(rceFixedHeader, MqttMessageIdVariableHeader.from(packetId), null);
        ctx.writeAndFlush(pubRecMsg);
    }
}
