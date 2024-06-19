package org.octopus.rpc;

import java.util.Objects;

public class EndPoint {
    private String host;
    private int port;

    public static EndPoint of(String host, int port) {
        return new EndPoint(host, port);
    }

    public EndPoint(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EndPoint endPoint = (EndPoint) o;
        return port == endPoint.port && Objects.equals(host, endPoint.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }
}
