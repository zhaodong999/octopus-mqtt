package org.octopus.server.service;

import com.google.protobuf.Any;
import com.google.protobuf.StringValue;
import org.octopus.proto.gateway.Server;
import org.octopus.rpc.client.RpcInvoker;
import org.octopus.rpc.exception.RpcClientException;
import org.octopus.rpc.server.anno.RpcMethod;
import org.octopus.rpc.server.anno.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RpcService(name = "msgService")
public class MsgService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MsgService.class);

    @RpcMethod(name = "receive")
    public void test(String param) {
        Server.ServerMessage.Builder builder = Server.ServerMessage.newBuilder();
        builder.setCmd(1);
        builder.setIdentity("zd");
        builder.setTopic("/sys/game");
        builder.setBody(Any.pack(StringValue.of("test" + param)));

        try {
            RpcInvoker.invokeOneway("gate", "publish", builder.build(), "zd");
        } catch (RpcClientException e) {
            LOGGER.info("test invoke rpc err", e);
        }
    }
}
