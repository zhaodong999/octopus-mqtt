package org.octopus.rpc.cluster;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;
import com.google.protobuf.Any;
import com.google.protobuf.StringValue;
import org.octopus.proto.rpc.Rpc;
import org.octopus.rpc.client.RpcClient;
import org.octopus.rpc.exception.ClusterLoadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Set;
import java.util.concurrent.*;


public class RpcServiceLocator extends Observable implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServiceLocator.class);
    private final ConcurrentMap<String, List<Instance>> serviceInstances = new ConcurrentHashMap<>();

    private final List<String> slfServiceNames = new ArrayList<>();
    private NamingService namingService;
    private static final int PAGE_SIZE = 20;
    private ScheduledExecutorService service;

    @Override
    public void close() throws IOException {
        if (service != null) {
            service.shutdown();
        }

        if (namingService != null) {
            try {
                namingService.shutDown();
            } catch (NacosException e) {
                LOGGER.error("shutdown naming service err", e);
            }
        }
    }

    public void connectCluster(String address) throws ClusterLoadException {
        try {
            namingService = NamingFactory.createNamingService(address);
        } catch (NacosException e) {
            throw new ClusterLoadException(e);
        }
    }

    public void registerInstance(Set<String> serviceNames, String ip, int port) {
        for (String serviceName : serviceNames) {
            try {
                namingService.registerInstance(serviceName, ip, port);
                slfServiceNames.add(serviceName);
                LOGGER.info("register service: {}\t{}\t{}", serviceName, ip, port);
            } catch (NacosException e) {
                LOGGER.error("register service err", e);
            }
        }
    }

    public void loadServices() {
        LOGGER.info("start schedule load remote rpc serivce");
        loadNewService();
        service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(() -> {
            try {
                loadNewService();
            } catch (Exception e) {
                LOGGER.error("pull instances err", e);
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    private void loadNewService() {
        int page = 1;

        try {
            while (true) {
                ListView<String> servicesOfServer = namingService.getServicesOfServer(page, PAGE_SIZE);
                if (servicesOfServer.getCount() == 0 || servicesOfServer.getCount() < PAGE_SIZE) {
                    break;
                }

                List<String> serviceNames = servicesOfServer.getData();
                for (String serviceName : serviceNames) {
                    //本机服务，不需要加载进行处理;服务已经初始化注册过了
                    if (slfServiceNames.contains(serviceName) || serviceInstances.containsKey(serviceName)) {
                        continue;
                    }

                    //注册服务， 注册服务的监听
                    List<Instance> allInstances = namingService.getAllInstances(serviceName);
                    serviceInstances.put(serviceName, allInstances);
                    LOGGER.info("load serviceData serviceName: {}", serviceName);
                    allInstances.forEach(instance -> LOGGER.info("serviceName: {}\t{}", instance.getIp(), instance.getPort()));
                    //更新当前的实例列表
                    namingService.subscribe(serviceName, e -> {
                        NamingEvent namingEvent = (NamingEvent) e;
                        LOGGER.info("subscribe serviceName: {}, event: {}", namingEvent.getServiceName(), namingEvent);
                        serviceInstances.put(serviceName, namingEvent.getInstances());
                        notifyObservers();
                    });
                }

                ++page;
            }
        } catch (Exception e) {
            LOGGER.error("load cluster info err", e);
        }
    }

    public static void main(String[] args) throws Exception {
        RpcClient rpcClient = RpcClusterFactory.getRpcClient("login", BalanceType.HASH, "userId_011");

        Any params = Any.pack(StringValue.of("rpcClient"));
        Rpc.RpcRequest rpcRequest = Rpc.RpcRequest.newBuilder().setService("login").setMethod("say").addArgs(params).build();
        CompletableFuture<Rpc.RpcResponse> call = rpcClient.call(rpcRequest);

        //同步获得结果
        Rpc.RpcResponse rpcResponse = call.get();
        Any result = rpcResponse.getResult();
        StringValue unpack = result.unpack(StringValue.class);
        LOGGER.info(unpack.getValue());

    }

    public List<Instance> getInstance(String name) {
        return serviceInstances.get(name);
    }


}
