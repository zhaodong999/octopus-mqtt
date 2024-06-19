package org.octopus.rpc.client;

import io.netty.util.HashedWheelTimer;
import org.octopus.rpc.exception.RpcTimeoutException;
import org.octopus.proto.rpc.Rpc;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 回调超时问题在这里处理, 用netty的时间轮,请求30秒无应答后，返回超时异常
 */
public class RequestHolder {

    private RequestHolder() {
    }

    private static final ConcurrentHashMap<Long, CompletableFuture<Rpc.RpcResponse>> REQUESTS = new ConcurrentHashMap<>();

    private static final HashedWheelTimer TIMER = new HashedWheelTimer();

    public static void put(Long id, CompletableFuture<Rpc.RpcResponse> callBack) {
        REQUESTS.put(id, callBack);

        TIMER.newTimeout(timeout -> {
            REQUESTS.remove(id);
            callBack.completeExceptionally(new RpcTimeoutException());
        }, 30, TimeUnit.SECONDS);
    }

    public static CompletableFuture<Rpc.RpcResponse> get(Long id) {
        return REQUESTS.remove(id);
    }
}
