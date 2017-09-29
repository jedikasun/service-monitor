package servicemonitor;


import java.util.HashMap;
import java.util.Map;

public class ServiceRegistry {


    private final Map<String, Service> services = new HashMap<>();

    private ServiceRegistry() {}

    /*
     * Singleton implementation
     */
    private static class Holder {
        private static final ServiceRegistry INSTANCE = new ServiceRegistry();
    }

    /*
     * Returns thread safe singleton
     */
    public static ServiceRegistry instance() {
        return Holder.INSTANCE;
    }

    public void addService(String host, int port, int pollingInterval, int gracePeriod) throws ServiceDefinitionException {
        synchronized (services) {
            String key = serviceKey(host, port); //construct key with which to identify the service in the map
            if (!services.containsKey(key)) {
                services.put(key, new Service(host, port, pollingInterval, gracePeriod)); //service is not present in map, therefore adding it
            }
            else {
                throw new ServiceDefinitionException("Attempting to add existing service");
            }
        }
    }

    public void removeService(String host, int port) throws ServiceDefinitionException {
        Service serv;
        synchronized (services) {
            serv = services.remove(serviceKey(host, port));
            if (serv == null) {
                throw new ServiceDefinitionException("Attempting to remove non-existing service");
            }
        }
        serv.invalidate(); //call invalidate outside of synchronized block
    }

    public void setPollingInterval(String host, int port, int pollingInterval) throws ServiceDefinitionException {
        synchronized (services) {
            Service serv = services.get(serviceKey(host, port));
            if (serv != null) {
                serv.setPollingInterval(pollingInterval);
            }
            else {
                throw new ServiceDefinitionException("Service definition not found");
            }
        }
    }

    public void setGracePeriod(String host, int port, int gracePeriod) throws ServiceDefinitionException {
        synchronized (services) {
            Service serv = services.get(serviceKey(host, port));
            if (serv != null) {
                serv.setGracePeriod(gracePeriod);
            }
            else {
                throw new ServiceDefinitionException("Service definition not found");
            }
        }
    }


    public void setPlannedOutage(String host, int port, long start, long end) throws ServiceDefinitionException {
        synchronized (services) {
            Service serv = services.get(serviceKey(host, port));
            if (serv != null) {
                serv.setServiceOutage(start, end);
            }
            else {
                throw new ServiceDefinitionException("Service definition not found");
            }
        }
    }

    public void register(String host, int port, ServiceObserver observer) throws ServiceDefinitionException {
        Service serv;
        synchronized (services) {
            serv = services.get(serviceKey(host, port));
            if (serv == null) {
                throw new ServiceDefinitionException("Service definition not found");
            }
        }
        serv.register(observer);
    }

    public void unregister(String host, int port, ServiceObserver observer) throws ServiceDefinitionException {
        Service serv;
        synchronized (services) {
            serv = services.get(serviceKey(host, port));
            if (serv == null) {
                throw new ServiceDefinitionException("Service definition not found");
            }
        }
        serv.unregister(observer);
    }

    private static String serviceKey(String host, int port) {
        return host + "*" + port;
    }

}
