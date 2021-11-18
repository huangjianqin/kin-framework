package org.kin.framework.utils;

/**
 * @author huangjianqin
 * @date 2021/11/19
 */
public class ConsistentHashTest {
    public static void main(String[] args) {
        ConsistentHash<String> consistentHash = new ConsistentHash<>();
        consistentHash.add("127.0.0.1:9000");
        consistentHash.add("127.0.0.1:9001");
        consistentHash.add("127.0.0.1:9002");
        consistentHash.add("127.0.0.1:9003");
        consistentHash.add("127.0.0.1:9004");

        System.out.println(consistentHash);
        System.out.println("------------------------------");
        for (int i = 0; i < 10; i++) {
            String key = "user" + i;
            System.out.println(key.hashCode());
            System.out.println(consistentHash.get(key));
            System.out.println("------------------------------");
        }
    }
}
