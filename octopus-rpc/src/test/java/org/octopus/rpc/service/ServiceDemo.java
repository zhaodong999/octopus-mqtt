package org.octopus.rpc.service;


import org.octopus.rpc.server.anno.RpcMethod;
import org.octopus.rpc.server.anno.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RpcService(name = "serviceDemo")
public class ServiceDemo {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceDemo.class);

    @RpcMethod(name = "test")
    public String test(int name) {
        LOGGER.info("serviceDemo param is: {}", name);
        return "hello: " + name;
    }

    @RpcMethod(name = "oneway")
    public void oneway(int count) {
        LOGGER.info("oneway invoke: param is: {}", count);
    }
}
