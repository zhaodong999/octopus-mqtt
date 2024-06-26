package org.octopus.rpc;

public enum ProtoCommand {

        /**
         * request,response 都是有payload
         */
        REQUEST(1),

        RESPONSE(2),

        /**
         * ping,pong 没有payload, 对应序列化，长度， requestId也没有
         */
        PING(3),

        PONG(4),

        UNKOWN(0);

        private final int code;

        ProtoCommand(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static ProtoCommand of(int code){
            for (ProtoCommand value : values()) {
                if(value.getCode() == code){
                    return value;
                }
            }

            return UNKOWN;
        }
    }