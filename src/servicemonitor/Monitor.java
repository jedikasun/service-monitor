package servicemonitor;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Aim of this class is to decouple the implementation of the actual delayed execution mechanism (the scheduled thread pool), from the Service class.
 * The Service does not have to implement Runnable, nor be aware of a thread pool.
 */
class Monitor {

    private ScheduledThreadPoolExecutor pool;

    private static class Holder {
        private static final Monitor INSTANCE = new Monitor();
    }

    static Monitor instance() {
        return Holder.INSTANCE;
    }

    private Monitor() {
        pool = new ScheduledThreadPoolExecutor(Integer.MAX_VALUE);
        pool.setKeepAliveTime(1, TimeUnit.MINUTES);
        pool.allowCoreThreadTimeOut(true);
    }

    void scheduleAfterDelay(Service service, long delayInMs) {
        pool.schedule(service::poll, delayInMs, TimeUnit.MILLISECONDS);
    }
}
