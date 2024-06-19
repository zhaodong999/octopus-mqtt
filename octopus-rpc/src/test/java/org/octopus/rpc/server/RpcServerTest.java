package org.octopus.rpc.server;

import com.alibaba.nacos.api.exception.NacosException;
import org.junit.jupiter.api.Test;
import org.octopus.rpc.cluster.RpcServiceLocator;
import org.octopus.rpc.service.RpcProxyManager;
import org.octopus.rpc.service.ServiceDemo;

class RpcServerTest {

    @Test
    void server() throws NacosException {
        //注册服务
        RpcProxyManager rpcProxyManager = new RpcProxyManager();
        rpcProxyManager.register(new ServiceDemo());

        //服务发现
        RpcServiceLocator rpcServiceLocator = new RpcServiceLocator();
        rpcServiceLocator.connectCluster("localhost:8848");

        //启动rpc 监听端口，
        RpcServer rpcServer = new RpcServer(8880, rpcProxyManager, rpcServiceLocator);
        try {
            rpcServer.start();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
