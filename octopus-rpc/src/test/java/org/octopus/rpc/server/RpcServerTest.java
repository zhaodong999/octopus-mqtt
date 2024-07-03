package org.octopus.rpc.server;

import org.junit.jupiter.api.Test;
import org.octopus.rpc.cluster.RpcServiceLocator;
import org.octopus.rpc.service.RpcProxyManager;
import org.octopus.rpc.service.ServiceDemo;

class RpcServerTest {

    @Test
    void testServer() {
        //注册服务,使用javassist生产服务代理
        RpcProxyManager rpcProxyManager = new RpcProxyManager();
        rpcProxyManager.register(new ServiceDemo());

        //服务注册，使用Nacos将服务注册到集群
        RpcServiceLocator rpcServiceLocator = new RpcServiceLocator();
        rpcServiceLocator.connectCluster("localhost:8848");

        //启动rpc 监听端口，
        RpcServer rpcServer = new RpcServer(8880, rpcProxyManager, rpcServiceLocator);
        try {
            rpcServer.start(true);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
