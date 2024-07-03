package org.octopus.gateway.service;

import io.netty.channel.ChannelHandlerContext;
import org.octopus.gateway.server.ConnectionManager;
import org.octopus.gateway.server.MqttMsgUtil;
import org.octopus.proto.gateway.Server;
import org.octopus.rpc.server.anno.RpcMethod;
import org.octopus.rpc.server.anno.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RpcService(name = "gate")
public class SendService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendService.class);

    @RpcMethod(name = "publish")
    public void sendMsg(Server.ServerMessage serverMessage) {
        LOGGER.info("rpc receive: {}\t{}", serverMessage.getIdentity(), serverMessage.getTopic());

        //get connection channel
        ChannelHandlerContext ctx = ConnectionManager.getCtx(serverMessage.getIdentity());
        if (ctx == null) {
            LOGGER.error("client not exist with identity: {}", serverMessage.getIdentity());
            return;
        }

        MqttMsgUtil.sendPubMsg(ctx, serverMessage);
    }
}
