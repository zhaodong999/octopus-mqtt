package org.octopus.rpc.server;

import com.codahale.metrics.Meter;
import com.google.protobuf.Any;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.octopus.monitor.metric.MetricRegistryType;
import org.octopus.monitor.metric.MetricsRegistryManager;
import org.octopus.proto.rpc.Rpc;
import org.octopus.rpc.ProtoCommand;
import org.octopus.rpc.RpcMsg;
import org.octopus.rpc.SerializeType;
import org.octopus.rpc.VariableHeader;
import org.octopus.rpc.service.RpcProxyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class RpcServerHandler extends SimpleChannelInboundHandler<RpcMsg> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServerHandler.class);

    private static final Meter PRC_SERVER_REQUEST_METER_QPS = MetricsRegistryManager.getInstance().getRegistry(MetricRegistryType.RPC).meter("rpc.server.request.qps");

    private static final Meter PRC_SERVER_REQUEST_METER_EXCEPTION = MetricsRegistryManager.getInstance().getRegistry(MetricRegistryType.RPC).meter("rpc.server.request.exception");
    private final RpcProxyManager rpcServiceManager;

    public RpcServerHandler(RpcProxyManager rpcServiceManager) {
        this.rpcServiceManager = rpcServiceManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcMsg msg) throws Exception {
        if (msg.getFixedHeader().getProtoCommand() == ProtoCommand.PING) {
            ctx.writeAndFlush(RpcMsg.PONG);
            return;
        }

        if (msg.getFixedHeader().getProtoCommand() == ProtoCommand.REQUEST) {
            PRC_SERVER_REQUEST_METER_QPS.mark();
            byte[] payLoad = msg.getPayLoad();
            Rpc.RpcRequest rpcRequest = Rpc.RpcRequest.parseFrom(payLoad);
            Any[] params = new Any[rpcRequest.getArgsCount()];
            rpcRequest.getArgsList().toArray(params);

            LOGGER.info("receive packet: {}/{}", rpcRequest.getService(), rpcRequest.getMethod());
            CompletableFuture<Any> completableFuture = rpcServiceManager.invoke(rpcRequest.getService(), rpcRequest.getMethod(), params);
            if (completableFuture == null) {
                LOGGER.warn("rpc response null, trackId: {}", msg.getVariableHeader().getTrackerId());
                return;
            }

            completableFuture.whenComplete((any, throwable) -> {

                if (throwable != null) {
                    LOGGER.error("proxy invoke error: {}", msg.getVariableHeader().getTrackerId(), throwable);
                    Rpc.RpcResponse.Builder builder = Rpc.RpcResponse.newBuilder();
                    builder.setStatus(Rpc.RpcStatus.FAILURE);
                    builder.setReason(throwable.getMessage());
                    RpcMsg rpcMsg = new RpcMsg(ProtoCommand.RESPONSE);
                    VariableHeader variableHeader = new VariableHeader(msg.getVariableHeader().getTrackerId(), SerializeType.PROTO);
                    rpcMsg.setVariableHeader(variableHeader);
                    rpcMsg.setPayLoad(builder.build().toByteArray());
                    ctx.channel().writeAndFlush(rpcMsg);
                    return;
                }

                LOGGER.info("get proxy invoke result: {}", msg.getVariableHeader().getTrackerId());
                Rpc.RpcResponse.Builder builder = Rpc.RpcResponse.newBuilder();
                builder.setStatus(Rpc.RpcStatus.OK);
                builder.setResult(any);
                builder.build();

                RpcMsg rpcMsg = new RpcMsg(ProtoCommand.RESPONSE);
                VariableHeader variableHeader = new VariableHeader(msg.getVariableHeader().getTrackerId(), SerializeType.PROTO);
                rpcMsg.setVariableHeader(variableHeader);
                rpcMsg.setPayLoad(builder.build().toByteArray());

                LOGGER.info("send msg trackId: {}", msg.getVariableHeader().getTrackerId());
                ctx.channel().writeAndFlush(rpcMsg);
            });
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                LOGGER.warn("rpc server close conn when read idle");
                ctx.close();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("rpc server error", cause);
        PRC_SERVER_REQUEST_METER_EXCEPTION.mark();
        ctx.close();
    }
}
