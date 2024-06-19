package org.octopus.client;

import com.google.protobuf.Any;
import com.google.protobuf.StringValue;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.octopus.proto.rpc.Rpc;


public class ClientTest {

    public static void main(String[] args) throws InterruptedException {
        ClientTest clientTest = new ClientTest();
        try {
            clientTest.start();
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }

        Thread.currentThread().join();
    }

    public void start() throws MqttException {
        MqttClient mqttClient = new MqttClient("tcp://localhost:8085", "userId_001", new MemoryPersistence());
        mqttClient.setCallback(new MqttCallBack());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName("zd");
        options.setPassword("fff".toCharArray());
        mqttClient.connect(options);

        mqttClient.subscribe("/sys/game");

        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setId(1);

        Rpc.RpcRequest rpcRequest = Rpc.RpcRequest.newBuilder().setService("login").setMethod("test").addArgs(Any.pack(StringValue.of("heihei"))).build();
        mqttMessage.setPayload(rpcRequest.toByteArray());
        mqttMessage.setQos(1);
        mqttMessage.setRetained(false);
        mqttClient.publish("/sys/game", mqttMessage);
    }
}
