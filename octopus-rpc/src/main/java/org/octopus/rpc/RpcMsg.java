package org.octopus.rpc;

public class RpcMsg {

    public static final int MAGIC = 0xE888;

    public static final int VERSION = 1;

    private FixedHeader fixedHeader;

    private VariableHeader variableHeader;

    private byte[] payLoad;

    public static final RpcMsg PING = new RpcMsg(ProtoCommand.PING);

    public static final RpcMsg PONG = new RpcMsg(ProtoCommand.PONG);

    public RpcMsg(ProtoCommand protoCommand) {
        this.fixedHeader = new FixedHeader(MAGIC, VERSION, protoCommand);
    }

    public RpcMsg(FixedHeader fixedHeader, VariableHeader variableHeader, byte[] data) {
        this.fixedHeader = fixedHeader;
        this.variableHeader = variableHeader;
        this.payLoad = data;
    }

    public FixedHeader getFixedHeader() {
        return fixedHeader;
    }

    public void setFixedHeader(FixedHeader fixedHeader) {
        this.fixedHeader = fixedHeader;
    }

    public VariableHeader getVariableHeader() {
        return variableHeader;
    }

    public void setVariableHeader(VariableHeader variableHeader) {
        this.variableHeader = variableHeader;
    }

    public byte[] getPayLoad() {
        return payLoad;
    }

    public void setPayLoad(byte[] payLoad) {
        variableHeader.setLength((short) payLoad.length);
        this.payLoad = payLoad;
    }


}
