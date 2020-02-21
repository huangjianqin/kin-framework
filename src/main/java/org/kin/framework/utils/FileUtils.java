package org.kin.framework.utils;

import java.io.File;

/**
 * @author huangjianqin
 * @date 2020-02-22
 */
public class FileUtils {
    public static boolean deleteDir(String dir) {
        File dirFile = new File(dir);
        if (dirFile.isDirectory()) {
            return deleteDir(dirFile);
        }

        return false;
    }

    public static boolean deleteDir(File dirFile) {
        for (File childFile : dirFile.listFiles()) {
            boolean result;
            if (childFile.isDirectory()) {
                result = deleteDir(childFile);
            } else {
                result = childFile.delete();
            }
            if (!result) {
                return result;
            }
        }
        dirFile.delete();

        return true;
    }
}
