package org.octopus.gateway.tracker;

import io.netty.handler.codec.mqtt.MqttPublishMessage;
import org.octopus.proto.gateway.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttMsgLogger {

    private MqttMsgLogger() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttMsgLogger.class);

    /**
     * 接收到客户端pub消息
     *
     * @param msg           publishMessage消息
     * @param clientMessage client消息内容
     * @param clientId      客户端id
     */
    public static void receivePubLog(MqttPublishMessage msg, Server.ClientMessage clientMessage, String clientId) {
        LOGGER.info("receive pubMsg: clientId={}, trackerId={}, msgId={}, service={}, method={}, identity={}", clientMessage.getIdentity(), clientMessage.getTrackerId(), msg.variableHeader().packetId(), clientMessage.getService(), clientMessage.getMethod(), clientId);
    }

    /**
     * 发送pubAck消息
     *
     * @param packetId 消息id
     * @param clientId 客户端id
     */
    public static void sendPubAckLog(int packetId, String clientId) {
        LOGGER.info("send pubAckMsg to client: clientId={}, msgId={}", clientId, packetId);
    }

    /**
     * 向客户端发送pub消息
     *
     * @param packetId      消息id
     * @param serverMessage 服务端消息内容
     */
    public static void sendPubLog(int packetId, Server.ServerMessage serverMessage) {
        LOGGER.info("sent pubMsg to client: clientId={}, trackerId={}, msgId={}, topic={}, cmd={}", serverMessage.getIdentity(), serverMessage.getTrackerId(), packetId, serverMessage.getTopic(), serverMessage.getCmd());
    }

    /**
     * 从客户端接收到pubAck消息
     *
     * @param packetId 消息id
     * @param clientId 客户端id
     */
    public static void receivePubAckLog(int packetId, String clientId) {
        LOGGER.info("receive pubAckMsg: clientId={}, msgId={}", clientId, packetId);
    }
}
