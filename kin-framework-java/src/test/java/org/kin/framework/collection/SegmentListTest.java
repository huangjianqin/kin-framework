package org.kin.framework.collection;

/**
 * @author huangjianqin
 * @date 2021/11/5
 */
public class SegmentListTest {
    public static void main(String[] args) {
        SegmentList<Integer> list = new SegmentList<>(true);
        int base = 128;
        for (int i = 0; i < base * 4; i++) {
            list.add(i);
        }
        System.out.println(list);
        System.out.println(list.size());
        System.out.println(list.getSegmentSize());
        System.out.println("---------------------------");
        System.out.println(list.get(0));
        System.out.println(list.get(128 - 1));
        System.out.println("---------------------------");
        System.out.println(list.get(128));
        System.out.println(list.get(128 * 2 - 1));
        System.out.println("---------------------------");
        System.out.println(list.get(128 * 2));
        System.out.println(list.get(128 * 3 - 1));
        System.out.println("---------------------------");
        System.out.println(list.get(128 * 3));
        System.out.println(list.get(128 * 4 - 1));
        System.out.println("---------------------------");
        try {
            System.out.println(list.get(128 * 4));
            System.out.println(list.get(127 * 5));
        } catch (Exception e) {
            System.err.println(e);
        }
        System.out.println("---------------------------");
        list.removeFromFirstWhen(i -> i != 25);
        System.out.println(list);
        System.out.println("---------------------------");
        System.out.println(list.size());
        System.out.println(list.get(0));
        System.out.println(list.get(128 - 26));
        System.out.println("---------------------------");
        list.removeFromLastWhen(i -> i != 486);
        System.out.println(list);
        System.out.println("---------------------------");
        System.out.println(list.size());
        System.out.println(list.get(128 * 3));
        System.out.println(list.get(128 * 4 - 51));
        System.out.println("---------------------------");
        list.removeFromFirst(206);
        System.out.println(list.size());
        System.out.println(list);
        System.out.println("---------------------------");
        for (int i = 512; i < base * 7; i++) {
            list.add(i);
        }
        System.out.println(list.getSegmentSize());
        System.out.println(list);
    }
}
