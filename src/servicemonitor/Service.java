package servicemonitor;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstraction of a service, as defined in the requirement (see package-info for details).
 */
class Service {

    private final String host;
    private final int port;

    private ServiceStatus currentStatus;
    private long gracePeriod_start;
    private volatile int pollingInterval, gracePeriod;
    private volatile long serviceOutageStart, serviceOutageEnd;
    private volatile boolean invalidated, scheduled; //invalidated is set to true when this Service is no longer registered with the registry. Scheduled is set to true only when there are observers.

    private final List<ServiceObserver> observers = new ArrayList<>();

    Service(String host, int port, int pollingInterval, int gracePeriod) {
        this.host = host;
        this.port = port;
        this.pollingInterval = pollingInterval;
        this.gracePeriod = gracePeriod;
        currentStatus = ServiceStatus.UNKNOWN;
    }

    void setPollingInterval(int pollingInterval) {
        this.pollingInterval = pollingInterval;
    }

    void setGracePeriod(int gracePeriod) {
        this.gracePeriod = gracePeriod;
    }

    /**
     * Marks this service as not monitored any more.
     */
    void invalidate() {
        invalidated = true;
    }

    void register(ServiceObserver observer) {
        if (invalidated) return;
        boolean alreadyScheduled; //copy of the 'scheduled' variable, value is set within the synchronized block
        synchronized (observers) {
            observers.add(observer);

            alreadyScheduled = scheduled;
            if (!scheduled) scheduled = true;
        }
        if (!alreadyScheduled) {
            Monitor.instance().scheduleAfterDelay(this, pollingInterval); //schedule first run;
        }
    }

    void unregister(ServiceObserver observer) {
        if (invalidated) return;
        synchronized (observers) {
            observers.remove(observer);

            if (observers.isEmpty()) scheduled = false;
        }
    }

    void setServiceOutage(long start, long end) {
        serviceOutageStart = start;
        serviceOutageEnd = end;
    }

    private long getServiceOutageEnd() {
        return serviceOutageEnd;
    }

    private boolean isInPlannedOutage() {
        long currentTime = System.currentTimeMillis();
        return currentTime >= serviceOutageStart && currentTime < serviceOutageEnd;
    }

    void poll() {
        /*
        IF in outage THEN
            set status to in-outage
        ELSE
            check service status
            IF service is down THEN
                IF status is up AND has more grace period remaining THEN
                    schedule for next run (grace period remainder)
                    EXIT
                END IF
            END IF
        END IF
        clear flags

        IF status has changed THEN
            notify observers
        END IF
        schedule for next run (after interval - considering outage)

         */
        if (invalidated || !scheduled) return;

        ServiceStatus newStatus;

        if (isInPlannedOutage()) {
            newStatus = ServiceStatus.PLANNED_OUT;
        } else {
            newStatus = connectToService();
            if (newStatus == ServiceStatus.DOWN && currentStatus == ServiceStatus.UP) {
                if (!gracePeriodStarted()) startGracePeriod();

                if (inGracePeriod()) {
                    Monitor.instance().scheduleAfterDelay(this, getRemainingGracePeriod()); //schedule next run
                    return;
                }
            }
        }

        endGracePeriod();

        if (newStatus != currentStatus) {
            ServiceStatus oldStatus = currentStatus;
            currentStatus = newStatus;
            notifyObservers(oldStatus, newStatus);
        }

        Monitor.instance().scheduleAfterDelay(this, newStatus == ServiceStatus.PLANNED_OUT ? (getServiceOutageEnd() - System.currentTimeMillis()) : pollingInterval); //schedule next run
    }

    private boolean inGracePeriod() {
        return getRemainingGracePeriod() > 0;
    }

    private long getRemainingGracePeriod() {
        return gracePeriod - (System.currentTimeMillis() - gracePeriod_start);
    }

    private boolean gracePeriodStarted() {
        return gracePeriod_start != 0;
    }

    private void endGracePeriod() {
        gracePeriod_start = 0;
    }

    private void startGracePeriod() {
        if (gracePeriod_start == 0) gracePeriod_start = System.currentTimeMillis();
    }

    private void notifyObservers(ServiceStatus oldStatus, ServiceStatus newStatus) {
        List<ServiceObserver> detailsCopy = new ArrayList<>();
        synchronized (observers) {
            detailsCopy.addAll(observers);
        }
        for (ServiceObserver obs : detailsCopy) {
            obs.notifyServiceStateChange(oldStatus, newStatus);
        }
    }

    private ServiceStatus connectToService() {
        try {
            Socket soc = new Socket(host, port);
            return soc.isConnected() ? ServiceStatus.UP : ServiceStatus.DOWN;
        } catch (IOException e) {
            return ServiceStatus.DOWN;
        }
    }

}
