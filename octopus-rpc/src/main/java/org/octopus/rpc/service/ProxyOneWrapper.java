package org.octopus.rpc.service;

import com.google.protobuf.Any;

public abstract class ProxyOneWrapper {

    public abstract void executor(Any... params);
}
