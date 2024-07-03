package org.octopus.gateway.server;

import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import org.octopus.gateway.exception.GatewayException;
import org.octopus.gateway.netty.AttrKey;
import org.octopus.gateway.tracker.MqttMsgLogger;
import org.octopus.proto.gateway.Server;
import org.octopus.proto.service.auth.Authservice;
import org.octopus.rpc.exception.RpcClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public enum GatewayProcessor {

    CONNECT {
        @Override
        public void handle(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
            MqttConnectMessage connectMessage = (MqttConnectMessage) mqttMessage;
            MqttConnectPayload payload = connectMessage.payload();
            String id = payload.clientIdentifier();
            try {
                CompletableFuture<Authservice.AuthResult> future = MqttRpcClientInvoker.auth(payload.clientIdentifier(), payload.userName(), payload.passwordInBytes());
                future.whenComplete((authResult, throwable) -> {
                    if (throwable != null) {
                        LOGGER.error("{}\tinvoke rpc err", id, throwable);
                        MqttMsgUtil.sendConnAckMsg(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE, ctx);
                        return;
                    }

                    if (authResult.getAuthType() != Authservice.AuthType.LOGIN) {
                        LOGGER.warn("{}\t name or password err {}\t{}", id, payload.userName(), payload.passwordInBytes());
                        MqttMsgUtil.sendConnAckMsg(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD, ctx);
                        return;
                    }

                    //校验成功的情况下
                    ConnectionManager.putCtx(id, ctx);
                    MqttMsgUtil.sendConnAckMsg(MqttConnectReturnCode.CONNECTION_ACCEPTED, ctx);
                    //init data
                    ctx.channel().attr(AttrKey.CLIENT_ID).set(id);
                    ctx.channel().attr(AttrKey.SERVER_MSG_ID).set(new AtomicInteger(1));
                    ctx.channel().attr(AttrKey.TOPICS).set(new HashMap<>());

                });
            } catch (RpcClientException e) {
                LOGGER.error("auth error", e);
                MqttMsgUtil.sendConnAckMsg(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE, ctx);
            }
        }
    },

    PUBLISH {
        @Override
        public void handle(ChannelHandlerContext ctx, MqttMessage mqttMessage) throws GatewayException {
            MqttPublishMessage publishMsg = (MqttPublishMessage) mqttMessage;

            ByteBuf payload = publishMsg.payload();
            byte[] body = new byte[payload.capacity()];
            payload.readBytes(body);


            Server.ClientMessage clientMessage = null;
            try {
                clientMessage = Server.ClientMessage.parseFrom(body);
            } catch (InvalidProtocolBufferException e) {
                throw new GatewayException(e.getMessage());
            }

            String clientId = ctx.channel().attr(AttrKey.CLIENT_ID).get();
            switch (publishMsg.fixedHeader().qosLevel()) {
                case AT_MOST_ONCE:
                    //最多一次，重复要处理一下
                    MqttRpcClientInvoker.forwardMsgOneway(publishMsg, clientMessage, clientId);
                    break;
                case AT_LEAST_ONCE:
                    //回复一个publishAck,会传递多次
                    MqttRpcClientInvoker.forwardMsgOneway(publishMsg, clientMessage, clientId);
                    MqttMsgUtil.sendPubAckMsg(publishMsg.variableHeader().packetId(), ctx, clientId);
                    break;
                case EXACTLY_ONCE:
                    //TODO 暂时不支持
                    MqttRpcClientInvoker.forwardMsgOneway(publishMsg, clientMessage, clientId);
                    MqttMsgUtil.sendPubRelMessage(publishMsg.variableHeader().packetId(), ctx);
                    //TODO , 存储当前packetId
                    break;
                default:
                    break;
            }
        }
    },

    PUBACK {
        @Override
        public void handle(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
            MqttPubAckMessage pubAckMessage = (MqttPubAckMessage) mqttMessage;
            MqttMsgLogger.receivePubAckLog(pubAckMessage.variableHeader().messageId(), ctx.channel().attr(AttrKey.CLIENT_ID).get());
        }
    },


    PUBREL {
        @Override
        public void handle(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
            LOGGER.info("pubRel msg: {}", mqttMessage);
        }
    },

    SUBSCRIBE {
        @Override
        public void handle(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
            MqttSubscribeMessage mqttSubscribeMessage = (MqttSubscribeMessage) mqttMessage;
            LOGGER.info("subMsg: {}", mqttSubscribeMessage);

            Map<String, MqttSubscriptionOption> topics = mqttSubscribeMessage.payload().topicSubscriptions().stream().filter(entity -> entity.topicFilter() != null).collect(Collectors.toMap(MqttTopicSubscription::topicFilter, MqttTopicSubscription::option));
            ctx.channel().attr(AttrKey.TOPICS).get().putAll(topics);
            MqttFixedHeader subFixedHeader =
                    new MqttFixedHeader(MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
            MqttMessage subMqttMsg = MqttMessageFactory.newMessage(subFixedHeader, MqttMessageIdVariableHeader.from(mqttSubscribeMessage.variableHeader().messageId()), new MqttSubAckPayload(MqttQoS.AT_MOST_ONCE.value()));
            ctx.writeAndFlush(subMqttMsg);
        }
    },

    PING {
        @Override
        public void handle(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
            LOGGER.info("mqtt ping msg: {}", mqttMessage);
            ctx.writeAndFlush(MqttMessage.PINGRESP);
        }
    },

    DISCONNECT {
        @Override
        public void handle(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
            LOGGER.info("mqtt disconnect msg: {}", mqttMessage);
            ctx.close();
        }
    },

    UNKNOWN {
        @Override
        public void handle(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
            LOGGER.info("mqtt unknown msg: {}", mqttMessage);
        }
    };


    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayProcessor.class);

    public abstract void handle(ChannelHandlerContext ctx, MqttMessage mqttMessage) throws GatewayException;
}
