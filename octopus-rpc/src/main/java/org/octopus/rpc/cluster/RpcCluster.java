package org.octopus.rpc.cluster;

import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.octopus.rpc.EndPoint;
import org.octopus.rpc.client.RpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class RpcCluster implements Observer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcCluster.class);

    private final String name;

    private final RpcServiceLocator rpcServiceLocator;

    private final CopyOnWriteArrayList<RpcClient> rpcClients = new CopyOnWriteArrayList<>();

    public RpcCluster(String name, RpcServiceLocator rpcServiceLocator) {
        this.name = name;
        this.rpcServiceLocator = rpcServiceLocator;

        List<Instance> rpcInstances = this.rpcServiceLocator.getInstance(name);
        if (rpcInstances != null && !rpcInstances.isEmpty()) {
            for (Instance instance : rpcInstances) {
                LOGGER.info("add rpc client: {}\t{}", name, instance);
                initRpcClient(instance);
            }
        }

        rpcServiceLocator.addObserver(this);
    }

    private void initRpcClient(Instance instance) {
        EndPoint endPoint = EndPoint.of(instance.getIp(), instance.getPort());
        RpcClient rpcClient = RpcClientManager.getRpcClient(endPoint);
        rpcClients.add(rpcClient);
    }

    public RpcClient getRpcClient(BalanceType hash, String id) {
        HashFunction hashFunction = Hashing.murmur3_128();
        HashCode hashCode = hashFunction.hashBytes(id.getBytes(StandardCharsets.UTF_8));
        int bucket = Hashing.consistentHash(hashCode, rpcClients.size());
        return rpcClients.get(bucket);
    }

    @Override
    public void update(Observable o, Object arg) {
        if (!(o instanceof RpcServiceLocator)) {
            return;
        }

        NamingEvent namingEvent = (NamingEvent) arg;
        if (!namingEvent.getServiceName().equals(name)) {
            return;
        }

        List<Instance> currInstances = namingEvent.getInstances();
        List<Instance> lastInstances = rpcServiceLocator.getInstance(name);

        List<Instance> addInstances = currInstances.stream().filter(instance -> !lastInstances.contains(instance)).collect(Collectors.toList());
        if (!addInstances.isEmpty()) {
            addInstances.forEach(instance -> {
                LOGGER.info("rpc runtime add instance: {}", instance);
                EndPoint endPoint = EndPoint.of(instance.getIp(), instance.getPort());
                RpcClient rpcClient = RpcClientManager.getRpcClient(endPoint);
                rpcClients.add(rpcClient);
            });
        }

        List<Instance> removeInstances = lastInstances.stream().filter(instance -> !currInstances.contains(instance)).collect(Collectors.toList());
        if(!removeInstances.isEmpty()){
            removeInstances.forEach(instance ->{
                LOGGER.info("rpc runtime remove instance: {}", instance);
                Optional<RpcClient> optionalRpcClient = rpcClients.stream().filter(e -> Objects.equals(e.getEndPoint(), EndPoint.of(instance.getIp(), instance.getPort()))).findFirst();
                optionalRpcClient.ifPresent(rpcClients::remove);
            });
        }
    }
}
