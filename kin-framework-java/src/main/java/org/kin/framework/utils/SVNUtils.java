package org.kin.framework.utils;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import java.io.File;

/**
 * @author huangjianqin
 * @date 2020-02-20
 */
public class SVNUtils {
    static {
        DAVRepositoryFactory.setup();
        SVNRepositoryFactoryImpl.setup();
        FSRepositoryFactory.setup();
    }

    /**
     * checkout svn repository
     */
    public static boolean checkoutRepository(String remote, String user, String password, String targetDir) {
        File targetDirFile = new File(targetDir);
        if (!targetDirFile.exists()) {
            targetDirFile.mkdir();
        }

        try {
            SVNURL svnUrl = SVNURL.parseURIEncoded(remote);
            DefaultSVNOptions options = SVNWCUtil.createDefaultOptions(true);
            SVNClientManager svnClientManager = SVNClientManager.newInstance(
                    options, user, password);
            SVNUpdateClient updateClient = svnClientManager.getUpdateClient();
            updateClient.setIgnoreExternals(false);

            updateClient.doCheckout(svnUrl, targetDirFile, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, true);
            return true;
        } catch (SVNException e) {

        }

        return false;
    }
}
