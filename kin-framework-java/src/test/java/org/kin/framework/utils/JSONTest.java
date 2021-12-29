package org.kin.framework.utils;

import java.util.HashMap;
import java.util.List;
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
        Map<String, Object> map = JSON.read(json, Map.class);
        System.out.println(map);

        String strListJson = "[1,2,3,4,5]";
        List<String> stringList = JSON.readList(strListJson, String.class);
        System.out.println(stringList.get(0));

        Integer i = JSON.read("null", Integer.class);
        System.out.println(i);
        Map<String, Object> d1MapData = new HashMap<>();
        d1MapData.put("f1", 1);
        d1MapData.put("f2", 1000000000L);
        System.out.println(JSON.convert(d1MapData, D1.class));
    }

    private static class D1 {
        private int f1;
        private long f2;

        //setter && getter
        public int getF1() {
            return f1;
        }

        public void setF1(int f1) {
            this.f1 = f1;
        }

        public long getF2() {
            return f2;
        }

        public void setF2(long f2) {
            this.f2 = f2;
        }

        @Override
        public String toString() {
            return "D1{" +
                    "f1=" + f1 +
                    ", f2=" + f2 +
                    '}';
        }
    }
}
