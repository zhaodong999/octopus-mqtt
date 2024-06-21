package org.octopus.gateway.config;

import java.util.ResourceBundle;

public class GatewayConfigManager {

    /* 配置文件名称 */
    private static final String CONF_FILE = "config";
    private final ResourceBundle configBundle = ResourceBundle.getBundle(CONF_FILE);

    private GatewayConfigManager() {
    }

    private static class GatewayConfigManagerHolder {
       private static final GatewayConfigManager INSTANCE = new GatewayConfigManager();
    }

    public static GatewayConfigManager getInstance() {
        return GatewayConfigManagerHolder.INSTANCE;
    }

    /* zookeeper 连接串*/
    private static final String SERVICE_REGISTRATION_ADDRESS = "service.registration.address";

    /* 服务监听端口*/
    private static final String SERVER_PORT = "server.port";

    private static final String MQTT_PORT = "mqtt.port";


    public String getServiceRegistrationAddr() {
        return configBundle.getString(SERVICE_REGISTRATION_ADDRESS);
    }

    public int getServerPort() {
        return Integer.parseInt(configBundle.getString(SERVER_PORT));
    }

    public int getMqttPort() {
        return Integer.parseInt(configBundle.getString(MQTT_PORT));
    }
}
