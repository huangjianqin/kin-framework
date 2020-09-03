package org.kin.framework.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author huangjianqin
 * @date 2020/9/3
 */
public class JSONTest {
    public static void main(String[] args) {
        Map<String, Object> params = new HashMap<>();
        params.put("11", 11);
        params.put("22", 22);
        params.put("33", 33);

        String json = JSON.write(params);
        System.out.println(json);
        System.out.println(params);
        System.out.println(JSON.read(json, Map.class));
    }
}
