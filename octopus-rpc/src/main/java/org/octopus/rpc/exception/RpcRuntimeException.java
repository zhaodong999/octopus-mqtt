package org.octopus.rpc.exception;

public class RpcRuntimeException extends RuntimeException{

    private static final long serialVersionUID = 1L;

    public RpcRuntimeException(String message)
    {
        super(message);
    }
}
