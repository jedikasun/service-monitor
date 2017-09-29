package servicemonitor;


/**
 * Observer interface to be implemented by any party that wishes to listen to service state changes.
 */
public interface ServiceObserver {

    void notifyServiceStateChange(ServiceStatus oldStatus, ServiceStatus newStatus);

}
