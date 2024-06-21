package org.octopus.server.service;

import com.google.protobuf.Any;
import com.google.protobuf.StringValue;
import org.octopus.proto.gateway.Server;
import org.octopus.proto.rpc.Rpc;
import org.octopus.rpc.client.RpcClient;
import org.octopus.rpc.cluster.BalanceType;
import org.octopus.rpc.cluster.RpcClusterFactory;
import org.octopus.rpc.exception.RpcClientException;
import org.octopus.rpc.server.anno.RpcMethod;
import org.octopus.rpc.server.anno.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RpcService(name = "login")
public class ServiceOne {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceOne.class);

    @RpcMethod(name = "say")
    public String say(String name) {
        return "hello: " + name;
    }


    @RpcMethod(name = "test")
    public void test(String param) {
        RpcClient rpcClient = RpcClusterFactory.getRpcClient("gate", BalanceType.HASH, "userId");

        Server.ServerMessage.Builder builder = Server.ServerMessage.newBuilder();
        builder.setCmd(1);
        builder.setId("userId_001");
        builder.setTopic("/sys/game");
        builder.setBody(Any.pack(StringValue.of("test" + param)));

        Rpc.RpcRequest.Builder requesBuilder = Rpc.RpcRequest.newBuilder();
        Rpc.RpcRequest rpcRequest = requesBuilder.setService("gate").setMethod("publish").addArgs(Any.pack(builder.build())).build();
        try {
            rpcClient.callOneway(rpcRequest);
        } catch (RpcClientException e) {
            LOGGER.info("test invoke rpc err", e);
        }
    }
}
