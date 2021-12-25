package org.kin.framework.beans;

import com.google.common.base.Stopwatch;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

/**
 * @author huangjianqin
 * @date 2021/9/10
 */
public class BeanUtilsCopyTest {
    public static void main(String[] args) {
        Message message = new Message();

        message.setA(Byte.MIN_VALUE);
        message.setB(Short.MIN_VALUE);
        message.setC(Integer.MIN_VALUE);
        message.setD(Long.MIN_VALUE);
        message.setE(Float.MIN_VALUE);
        message.setF(Double.MIN_VALUE);
        message.setG("Hello Java Bean");
        message.setH(Byte.MAX_VALUE);
        message.setI(Short.MAX_VALUE);
        message.setJ(Integer.MAX_VALUE);
        message.setK(Long.MAX_VALUE);
        message.setL(Float.MAX_VALUE);
        message.setM(Double.MAX_VALUE);
        message.setList(Arrays.asList(1, 2, 3, 4, 5));
        message.setSet(new HashSet<>(Arrays.asList(1, 2, 3, 4, 5)));

        Map<Integer, Long> map1 = new HashMap<>();
        map1.put(1, 11L);
        map1.put(2, 22L);
        map1.put(3, 33L);
        map1.put(4, 44L);
        map1.put(5, 55L);
        message.setMap(map1);

        int[] ints = new int[]{1, 2, 3, 4, 5};
        MessageParent[] messageParents = new MessageParent[]{message.clone(), message.clone(), message.clone()};
        List<MessageParent> messageParentList = Arrays.asList(message.clone(), message.clone(), message.clone());
        Set<MessageParent> messageParentSet = new HashSet<>(Arrays.asList(message.clone(), message.clone(), message.clone()));

        Map<Integer, MessageParent> messageParentMap = new HashMap<>();
        messageParentMap.put(1, message.clone());
        messageParentMap.put(2, message.clone());
        messageParentMap.put(3, message.clone());
        messageParentMap.put(4, message.clone());
        messageParentMap.put(5, message.clone());

        int[][] intInts = new int[5][];
        intInts[0] = new int[]{1, 2, 3, 4, 5};
        intInts[1] = new int[]{1, 2, 3, 4, 5};
        intInts[2] = new int[]{1, 2, 3, 4, 5};
        intInts[3] = new int[]{1, 2, 3, 4, 5};
        intInts[4] = new int[]{1, 2, 3, 4, 5};

        MessageParent[][] beanMessageParents = new MessageParent[3][];
        beanMessageParents[0] = new MessageParent[]{message.clone(), message.clone(), message.clone()};
        beanMessageParents[1] = new MessageParent[]{message.clone(), message.clone(), message.clone()};
        beanMessageParents[2] = new MessageParent[]{message.clone(), message.clone(), message.clone()};

        List<List<MessageParent>> listList = new ArrayList<>();
        listList.add(Arrays.asList(message.clone(), message.clone(), message.clone()));
        listList.add(Arrays.asList(message.clone(), message.clone(), message.clone()));
        listList.add(Arrays.asList(message.clone(), message.clone(), message.clone()));

        Set<Set<MessageParent>> setSet = new HashSet<>();
        setSet.add(new HashSet<>(Arrays.asList(message.clone(), message.clone(), message.clone())));
        setSet.add(new HashSet<>(Arrays.asList(message.clone(), message.clone(), message.clone())));
        setSet.add(new HashSet<>(Arrays.asList(message.clone(), message.clone(), message.clone())));

        Map<Integer, Map<Integer, MessageParent>> mapMap = new HashMap<>();
        mapMap.put(1, Collections.singletonMap(11, message.clone()));
        mapMap.put(2, Collections.singletonMap(22, message.clone()));
        mapMap.put(3, Collections.singletonMap(33, message.clone()));
        mapMap.put(4, Collections.singletonMap(44, message.clone()));
        mapMap.put(5, Collections.singletonMap(55, message.clone()));

        List<Map<Integer, MessageParent>> mapList = new ArrayList<>();
        mapList.add(Collections.singletonMap(11, message.clone()));
        mapList.add(Collections.singletonMap(22, message.clone()));
        mapList.add(Collections.singletonMap(33, message.clone()));

        message.setInts(ints);
        message.setMessageParents(messageParents);
        message.setMessageParentList(messageParentList);
        message.setMessageParentSet(messageParentSet);
        message.setMessageParentMap(messageParentMap);
        message.setIntInts(intInts);
        message.setBeanMessageParents(beanMessageParents);
        message.setListList(listList);
        message.setSetSet(setSet);
        message.setMapMap(mapMap);
        message.setMapList(mapList);

        message.setE1(MessageEnum.E);
        message.setE2(MessageEnum.G);

        //object
        message.setO1(1);
        message.setO2("Hello Dynamic Bean");
        message.setO3(messageParents);
        message.setO4(messageParentList);
        message.setO5(messageParentSet);
        message.setO6(messageParentMap);
        message.setO7(intInts);
        message.setO8(beanMessageParents);
        message.setO9(listList);
        message.setO10(setSet);
        message.setO11(mapMap);
        message.setO12(mapList);

        //abstract
        message.setAm1(message.clone());
        message.setAm2(messageParents);
        List<AbstractMessage> am3 = Arrays.asList(message.clone(), message.clone(), message.clone());
        message.setAm3(am3);
        List<List<AbstractMessage>> am4 = new ArrayList<>();
        am4.add(Arrays.asList(message.clone(), message.clone(), message.clone()));
        am4.add(Arrays.asList(message.clone(), message.clone(), message.clone()));
        am4.add(Arrays.asList(message.clone(), message.clone(), message.clone()));
        message.setAm4(am4);
        Map<Integer, AbstractMessage> am5 = new HashMap<>();
        am5.put(1, message.clone());
        am5.put(2, message.clone());
        am5.put(3, message.clone());
        am5.put(4, message.clone());
        message.setAm5(am5);
        Map<Integer, Map<Integer, AbstractMessage>> am6 = new HashMap<>();
        am6.put(1, am5);
        am6.put(2, am5);
        am6.put(3, am5);
        message.setAm6(am6);
        List<Map<Integer, AbstractMessage>> am7 = new ArrayList<>();
        am7.add(am5);
        am7.add(am5);
        message.setAm7(am7);

        //通过-Dkin.beans.copy.deep=true开启深复制
        BeanUtils.getBeanInfo(Message.class);
        reflection(message);
        unsafe(message);
        bytebuddy(message);
        spring(message);
    }

    /**
     * copy测试核心逻辑
     *
     * @param func 包装copy逻辑
     */
    private static void copy(Message source, BiFunction<Message, Message, Void> func) {
        Stopwatch watcher = Stopwatch.createStarted();
        for (int i = 0; i < 1000; i++) {
            Message target = new Message();
            func.apply(source, target);
            if (!source.equals(target)) {
                throw new IllegalStateException("bean copy前后不一致");
            }
        }
        watcher.stop();
        long costMs = watcher.elapsed(TimeUnit.MILLISECONDS);
        System.out.printf("耗时: %dms%n", costMs);
    }

    /**
     * 基于反射
     */
    private static void reflection(Message source) {
        System.out.println("-------------------reflection-------------------");
        copy(source, (s, t) -> {
            ReflectionBeanCopy.INSTANCE.copyProperties(s, t);
            return null;
        });
    }

    /**
     * 基于unsafe
     */
    private static void unsafe(Message source) {
        System.out.println("-------------------unsafe-------------------");
        copy(source, (s, t) -> {
            UnsafeBeanCopy.INSTANCE.copyProperties(s, t);
            return null;
        });
    }

    /**
     * 基于bytebuddy
     */
    private static void bytebuddy(Message source) {
        System.out.println("-------------------bytebuddy-------------------");
        copy(source, (s, t) -> {
            ByteBuddyBeanCopy.INSTANCE.copyProperties(s, t);
            return null;
        });
    }

    /**
     * spring bean copy
     */
    private static void spring(Message source) {
        System.out.println("-------------------spring-------------------");
        copy(source, (s, t) -> {
            org.springframework.beans.BeanUtils.copyProperties(s, t);
            return null;
        });
    }
}
