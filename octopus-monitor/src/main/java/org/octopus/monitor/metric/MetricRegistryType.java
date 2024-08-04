package org.octopus.monitor.metric;

public enum MetricRegistryType {

    RPC("rpc"),
    SERVER("server"),
    GATEWAY("gateway");

    private MetricRegistryType(String name) {
        this.name = name;
    }

    private final String name;

    public String getName() {
        return name;
    }
}
