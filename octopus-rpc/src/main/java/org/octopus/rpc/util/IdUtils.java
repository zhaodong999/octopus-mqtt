package org.octopus.rpc.util;

import java.util.UUID;

public class IdUtils {
 
    //这里的0，0分别是
    // * @param workerId 工作ID (0~31)
    // * @param datacenterId 数据中心ID (0~31)，可以写在配置文件中。
    private static UniqueGenerate idWorker = new UniqueGenerate(0, 0);
 
    public static Long getUniqueIdBySnakeflow(){
        return idWorker.nextId();
    }
 
    public static String getUniqueIdByUUid(){
        return UUID.randomUUID().toString().replace("-", "");
    }
 
}