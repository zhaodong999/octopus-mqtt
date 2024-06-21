package org.octopus.gateway.server;

import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import org.octopus.gateway.netty.AttrKey;
import org.octopus.proto.rpc.Rpc;
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
            LOGGER.info("connectMsg: {}", connectMessage);
            MqttConnectPayload payload = connectMessage.payload();
            String id = payload.clientIdentifier();
            String name = payload.userName();
            String password = new String(payload.passwordInBytes());

            CompletableFuture<AuthResult> future = AuthService.auth(id, name, password);
            future.whenComplete((authResult, throwable) -> {
                MqttFixedHeader connAckFixedHeader =
                        new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
                MqttConnAckVariableHeader connAckVariableHeader;
                if (throwable == null) {
                    connAckVariableHeader =
                            new MqttConnAckVariableHeader(authResult.getCode(), false);
                } else {
                    connAckVariableHeader =
                            new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE, false);
                }

                ConnectionManager.putCtx(id, ctx);
                MqttMessage connAckMsg = MqttMessageFactory.newMessage(connAckFixedHeader, connAckVariableHeader, null);
                ctx.writeAndFlush(connAckMsg);
                ctx.channel().attr(AttrKey.CLIENT_ID).set(id);
                ctx.channel().attr(AttrKey.SERVER_MSG_ID).set(new AtomicInteger(1));
                ctx.channel().attr(AttrKey.TOPICS).set(new HashMap<>());
            });
        }
    },

    PUBLISH {
        @Override
        public void handle(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
            MqttPublishMessage publishMsg = (MqttPublishMessage) mqttMessage;
            LOGGER.info("publishMsg: {}", publishMsg);

            ByteBuf payload = publishMsg.payload();
            byte[] body = new byte[payload.capacity()];
            payload.readBytes(body);

            Rpc.RpcRequest request;
            try {
                request = Rpc.RpcRequest.parseFrom(body);
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }

            if (request == null) {
                return;
            }

            String clientId = ctx.channel().attr(AttrKey.CLIENT_ID).get();
            switch (publishMsg.fixedHeader().qosLevel()) {
                case AT_MOST_ONCE:
                    //最多一次，重复要处理一下
                    PublishService.handleOne(request, clientId);
                    break;
                case AT_LEAST_ONCE:
                    //回复一个publishAck,会传递多次
                    PublishService.handleOne(request, clientId);

                    MqttFixedHeader fixedHeader =
                            new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
                    MqttMessage pubAckMsg = MqttMessageFactory.newMessage(fixedHeader, MqttMessageIdVariableHeader.from(publishMsg.variableHeader().packetId()), null);
                    ctx.writeAndFlush(pubAckMsg);
                    break;
                case EXACTLY_ONCE:
                    //TODO 暂时不支持
                    PublishService.handleOne(request, clientId);

                    MqttFixedHeader rceFixedHeader =
                            new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 0);
                    MqttMessage pubRecMsg = MqttMessageFactory.newMessage(rceFixedHeader, MqttMessageIdVariableHeader.from(publishMsg.variableHeader().packetId()), null);
                    ctx.writeAndFlush(pubRecMsg);

                    //TODO , 存储当前packetId
                    break;
                default:
                    break;
            }
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

    public abstract void handle(ChannelHandlerContext ctx, MqttMessage mqttMessage);
}
