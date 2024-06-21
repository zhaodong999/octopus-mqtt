package org.octopus.gateway;


import org.octopus.gateway.config.GatewayConfigManager;
import org.octopus.gateway.server.MqttServer;
import org.octopus.gateway.service.SendService;
import org.octopus.rpc.cluster.RpcClusterFactory;
import org.octopus.rpc.cluster.RpcServiceLocator;
import org.octopus.rpc.exception.RpcRuntimeException;
import org.octopus.rpc.server.RpcServer;
import org.octopus.rpc.service.RpcProxyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


public class GateWayServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(GateWayServer.class);

    private void start() throws RpcRuntimeException {
        RpcProxyManager rpcProxyManager = new RpcProxyManager();
        rpcProxyManager.register(new SendService());

        String addr = GatewayConfigManager.getInstance().getServiceRegistrationAddr();
        try (RpcServiceLocator rpcServiceLocator = new RpcServiceLocator();
             RpcServer rpcServer = new RpcServer(GatewayConfigManager.getInstance().getServerPort(), rpcProxyManager, rpcServiceLocator);
             MqttServer mqttServer = new MqttServer(GatewayConfigManager.getInstance().getMqttPort())) {

            // 连接集群
            rpcServiceLocator.connectCluster(addr);
            RpcClusterFactory.init(rpcServiceLocator);

            // 启动服务
            rpcServer.start();

            // 启动网关
            mqttServer.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() ->
            {
                try {
                    rpcServer.close();
                } catch (Exception e) {
                    LOGGER.error("rpc server close err", e);
                }

                try {
                    rpcServiceLocator.close();
                } catch (Exception e) {
                    LOGGER.error("rpc service locator close err", e);
                }

                try {
                    mqttServer.close();
                } catch (Exception e) {
                    LOGGER.error("mqtt server close err", e);
                }
            }));
        } catch (RpcRuntimeException | IOException e) {
            LOGGER.error("connect cluster err", e);
        } catch (InterruptedException e) {
            LOGGER.error("mqtt service start err", e);
            Thread.currentThread().interrupt();
        }


    }

    public static void main(String[] args) {
        GateWayServer gateWayServer = new GateWayServer();
        try {
            gateWayServer.start();
        } catch (RpcRuntimeException e) {
            LOGGER.error("start err", e);
            System.exit(1);
        }
    }
}
