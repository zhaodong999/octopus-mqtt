package org.octopus.rpc.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.octopus.rpc.ProtoCommand;
import org.octopus.rpc.RpcMsg;
import org.octopus.rpc.SerializeType;
import org.octopus.rpc.VariableHeader;
import org.octopus.proto.rpc.Rpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class RpcClientHandler extends SimpleChannelInboundHandler<RpcMsg> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcClientHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcMsg msg) throws Exception {
        if (msg.getFixedHeader().getProtoCommand() == ProtoCommand.Pong) {
            LOGGER.info("rpc receive pong");
            return;
        }

        if (msg.getFixedHeader().getProtoCommand() == ProtoCommand.Response) {
            VariableHeader variableHeader = msg.getVariableHeader();
            long trackId = variableHeader.getTrackerId();
            LOGGER.info("receive msg trackId : {}", trackId);

            CompletableFuture<Rpc.RpcResponse> callBack = RequestHolder.get(trackId);
            if (callBack == null) {
                return;
            }

            if (variableHeader.getSerializeType() != SerializeType.Proto) {
                callBack.completeExceptionally(new Throwable());
            }

            byte[] payLoad = msg.getPayLoad();
            Rpc.RpcResponse rpcResponse = Rpc.RpcResponse.parseFrom(payLoad);
            callBack.complete(rpcResponse);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                LOGGER.warn("rpc client close conn when read idle");
                ctx.close();
            } else if (e.state() == IdleState.WRITER_IDLE) {
                LOGGER.info("rpc send ping");
                ctx.writeAndFlush(RpcMsg.PING);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("rpc client error", cause);
        ctx.close();
    }
}
