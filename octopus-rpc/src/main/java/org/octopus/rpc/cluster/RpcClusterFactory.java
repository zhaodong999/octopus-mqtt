package org.octopus.rpc.cluster;

import org.octopus.rpc.client.RpcClient;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 仅在需要调用的时候，才初始化连接外壳，减少无效实例占用内存
 */
public class RpcClusterFactory {

    private RpcClusterFactory(){}

    // key-> serviceName
    private static final ConcurrentMap<String, RpcCluster> RPC_CLIENT_CLUSTERS = new ConcurrentHashMap<>();

    private static RpcServiceLocator rpcServiceLocator;

    public static void init(RpcServiceLocator _rpcServiceLocator){
        rpcServiceLocator = _rpcServiceLocator;
    }

    public static RpcClient getRpcClient(String serviceName, BalanceType balanceType, String id) {
        if (RPC_CLIENT_CLUSTERS.containsKey(serviceName)) {
            RpcCluster rpcCluster = RPC_CLIENT_CLUSTERS.get(serviceName);
            return rpcCluster.getRpcClient(balanceType, id);
        }

        RpcCluster rpcCluster = new RpcCluster(serviceName, rpcServiceLocator);
        RPC_CLIENT_CLUSTERS.put(serviceName, rpcCluster);
        return rpcCluster.getRpcClient(balanceType, id);
    }
}
