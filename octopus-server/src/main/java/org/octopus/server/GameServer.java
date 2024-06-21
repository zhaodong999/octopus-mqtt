package org.octopus.server;

import com.alibaba.nacos.api.exception.NacosException;
import org.octopus.rpc.cluster.RpcClusterFactory;
import org.octopus.rpc.cluster.RpcServiceLocator;
import org.octopus.rpc.server.RpcServer;
import org.octopus.rpc.service.RpcProxyManager;
import org.octopus.server.config.ServerConfigManager;
import org.octopus.server.service.ServiceOne;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(GameServer.class);

    public void start() throws NacosException {
        //注册服务
        RpcProxyManager rpcProxyManager = new RpcProxyManager();
        rpcProxyManager.register(new ServiceOne());

        RpcServiceLocator rpcServiceLocator = new RpcServiceLocator();
        rpcServiceLocator.connectCluster(ServerConfigManager.getInstance().getServiceRegistrationAddr());

        RpcClusterFactory.init(rpcServiceLocator);
        //启动rpc 监听端口，
        RpcServer rpcServer = new RpcServer(ServerConfigManager.getInstance().getServerPort(), rpcProxyManager, rpcServiceLocator);
        try {
            rpcServer.start();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                rpcServer.close();
            } catch (Exception e) {
                LOGGER.error("server close err", e);
            }
        }));
    }

    public static void main(String[] args) {
        GameServer gameServer = new GameServer();
        try {
            gameServer.start();
        } catch (NacosException e) {
            LOGGER.error("server start err", e);
            System.exit(0);
        }
    }
}
