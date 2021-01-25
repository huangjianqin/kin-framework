package org.kin.framework.concurrent;

import org.kin.framework.collection.ConcurrentHashSet;

import java.util.Set;

/**
 * @author huangjianqin
 * @date 2020-04-16
 */
public class DispatcherTest {
    private static Set<String> names = new ConcurrentHashSet<>();
    private static int counter;

    public static void main(String[] args) throws InterruptedException {
        ExecutionContext executionContext = ExecutionContext.cache("dispatcher-test");

        Dispatcher<Integer, IntMessage> eventBaseDispatcher = new EventBasedDispatcher<>(5);
        int key = 1;
        eventBaseDispatcher.register(key, new TestReceiver(), false);

        int num = 3;
        for (int i = 0; i < num; i++) {
            executionContext.execute(new TestRunnable(eventBaseDispatcher));
        }

        Thread.sleep(10000);

        System.out.println(names);
        System.out.println(counter);

        eventBaseDispatcher.close();
        executionContext.shutdown();
    }

    static class TestReceiver extends Receiver<IntMessage> {
        @Override
        public void receive(IntMessage mail) {
            names.add(Thread.currentThread().getName());
            counter++;
        }

        @Override
        protected void onStart() {
            super.onStart();
            System.out.println("recevier start");
        }

        @Override
        protected void onStop() {
            super.onStop();
            System.out.println("recevier stop");
        }
    }

    static class IntMessage extends InBox.InBoxMessage {
        private int i;

        public IntMessage(int i) {
            this.i = i;
        }
    }


    static class TestRunnable implements Runnable {
        private Dispatcher<Integer, IntMessage> eventBaseDispatcher;

        public TestRunnable(Dispatcher<Integer, IntMessage> eventBaseDispatcher) {
            this.eventBaseDispatcher = eventBaseDispatcher;
        }

        @Override
        public void run() {
            int key = 1;
            for (int i = 0; i < 10000; i++) {
                eventBaseDispatcher.postMessage(key, new IntMessage(i));
            }
        }
    }
}
