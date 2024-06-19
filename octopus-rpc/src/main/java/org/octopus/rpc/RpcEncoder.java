package org.octopus.rpc;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

public class RpcEncoder extends MessageToMessageEncoder<RpcMsg> {

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMsg msg, List<Object> out) throws Exception {
        FixedHeader fixedHeader = msg.getFixedHeader();
        ProtoCommand protoCommand = fixedHeader.getProtoCommand();
        if (protoCommand == ProtoCommand.Pong || protoCommand == ProtoCommand.Ping) {
            ByteBuf buffer = ctx.alloc().buffer(3);
            int header = fixedHeader.getVersion() << 4 | fixedHeader.getProtoCommand().getCode();
            buffer.writeShort(fixedHeader.getMagic()).writeByte(header);
            out.add(buffer);
        } else if (protoCommand == ProtoCommand.Request || protoCommand == ProtoCommand.Response) {
            ByteBuf buffer = ctx.alloc().buffer(6 + msg.getPayLoad().length + 8);

            int header = fixedHeader.getVersion() << 4 | fixedHeader.getProtoCommand().getCode();
            VariableHeader variableHeader = msg.getVariableHeader();
            buffer.writeShort(fixedHeader.getMagic())
                    .writeByte(header)
                    .writeByte(variableHeader.getSerializeType().getCode())
                    .writeShort(variableHeader.getLength())
                    .writeLong(variableHeader.getTrackerId())
                    .writeBytes(msg.getPayLoad());

            out.add(buffer);
        }
    }
}
