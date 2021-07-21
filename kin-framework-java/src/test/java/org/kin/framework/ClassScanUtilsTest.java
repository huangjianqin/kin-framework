package org.kin.framework;

import org.kin.framework.proxy.ProxyInvoker;
import org.kin.framework.utils.ClassScanUtils;
import org.kin.framework.utils.SPI;

/**
 * @author huangjianqin
 * @date 2021/7/20
 */
public class ClassScanUtilsTest {
    public static void main(String[] args) {
        System.out.println(ClassScanUtils.scan("org.kin", ProxyInvoker.class));
        System.out.println(ClassScanUtils.scan("org.kin", SPI.class));
    }
}
