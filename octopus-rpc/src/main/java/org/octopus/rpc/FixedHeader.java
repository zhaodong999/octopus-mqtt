package org.octopus.rpc;

public class FixedHeader {

    private int magic;
    private int version;

    /**
     * 协议指令
     * request, response, ping, pong
     */
    private ProtoCommand protoCommand;


    public FixedHeader(int magic, int version, ProtoCommand protoCommand) {
        this.magic = magic;
        this.version = version;
        this.protoCommand = protoCommand;
    }

    public FixedHeader(int magic) {
        this.magic = magic;
    }

    public int getMagic() {
        return magic;
    }

    public void setMagic(int magic) {
        this.magic = magic;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public ProtoCommand getProtoCommand() {
        return protoCommand;
    }

    public void setProtoCommand(ProtoCommand protoCommand) {
        this.protoCommand = protoCommand;
    }
}
