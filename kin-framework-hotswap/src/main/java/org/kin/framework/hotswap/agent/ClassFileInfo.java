package org.kin.framework.hotswap.agent;

import org.kin.framework.utils.ExceptionUtils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author huangjianqin
 * @date 2019/3/1
 */
public class ClassFileInfo {
    private final String filePath;
    private final String className;
    private final long lastModifyTime;
    private final String md5;

    public ClassFileInfo(String filePath, String className, byte[] bytes, long lastModifyTime) {
        this.filePath = filePath;
        this.className = className;
        this.lastModifyTime = lastModifyTime;
        this.md5 = this.md5(bytes);
    }

    private String md5(byte[] bytes) {
        try {
            MessageDigest me = MessageDigest.getInstance("MD5");
            me.update(bytes);
            BigInteger bi = new BigInteger(1, me.digest());
            return bi.toString(16).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            ExceptionUtils.throwExt(e);
        }

        throw new IllegalStateException("encounter unknown error");
    }

    //getter
    public String getFilePath() {
        return filePath;
    }

    public String getClassName() {
        return className;
    }

    public long getLastModifyTime() {
        return lastModifyTime;
    }

    public String getMd5() {
        return md5;
    }

    @Override
    public String toString() {
        return "ClassFileInfo{" +
                "filePath='" + filePath + '\'' +
                ", className='" + className + '\'' +
                ", lastModifyTime=" + lastModifyTime +
                ", md5='" + md5 + '\'' +
                '}';
    }
}
