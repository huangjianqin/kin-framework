package org.kin.framework.utils;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

/**
 * @author huangjianqin
 * @date 2020-02-20
 */
public class GitUtils {
    /**
     * clone git repository latest branch'
     */
    public static boolean cloneRepository(String remote, String user, String password, String targetDir) throws GitAPIException {
        return cloneRepository(remote, user, password, targetDir, Collections.emptyList());
    }

    /**
     * clone git repository
     */
    public static boolean cloneRepository(String remote, String user, String password, String targetDir, String... branches) throws GitAPIException {
        return cloneRepository(remote, user, password, targetDir, CollectionUtils.toList(branches));
    }

    /**
     * clone git repository
     */
    public static boolean cloneRepository(String remote, String user, String password, String targetDir, Collection<String> branches) throws GitAPIException {
        File targetDirFile = new File(targetDir);
        if (!targetDirFile.exists()) {
            targetDirFile.mkdir();
        }

        CloneCommand clone = Git.cloneRepository().setURI(remote).setDirectory(targetDirFile);
        if (CollectionUtils.isNonEmpty(branches)) {
            clone.setBranchesToClone(branches);
        }

        if (remote.contains("http") || remote.contains("https")) {
            UsernamePasswordCredentialsProvider provider = new UsernamePasswordCredentialsProvider(user, password);
            clone.setCredentialsProvider(provider);
        }

        Git clonedRepository;
        clonedRepository = clone.call();
        clonedRepository.close();
        return true;
    }
}
