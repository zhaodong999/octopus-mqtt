package org.octopus.client;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttCallBack implements MqttCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttCallBack.class);

    @Override
    public void connectionLost(Throwable cause) {

    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        LOGGER.info("topic : {}, message: {}", topic, message.getId());

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }
}
