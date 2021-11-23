package org.kin.framework.utils;

/**
 * @author huangjianqin
 * @date 2021/5/28
 */
public class ExtensionLoaderTest {
    public static void main(String[] args) {
        ExtensionLoader loader = ExtensionLoader.load();
        System.out.println(loader.getExtensions(KinService.class));
        System.out.println(loader.getExtension(KinService.class, (byte) 1));
        System.out.println(loader.getExtension(KinService.class, "E"));
        System.out.println(loader.getExtensionOrDefault(KinService.class, "E"));
    }
}
