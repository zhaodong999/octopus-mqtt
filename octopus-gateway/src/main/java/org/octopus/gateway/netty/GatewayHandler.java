package org.octopus.gateway.netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.MqttMessage;
import org.octopus.gateway.server.GatewayProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelHandler.Sharable
public class GatewayHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayHandler.class);

    public static final GatewayHandler INSTANCE = new GatewayHandler();

    private GatewayHandler() {
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        MqttMessage message = (MqttMessage) msg;
        LOGGER.info("Received MQTT message: {}", message);

        switch (message.fixedHeader().messageType()) {
            case CONNECT:
                GatewayProcessor.CONNECT.handle(ctx, message);
                break;
            case PUBLISH:
                GatewayProcessor.PUBLISH.handle(ctx, message);
                break;
            case PUBREL:
                GatewayProcessor.PUBREL.handle(ctx, message);
                break;
            case SUBSCRIBE:
                GatewayProcessor.SUBSCRIBE.handle(ctx, message);
                break;
            case PINGREQ:
                GatewayProcessor.PING.handle(ctx, message);
                break;
            default:
                LOGGER.error("mqtt msg err");
                break;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("handle error", cause);
        ctx.close();
    }
}