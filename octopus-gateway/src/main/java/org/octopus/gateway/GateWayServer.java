package org.octopus.gateway;


import com.alibaba.nacos.api.exception.NacosException;
import org.octopus.gateway.config.ConfigManager;
import org.octopus.gateway.server.MqttServer;
import org.octopus.gateway.service.SendService;
import org.octopus.rpc.cluster.RpcClusterFactory;
import org.octopus.rpc.cluster.RpcServiceLocator;
import org.octopus.rpc.server.RpcServer;
import org.octopus.rpc.service.RpcProxyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class GateWayServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(GateWayServer.class);

    private void start() throws RuntimeException, NacosException, InterruptedException {
        RpcProxyManager rpcProxyManager = new RpcProxyManager();
        rpcProxyManager.register(new SendService());

        String addr = ConfigManager.getInstance().getServiceRegistrationAddr();
        RpcServiceLocator rpcServiceLocator = new RpcServiceLocator();
        rpcServiceLocator.connectCluster(addr);
        RpcClusterFactory.init(rpcServiceLocator);

        //启动rpc 监听端口，
        RpcServer rpcServer = new RpcServer(ConfigManager.getInstance().getServerPort(), rpcProxyManager, rpcServiceLocator);
        rpcServer.start();
        LOGGER.info("rpc server start complete");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                rpcServer.close();
            } catch (Exception e) {
                LOGGER.error("rpc server close err", e);
            }
        }));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                rpcServiceLocator.close();
            } catch (Exception e) {
                LOGGER.error("rpc service locator close err", e);
            }
        }));

        try (MqttServer mqttServer = new MqttServer(ConfigManager.getInstance().getMqttPort())) {
            mqttServer.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    mqttServer.close();
                } catch (Exception e) {
                    LOGGER.error("mqtt server close err", e);
                }

            }));
        } catch (IOException e) {
            LOGGER.error("mqtt service start err", e);
        }
    }

    public static void main(String[] args) {
        GateWayServer gateWayServer = new GateWayServer();
        try {
            gateWayServer.start();
        } catch (NacosException | InterruptedException e) {
            LOGGER.error("start err", e);
            throw new RuntimeException(e);
        }
    }
}
