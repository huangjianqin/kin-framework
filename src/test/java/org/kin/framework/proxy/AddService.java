package org.kin.framework.proxy;

import java.util.ArrayList;
import java.util.List;

/**
 * @author huangjianqin
 * @date 2020-01-16
 */
public class AddService {
    public int add(int a, int b){
        return a + b;
    }

    public <T> T get(T t){
        return t;
    }

    public <T> List<T> addItem(T item){
        List<T> list = new ArrayList<>();
        list.add(item);
        return list;
    }

}
