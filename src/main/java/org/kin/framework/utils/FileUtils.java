package org.kin.framework.utils;

import java.io.File;

/**
 * @author huangjianqin
 * @date 2020-02-22
 */
public class FileUtils {
    public static boolean delete(String targetPath) {
        File targetFile = new File(targetPath);
        return delete(targetFile);
    }

    public static boolean delete(File targetFile) {
        if (targetFile.isDirectory()) {
            for (File childFile : targetFile.listFiles()) {
                delete(childFile);
            }
            return targetFile.delete();
        } else {
            return targetFile.delete();
        }
    }
}
