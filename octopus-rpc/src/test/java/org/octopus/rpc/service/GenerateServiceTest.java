package org.octopus.rpc.service;

import com.google.protobuf.Any;
import com.google.protobuf.Int32Value;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.octopus.rpc.exception.RpcServiceNotFound;


class GenerateServiceTest {

    @Test
    void register() throws RpcServiceNotFound {
        ServiceDemo serviceDemo = new ServiceDemo();

        RpcProxyManager rpcProxyManager = new RpcProxyManager();
        Assertions.assertDoesNotThrow(() -> {
            rpcProxyManager.register(serviceDemo);
        });

        Int32Value intValue = Int32Value.newBuilder().setValue(123).build();
        rpcProxyManager.invoke("serviceDemo", "oneway", Any.pack(intValue));

    }
}
