package org.kin.framework.utils;

import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * @author huangjianqin
 * @date 2020-05-30
 */
public class GitUtilsTest {
    public static void main(String[] args) {
        try {
            GitUtils.cloneRepository("https://github.com/huangjianqin/kin-java-agent.git",
                    "", "", "test");
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
    }
}
