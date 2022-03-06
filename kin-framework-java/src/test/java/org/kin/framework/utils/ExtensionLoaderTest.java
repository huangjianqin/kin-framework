package org.kin.framework.utils;

/**
 * @author huangjianqin
 * @date 2021/5/28
 */
public class ExtensionLoaderTest {
    public static void main(String[] args) {
        System.out.println(ExtensionLoader.getExtensions(KinService.class));
        System.out.println(ExtensionLoader.getExtension(KinService.class, (byte) 1));
        System.out.println(ExtensionLoader.getExtension(KinService.class, "E"));
        System.out.println(ExtensionLoader.getExtensionOrDefault(KinService.class, "E"));
    }
}
