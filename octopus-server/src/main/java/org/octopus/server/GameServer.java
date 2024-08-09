package org.octopus.server;

import org.octopus.db.SessionFactoryUtil;
import org.octopus.monitor.metric.MetricRegistryType;
import org.octopus.monitor.metric.MetricsRegistryManager;
import org.octopus.rpc.cluster.RpcClusterFactory;
import org.octopus.rpc.cluster.RpcServiceLocator;
import org.octopus.rpc.server.RpcServer;
import org.octopus.rpc.service.RpcProxyManager;
import org.octopus.server.config.ServerConfigManager;
import org.octopus.server.service.AuthService;
import org.octopus.server.service.MsgService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class GameServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(GameServer.class);

    public void start() throws IOException {
        MetricsRegistryManager.getInstance().register(MetricRegistryType.RPC, "com.octopus.monitor.rpc");

        //注册服务
        RpcProxyManager rpcProxyManager = new RpcProxyManager();
        rpcProxyManager.register(new AuthService());
        rpcProxyManager.register(new MsgService());

        //初始化数据库
        SessionFactoryUtil.getInstance().init();

        //启动rpc 监听端口，
        try (RpcServiceLocator rpcServiceLocator = new RpcServiceLocator();
             RpcServer rpcServer = new RpcServer(ServerConfigManager.getInstance().getServerPort(), rpcProxyManager, rpcServiceLocator)) {

            //初始化集群
            rpcServiceLocator.connectCluster(ServerConfigManager.getInstance().getServiceRegistrationAddr());
            RpcClusterFactory.init(rpcServiceLocator);

            rpcServer.start(true);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    rpcServiceLocator.close();
                } catch (Exception e) {
                    LOGGER.error("rpc service locator close err", e);
                }

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
        try{
            gameServer.start();
        }catch (Exception e){
            LOGGER.error("server start err", e);
            System.exit(1);
        }
    }
}
