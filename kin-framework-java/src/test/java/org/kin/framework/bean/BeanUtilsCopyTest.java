package org.kin.framework.bean;

import org.kin.framework.beans.BeanUtils;

/**
 * @author huangjianqin
 * @date 2021/9/10
 */
public class BeanUtilsCopyTest {
    public static void main(String[] args) {
        Source source = new Source();
        Source target = new Source();
        BeanUtils.copyProperties(source, target);
        System.out.println(source.equals(target));
        System.out.println(source);
        System.out.println(target);
    }
}
