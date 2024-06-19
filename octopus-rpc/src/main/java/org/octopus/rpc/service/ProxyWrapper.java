package org.octopus.rpc.service;

import com.google.protobuf.Any;

public abstract class ProxyWrapper {

    public abstract Any executor(Any... params);

}
