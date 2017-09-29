package servicemonitor;


import junit.framework.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.SynchronousQueue;

public class ServiceRegistryTest {

    @BeforeTest
    public void setup() throws ServiceDefinitionException {
        //the first two will be valid for the remainder of this test class
        ServiceRegistry.instance().addService("127.0.0.1", 10100, 1000 * 30, 10 * 1000);
        ServiceRegistry.instance().addService("172.17.10.8", 10200, 1000 * 30, 10 * 1000);

        //services configured beyond this point will be invalidated during the test class
        ServiceRegistry.instance().addService("172.17.10.8", 10300, 1000 * 30, 10 * 1000);
    }

    // Adding an already added service. This should throw a ServiceDefinitionException.
    @Test(expectedExceptions = ServiceDefinitionException.class)
    public void addSameServiceAgain() throws ServiceDefinitionException {
        ServiceRegistry.instance().addService("127.0.0.1", 10100, 1000 * 30, 10 * 1000);
    }

    @Test
    public void removeService() throws ServiceDefinitionException {
        ServiceRegistry.instance().removeService("172.17.10.8", 10300);
    }

    //Remove a service that doesn't exist in the registry. This should throw a ServiceDefinitionException.
    @Test(expectedExceptions = ServiceDefinitionException.class)
    public void removeInvalidService() throws ServiceDefinitionException {
        ServiceRegistry.instance().removeService("192.17.100.100", 10300);
    }

    @Test
    public void setPollInterval() throws ServiceDefinitionException {
        ServiceRegistry.instance().setPollingInterval("127.0.0.1", 10100, 40 * 1000);
    }

    @Test(expectedExceptions = ServiceDefinitionException.class)
    public void setPollIntervalForInvalidService() throws ServiceDefinitionException {
        ServiceRegistry.instance().setPollingInterval("192.17.100.12", 10100, 40 * 1000);
    }

    @Test
    public void setGracePeriod() throws ServiceDefinitionException {
        ServiceRegistry.instance().setGracePeriod("127.0.0.1", 10100, 10 * 1000);
    }

    @Test(expectedExceptions = ServiceDefinitionException.class)
    public void setGracePeriodForInvalidService() throws ServiceDefinitionException {
        ServiceRegistry.instance().setPollingInterval("192.17.100.12", 10100, 10 * 1000);
    }

    @Test
    public void register() throws ServiceDefinitionException, InterruptedException, IOException {
        int expectedDelay = 10 * 1000;
        ServiceRegistry.instance().setPollingInterval("127.0.0.1", 10100, expectedDelay);
        SynchronousQueue lockQ = new SynchronousQueue();
        ServiceRegistry.instance().register("127.0.0.1", 10100, (oldStatus, newStatus) -> {
            System.out.println("127.0.0.1:10100 changed from " + oldStatus + " to " + newStatus);
            try {
                lockQ.put(""); //notifying the wait below
            } catch (InterruptedException e) {
            }
        });

        //initial call back should be within the expected delay
        long startWait = System.currentTimeMillis();
        lockQ.take(); //wait till the lambda is called back
        long actualWait = System.currentTimeMillis() - startWait;
        System.out.println("Actual wait = " + actualWait + ", expected delay = " + expectedDelay);
        Assert.assertTrue(Math.abs(actualWait - expectedDelay) < 2000);

        //bring the service up
        ServerSocket ssoc = new ServerSocket(10100);
        int grace = 5 * 1000;
        ServiceRegistry.instance().setGracePeriod("127.0.0.1", 10100, grace);
        ssoc.accept();
        lockQ.take(); //wait till the lambda is called back

        //service is down again now..
        ssoc.close();

        startWait = System.currentTimeMillis();
        lockQ.take(); //wait till the lambda is called back
        actualWait = System.currentTimeMillis() - startWait;
        expectedDelay = expectedDelay + grace; //10 + grace of 5
        System.out.println("Actual wait = " + actualWait + ", expected delay = " + expectedDelay);
        Assert.assertTrue(Math.abs(actualWait - expectedDelay) < 3000);
    }

}
