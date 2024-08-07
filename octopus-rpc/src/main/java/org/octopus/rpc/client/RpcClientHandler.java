package org.octopus.rpc.client;

import com.codahale.metrics.Meter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.octopus.monitor.metric.MetricRegistryType;
import org.octopus.monitor.metric.MetricsRegistryManager;
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

    private static final Meter RPC_CLIENT_REQUEST_EXCEPTION = MetricsRegistryManager.getInstance().getRegistry(MetricRegistryType.RPC).meter("rpc.client.request.exception");

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcMsg msg) throws Exception {
        if (msg.getFixedHeader().getProtoCommand() == ProtoCommand.PONG) {
            LOGGER.debug("rpc receive pong");
            return;
        }

        if (msg.getFixedHeader().getProtoCommand() == ProtoCommand.RESPONSE) {
            VariableHeader variableHeader = msg.getVariableHeader();
            long trackId = variableHeader.getTrackerId();
            LOGGER.info("receive msg trackId : {}", trackId);

            CompletableFuture<Rpc.RpcResponse> callBack = RequestHolder.get(trackId);
            if (callBack == null) {
                return;
            }

            if (variableHeader.getSerializeType() != SerializeType.PROTO) {
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
                LOGGER.debug("rpc send ping");
                ctx.writeAndFlush(RpcMsg.PING);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("rpc client error", cause);
        RPC_CLIENT_REQUEST_EXCEPTION.mark();
        ctx.close();
    }
}
