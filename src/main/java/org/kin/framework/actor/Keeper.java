package org.kin.framework.actor;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.concurrent.ThreadManager;
import org.kin.framework.utils.ExceptionUtils;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
        private Thread bindThread;

        public RunnableKeeperAction(KeeperAction target) {
            this.target = target;
        }

        public void stop(){
            isStopped = true;
        }

        public void stopInterruptly(){
            stop();
            bindThread.interrupt();
        }

        @Override
        public void run() {
            bindThread = Thread.currentThread();
            target.preAction();
            try{
                while(!isStopped && !Thread.currentThread().isInterrupted()){
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

    @FunctionalInterface
    public interface KeeperStopper {
        void stopKeeper();
    }

    //api
    public static KeeperStopper keep(KeeperAction keeperAction){
        RunnableKeeperAction runnableKeeperAction = new RunnableKeeperAction(keeperAction);
        THREADS.execute(runnableKeeperAction);
        RUNNABLE_KEEPER_ACTIONS.add(runnableKeeperAction);

        return () -> runnableKeeperAction.stop();
    }

    public static KeeperStopper keep(Runnable runnable){
        RunnableKeeperAction runnableKeeperAction = new RunnableKeeperAction(new KeeperAction() {
            @Override
            public void preAction() {

            }

            @Override
            public void action() {
                runnable.run();
            }

            @Override
            public void postAction() {

            }
        });
        THREADS.execute(runnableKeeperAction);
        RUNNABLE_KEEPER_ACTIONS.add(runnableKeeperAction);

        return () -> runnableKeeperAction.stop();
    }
}
