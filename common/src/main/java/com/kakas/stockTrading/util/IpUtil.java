package com.kakas.stockTrading.util;

import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
public class IpUtil {

    static final String ip;

    static final String hostId;

    static {
        ip = doGetIp();
        hostId = doGetHostId();
    }

    private static String doGetIp() {
        InetAddress addr = null;
        try {
            addr = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            log.warn("Host unknown!");
            throw new RuntimeException(e);
        }
        return addr == null ? "127.0.0.0" : addr.getHostAddress();
    }

    private static String doGetHostId() {
        return ip.replace('.', '_');
    }



    public static String getIp() {
        return ip;
    }

    public static  String getHostId() {
        return hostId;
    }
}
