package org.kin.framework.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author huangjianqin
 * @date 2021/12/8
 */
public class MedianEstimatorTest {
    public static void main(String[] args) {
        MedianEstimator medianEstimator = new MedianEstimator();

        Set<Integer> set = new HashSet<>();
        for (int i = 0; i < 1000000; i++) {
            int v = ThreadLocalRandom.current().nextInt(200);
            medianEstimator.insert(v);
            set.add(v);
        }

        List<Integer> list = new ArrayList<>(set);
        list.sort(Integer::compareTo);
        System.out.println(list.get(set.size() / 2));
        System.out.println(list.get(set.size() / 2 + 1));
        System.out.println(medianEstimator.estimation());
    }
}
