package org.kin.framework.utils;

/**
 * @author huangjianqin
 * @date 2021/5/28
 */
public class KinServiceLoaderTest {
    public static void main(String[] args) {
        KinServiceLoader loader = KinServiceLoader.load();
        System.out.println(loader.getExtensions(KinService.class));
    }
}
