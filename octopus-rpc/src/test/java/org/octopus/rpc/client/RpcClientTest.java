package org.octopus.rpc.client;

import com.google.protobuf.Any;
import com.google.protobuf.Int32Value;
import com.google.protobuf.StringValue;
import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.octopus.rpc.EndPoint;
import org.octopus.rpc.exception.RpcClientException;
import org.octopus.proto.rpc.Rpc;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

class RpcClientTest {

    private static NioEventLoopGroup nioEventLoopGroup;

    @BeforeAll
    static void init() {
        nioEventLoopGroup = new NioEventLoopGroup();
    }

    @BeforeAll
    static void close(){
        nioEventLoopGroup.shutdownGracefully();
    }

    @Test
    void invokeTest() throws RpcClientException, IOException, ExecutionException, InterruptedException {
        //建立连接
        RpcClient rpcClient = new RpcClient(EndPoint.of("localhost", 8880), nioEventLoopGroup);

        rpcClient.connect();
        Any params = Any.pack(Int32Value.of(1));

        //service login,  method say,  param rpcClient
        Rpc.RpcRequest rpcRequest = Rpc.RpcRequest.newBuilder().setService("serviceDemo").setMethod("test").addArgs(params).build();
        CompletableFuture<Rpc.RpcResponse> call = rpcClient.call(rpcRequest);

        //同步获得结果
        Rpc.RpcResponse rpcResponse = call.get();
        Any result = rpcResponse.getResult();
        StringValue unpack = result.unpack(StringValue.class);
        System.out.println(unpack.getValue());
        rpcClient.close();
    }

    @Test
    void invokeOneWay() throws Exception {
        NioEventLoopGroup nioEventLoopGroup = new NioEventLoopGroup();
        //建立连接
        RpcClient rpcClient = new RpcClient(EndPoint.of("localhost", 8880), nioEventLoopGroup);

        rpcClient.connect();
        Any params = Any.pack(Int32Value.of(1));

        //service login,  method say,  param rpcClient
        Rpc.RpcRequest rpcRequest = Rpc.RpcRequest.newBuilder().setService("serviceDemo").setMethod("oneway").addArgs(params).build();
        rpcClient.callOneway(rpcRequest);

        Thread.sleep(1000);
        rpcClient.close();
    }
}
