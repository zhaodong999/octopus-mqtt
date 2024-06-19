package org.octopus.gateway.netty;

import io.netty.handler.codec.mqtt.MqttSubscriptionOption;
import io.netty.util.AttributeKey;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class AttrKey {

    private AttrKey() {
    }

    /*服务端自增id*/
    public static final AttributeKey<AtomicInteger> SERVER_MSG_ID = AttributeKey.valueOf("serverMsgId");

    public static final AttributeKey<String> CLIENT_ID = AttributeKey.valueOf("clientId");

    public static final AttributeKey<Map<String, MqttSubscriptionOption>> TOPICS = AttributeKey.valueOf("topics");

}
