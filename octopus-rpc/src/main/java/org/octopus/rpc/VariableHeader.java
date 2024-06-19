package org.octopus.rpc;

public class VariableHeader {

    private long trackerId;

    private short length;

    private SerializeType serializeType;


    public VariableHeader(long trackerId, short length, SerializeType serializeType) {
        this.trackerId = trackerId;
        this.length = length;
        this.serializeType = serializeType;
    }

    public VariableHeader(SerializeType serializeType) {
        this.serializeType = serializeType;
    }

    public VariableHeader(long trackerId, SerializeType serializeType) {
        this.trackerId = trackerId;
        this.serializeType = serializeType;
    }

    public long getTrackerId() {
        return trackerId;
    }

    public void setTrackerId(long trackerId) {
        this.trackerId = trackerId;
    }

    public short getLength() {
        return length;
    }

    public void setLength(short length) {
        this.length = length;
    }

    public SerializeType getSerializeType() {
        return serializeType;
    }

    public void setSerializeType(SerializeType serializeType) {
        this.serializeType = serializeType;
    }
}
