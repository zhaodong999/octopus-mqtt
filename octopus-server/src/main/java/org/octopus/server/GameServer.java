package org.octopus.server;

import org.octopus.rpc.cluster.RpcClusterFactory;
import org.octopus.rpc.cluster.RpcServiceLocator;
import org.octopus.rpc.server.RpcServer;
import org.octopus.rpc.service.RpcProxyManager;
import org.octopus.server.config.ServerConfigManager;
import org.octopus.server.service.ServiceOne;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class GameServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(GameServer.class);

    public void start() {
        //注册服务
        RpcProxyManager rpcProxyManager = new RpcProxyManager();
        rpcProxyManager.register(new ServiceOne());

        RpcServiceLocator rpcServiceLocator = new RpcServiceLocator();
        rpcServiceLocator.connectCluster(ServerConfigManager.getInstance().getServiceRegistrationAddr());

        //初始化集群
        RpcClusterFactory.init(rpcServiceLocator);

        //启动rpc 监听端口，
        try (RpcServer rpcServer = new RpcServer(ServerConfigManager.getInstance().getServerPort(), rpcProxyManager, rpcServiceLocator)) {
            rpcServer.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    rpcServer.close();
                } catch (Exception e) {
                    LOGGER.error("server close err", e);
                }
            }));
        } catch (IOException | InterruptedException e) {
            LOGGER.error("server start err", e);
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        GameServer gameServer = new GameServer();
        gameServer.start();
    }
}
