package org.octopus.rpc.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class IpUtils {

    private final static Logger LOGGER = LoggerFactory.getLogger(IpUtils.class);

    public static String getIp() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress()
                            && inetAddress.isSiteLocalAddress()) {
                        String ipAddress = inetAddress.getHostAddress();
                        LOGGER.info("本机IP地址：{}", ipAddress);
                        return ipAddress;
                    }
                }
            }
        } catch (SocketException e) {
            LOGGER.error("get ip err", e);
        }

        return null;
    }
}
