package utils.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.ConfigurationService;
import utils.ssh.SSHClient;
import utils.ssh.SSHException;
import utils.test.data.Measure;

import java.io.IOException;
import java.util.Queue;

/**
 * Created by ricardolorenzo on 31/07/2014.
 */
public abstract class TestNodeRunner implements Runnable {
    private static Logger log = LoggerFactory.getLogger(TestNodeRunner.class);
    protected Queue<Measure> measurementQueue;
    private final String jumpAddress;
    private final String nodeAddress;
    private final String systemCommand;

    public TestNodeRunner(Queue<Measure> measurementQueue, String jumpAddress, String nodeAddress, Test test) {
        this.measurementQueue = measurementQueue;
        this.jumpAddress = jumpAddress;
        this.nodeAddress = nodeAddress;
        this.systemCommand = test.getSystemCommand();
        System.out.println("C: " + this.systemCommand);
    }

    protected String getNodeAddress() {
        return nodeAddress;
    }

    @Override
    public void run() {
        try {
            SSHClient client = new SSHClient(jumpAddress, 22);
            client.connect(ConfigurationService.TEST_USER);
            try {
                client.forwardConnect(nodeAddress, ConfigurationService.TEST_USER, 22);
                client.sendForwardPipedCommand(nodeAddress, systemCommand);
                for(String line = client.readForwardPipedCommandOutputLine(nodeAddress); line != null;
                    line = client.readForwardPipedCommandOutputLine(nodeAddress)) {
                    Measure m = getMeasure(line);
                    if(m != null) {
                        measurementQueue.add(m);
                    }
                }
                client.terminateForwardPipedCommand(nodeAddress);
                log.info("[" + Thread.currentThread().getName() + "] - process finished");
                System.out.println("INFO: [" + Thread.currentThread().getName() + "] - process finished");
            } catch(SSHException e) {
                log.error("[" + Thread.currentThread().getName() + "] - " + nodeAddress + " connection: " +
                        e.getMessage());
            } catch(IOException e) {
                log.error("[" + Thread.currentThread().getName() + "] - " + nodeAddress + " input/output: " +
                        e.getMessage());
            } catch(Throwable e) {
                log.error("[" + Thread.currentThread().getName() + "] - " + nodeAddress + "\n" +
                        e.toString());
            } finally {
                client.terminateForwardPipedCommand(nodeAddress);
                client.disconnect();
            }
        } catch(IOException e) {
            log.error("[" + Thread.currentThread().getName() + "] - " + e.getMessage());
        } catch(SSHException e) {
            log.error("[" + Thread.currentThread().getName() + "] - cannot connect to the jump server: " + jumpAddress);
        }
    }

    protected abstract Measure getMeasure(String line);
}
