package org.kin.framework.proxy;

/**
 * javassist与byte buddy性能接近, 但javassist make class会大量消耗metaspace,
 * 尤其是CompressedClassSpaceSize, 当使用javassist创建大量class时, 需调大-XX:CompressedClassSpaceSize(最大3g)
 * 还有javassist ClassPool.get()方法时, 加载ClassPath已有的类, 会读取对应的class文件, 如果在生产环境存在, class文件加密的话, 会出现异常
 *
 * @author huangjianqin
 * @date 2020/12/22
 */
public class Proxys {
    private Proxys() {
    }

    /**
     * @return javassist 工厂类
     */
    public static ProxyFactory javassist() {
        return JavassistFactory.INSTANCE;
    }

    /**
     * @return byteBuddy 工厂类
     */
    public static ProxyFactory byteBuddy() {
        return ByteBuddyFactory.INSTANCE;
    }

    /**
     * @return jdk 工厂类
     */
    public static ProxyFactory reflection() {
        return ReflectionProxyFactory.INSTANCE;
    }

}
