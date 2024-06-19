package org.octopus.rpc;

public enum SerializeType {

        Unknown(0),
        Proto(1);

        private final int code;

        SerializeType(int code) {
            this.code = code;
        }

    public int getCode() {
            return code;
        }

    public static SerializeType of(short code) {
        for (SerializeType value : values()) {
            if(value.getCode() == code){
                return value;
            }
        }

        return Unknown;
    }
}