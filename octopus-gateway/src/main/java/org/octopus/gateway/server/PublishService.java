package org.octopus.gateway.server;

import org.octopus.proto.rpc.Rpc;
import org.octopus.rpc.client.RpcClient;
import org.octopus.rpc.cluster.BalanceType;
import org.octopus.rpc.cluster.RpcClusterFactory;
import org.octopus.rpc.exception.RpcClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublishService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublishService.class);

    private PublishService() {
    }

    public static void handleOne(Rpc.RpcRequest request, String id) {
        String serviceName = request.getService();
        LOGGER.info("rpc client send: {}\t{}\t{}", request.getService(), request.getMethod(), id);

        try {
            RpcClient rpcClient = RpcClusterFactory.getRpcClient(serviceName, BalanceType.HASH, id);
            rpcClient.callOneway(request);
        } catch (RpcClientException e) {
            LOGGER.error("rpc client send msg err", e);
        }
    }

}

