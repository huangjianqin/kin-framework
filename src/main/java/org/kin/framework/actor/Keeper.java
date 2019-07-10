package org.kin.framework.actor;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.concurrent.ThreadManager;
import org.kin.framework.utils.ExceptionUtils;

import java.util.Set;
import java.util.concurrent.*;

/**
 * @author huangjianqin
 * @date 2019/7/10
 */
public class Keeper {
    private static final ThreadManager THREADS = new ThreadManager(
            new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(),
                    new SimpleThreadFactory("keeper")));
    private static final Set<RunnableKeeperAction> RUNNABLE_KEEPER_ACTIONS = new CopyOnWriteArraySet<>();
    static {
        JvmCloseCleaner.DEFAULT().add(() -> {
            THREADS.shutdown();
            for(RunnableKeeperAction runnableKeeperAction: RUNNABLE_KEEPER_ACTIONS){
                runnableKeeperAction.stop();
            }
            RUNNABLE_KEEPER_ACTIONS.clear();
        });
    }

    private static class RunnableKeeperAction implements Runnable {
        private volatile boolean isStopped;
        private KeeperAction target;

        public RunnableKeeperAction(KeeperAction target) {
            this.target = target;
        }

        public void stop(){
            isStopped = true;
        }

        @Override
        public void run() {
            target.preAction();
            try{
                while(!isStopped){
                    try {
                        target.action();
                    } catch (Exception e) {
                        ExceptionUtils.log(e);
                    }
                }
            }finally {
                target.postAction();
            }
        }
    }

    //api
    public static void keep(KeeperAction keeperAction){
        RunnableKeeperAction runnableKeeperAction = new RunnableKeeperAction(keeperAction);
        THREADS.execute(runnableKeeperAction);
        RUNNABLE_KEEPER_ACTIONS.add(runnableKeeperAction);
    }
}
