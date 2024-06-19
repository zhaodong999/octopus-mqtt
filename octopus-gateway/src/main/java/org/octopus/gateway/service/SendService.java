package org.octopus.gateway.service;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import org.octopus.gateway.netty.AttrKey;
import org.octopus.gateway.server.ConnectionManager;
import org.octopus.proto.gateway.Server;
import org.octopus.rpc.server.anno.RpcMethod;
import org.octopus.rpc.server.anno.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.octopus.gateway.netty.AttrKey.SERVER_MSG_ID;

@RpcService(name = "gate")
public class SendService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendService.class);

    @RpcMethod(name = "publish")
    public void sendMsg(Server.ServerMessage serverMessage) {
        LOGGER.info("rpc receive: {}\t{}", serverMessage.getId(), serverMessage.getTopic());

        //get connection channel
        ChannelHandlerContext ctx = ConnectionManager.getCtx(serverMessage.getId());
        if (ctx == null) {
            LOGGER.error("client not exist with identity: {}", serverMessage.getId());
            return;
        }

        Map<String, MqttSubscriptionOption> topics = ctx.channel().attr(AttrKey.TOPICS).get();
        if (!topics.containsKey(serverMessage.getTopic())) {
            LOGGER.error("client not sub this topic :{}", serverMessage.getTopic());
            return;
        }

        MqttSubscriptionOption clientSubOption = topics.get(serverMessage.getTopic());
        int qos = clientSubOption.qos().value() > serverMessage.getQos() ? clientSubOption.qos().value() : serverMessage.getQos();

        byte[] body = serverMessage.toByteArray();
        //TODO  这里有一套逻辑，堆外缓存消息，主题下的存储消息等，消息ID 释放
        int serverMsgId = ctx.channel().attr(SERVER_MSG_ID).get().getAndIncrement();

        //create publishMessage
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.valueOf(qos), true, body.length);
        MqttPublishVariableHeader variableHeader = new MqttPublishVariableHeader(serverMessage.getTopic(), serverMsgId);
        ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
        MqttMessage mqttMessage = MqttMessageFactory.newMessage(fixedHeader, variableHeader, buffer);

        if (!ctx.channel().isWritable()) {
            LOGGER.error("channel can't write, check send ratio");
            return;
        }

        //send to client
        ctx.writeAndFlush(mqttMessage);
    }
}
