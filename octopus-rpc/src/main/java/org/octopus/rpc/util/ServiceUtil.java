package org.octopus.rpc.util;

public class ServiceUtil {

    private ServiceUtil(){}

    public static String firstUpperCase(String name){
        String first = name.substring(0,1).toUpperCase();
        return first + name.substring(1);
    }
}
