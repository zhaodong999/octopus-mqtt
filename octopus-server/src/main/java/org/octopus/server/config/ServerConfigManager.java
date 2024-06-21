package org.octopus.server.config;

import java.util.ResourceBundle;

public class ServerConfigManager {

    private static ServerConfigManager instance;

    /* 配置文件名称 */
    private static final String CONF_FILE = "config";

    /* zookeeper 连接串*/
    private static final String SRVICE_REGISTRATION_ADDRESS = "service.registration.address";

    /* 服务监听端口*/
    private static final String SERVER_PORT = "server.port";

    private static final String MQTT_PORT = "mqtt.port";

    private final ResourceBundle configBundle;

    private ServerConfigManager() {
        configBundle = ResourceBundle.getBundle(CONF_FILE);
    }

    public static ServerConfigManager getInstance() {
        if (instance == null) {
            instance = new ServerConfigManager();
        }

        return instance;
    }

    public String getServiceRegistrationAddr() {
        return configBundle.getString(SRVICE_REGISTRATION_ADDRESS);
    }

    public int getServerPort() {
        return Integer.parseInt(configBundle.getString(SERVER_PORT));
    }

    public int getMqttPort() {
        return Integer.parseInt(configBundle.getString(MQTT_PORT));
    }
}
