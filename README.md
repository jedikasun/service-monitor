# service-monitor
Monitors the state of a "service" and notifies observers of state changes

The ServiceRegistry class contains the public API for this project.
Expected usage:
  1) call the ServiceRegistry.addService() to "create" services
  2) if necessary, set the polling interval/grace period/outage of the service(s) using the methods in the ServiceRegistry
  3) register to listen for service status changes by passing in an implementation of the ServiceObserver to the ServiceRegistry.register() method.
  

Todo:
  1) refactor the setting of the service outages to allow caller to set multiple outages to a service.
  2) complete the unit testing suite 
  3) complete the documentation
  
