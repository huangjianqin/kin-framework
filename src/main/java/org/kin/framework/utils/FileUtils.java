package org.kin.framework.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-02-22
 */
public class FileUtils {
    /**
     * 循环递归删除文件
     */
    public static boolean delete(String targetPath) {
        File targetFile = new File(targetPath);
        return delete(targetFile);
    }

    /**
     * 循环递归删除文件
     */
    public static boolean delete(File targetFile) {
        if (Objects.nonNull(targetFile)) {
            if (targetFile.isDirectory()) {
                File[] childFiles = targetFile.listFiles();
                if (CollectionUtils.isNonEmpty(childFiles)) {
                    for (File childFile : childFiles) {
                        delete(childFile);
                    }
                    return targetFile.delete();
                }
            } else {
                return targetFile.delete();
            }
        }
        return false;
    }

    /**
     * 创建包含指定内容的文件
     */
    public static boolean createFile(String fileName, String content) throws IOException {
        File file = new File(fileName);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }
        try (FileOutputStream fileOutputStream = new FileOutputStream(fileName)) {
            fileOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
            fileOutputStream.close();
            return true;
        }
    }
}
