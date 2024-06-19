package org.octopus.rpc;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

public class RpcDecoder extends ReplayingDecoder<RpcDecoder.DecoderState> {


    private FixedHeader fixedHeader;
    private VariableHeader variableHeader;

    public RpcDecoder() {
        super(DecoderState.READ_MAGIC);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch (state()) {
            case READ_MAGIC:
                int magic = in.readUnsignedShort();
                if (magic != RpcMsg.MAGIC) {
                    ctx.close();
                }
                fixedHeader = new FixedHeader(RpcMsg.MAGIC);
                checkpoint(DecoderState.READ_COMMAND);
            case READ_COMMAND:
                short command = in.readUnsignedByte();
                fixedHeader.setVersion(command >>> 4);
                ProtoCommand protoCommand = ProtoCommand.of(command & 0x0F);
                fixedHeader.setProtoCommand(protoCommand);

                if (protoCommand == ProtoCommand.Ping || protoCommand == ProtoCommand.Pong) {
                    RpcMsg rpcMsg = new RpcMsg(protoCommand);
                    checkpoint(DecoderState.READ_MAGIC);
                    out.add(rpcMsg);
                    fixedHeader = null;
                    break;
                }

                checkpoint(DecoderState.READ_SERIALIZE);
            case READ_SERIALIZE:
                short serializeCode = in.readUnsignedByte();
                variableHeader = new VariableHeader(SerializeType.of(serializeCode));
                checkpoint(DecoderState.READ_LENGTH);
            case READ_LENGTH:
                int length = in.readUnsignedShort();
                variableHeader.setLength((short) length);
                checkpoint(DecoderState.READ_REQUEST_ID);
            case READ_REQUEST_ID:
                long trackerId = in.readLong();
                variableHeader.setTrackerId(trackerId);
                checkpoint(DecoderState.READ_PAYLOAD);
            case READ_PAYLOAD:
                byte[] data = new byte[variableHeader.getLength()];
                in.readBytes(data);
                RpcMsg rpcMsg = new RpcMsg(fixedHeader, variableHeader, data);
                checkpoint(DecoderState.READ_MAGIC);
                out.add(rpcMsg);

                fixedHeader = null;
                variableHeader = null;
                break;
        }
    }

    enum DecoderState {
        READ_MAGIC,
        READ_COMMAND,
        READ_SERIALIZE,
        READ_LENGTH,
        READ_REQUEST_ID,
        READ_PAYLOAD
    }
}
