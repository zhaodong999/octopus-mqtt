package org.octopus.rpc.exception;

public class RpcTimeoutException extends RpcException {
    public RpcTimeoutException(String message) {
        super(message);
    }
}
