package org.kin.framework.concurrent;

import org.kin.framework.JvmCloseCleaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author huangjianqin
 * @date 2019/7/10
 */
public class Keeper {
    private static final Logger log = LoggerFactory.getLogger(Keeper.class);
    private static final ExecutionContext EXECUTION_CONTEXT = ExecutionContext.cache("keeper");
    private static final Set<RunnableKeeperAction> RUNNABLE_KEEPER_ACTIONS = new CopyOnWriteArraySet<>();

    static {
        JvmCloseCleaner.instance().add(() -> {
            EXECUTION_CONTEXT.shutdown();
            for (RunnableKeeperAction runnableKeeperAction : RUNNABLE_KEEPER_ACTIONS) {
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

        public void stop() {
            isStopped = true;
        }

        public void stopInterruptly() {
            stop();
            bindThread.interrupt();
        }

        @Override
        public void run() {
            bindThread = Thread.currentThread();
            target.preAction();
            try {
                while (!isStopped && !Thread.currentThread().isInterrupted()) {
                    try {
                        target.action();
                    } catch (Exception e) {
                        log.error("", e);
                    }
                }
            } finally {
                target.postAction();
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            RunnableKeeperAction that = (RunnableKeeperAction) o;
            return target.equals(that.target);
        }

        @Override
        public int hashCode() {
            return Objects.hash(target);
        }
    }

    @FunctionalInterface
    public interface KeeperStopper {
        /**
         * 停止KeeperAction
         */
        void stop();
    }

    //--------------------------------------------api-----------------------------------------------------------

    public static KeeperStopper keep(KeeperAction keeperAction) {
        RunnableKeeperAction runnableKeeperAction = new RunnableKeeperAction(keeperAction);
        EXECUTION_CONTEXT.execute(runnableKeeperAction);
        RUNNABLE_KEEPER_ACTIONS.add(runnableKeeperAction);

        return runnableKeeperAction::stop;
    }

    public static KeeperStopper keep(Runnable runnable) {
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
        EXECUTION_CONTEXT.execute(runnableKeeperAction);
        RUNNABLE_KEEPER_ACTIONS.add(runnableKeeperAction);

        return runnableKeeperAction::stop;
    }
}
