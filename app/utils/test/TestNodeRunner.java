package utils.test;

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
    protected Queue<Measure> measurementQueue;
    private final String jumpAddress;
    private final String nodeAddress;
    private final String systemCommand;

    public TestNodeRunner(Queue<Measure> measurementQueue, String jumpAddress, String nodeAddress, Test test) {
        this.measurementQueue = measurementQueue;
        this.jumpAddress = jumpAddress;
        this.nodeAddress = nodeAddress;
        this.systemCommand = test.getSystemCommand();
    }

    protected String getNodeAddress() {
        return nodeAddress;
    }

    @Override
    public void run() {
        try {
            SSHClient client = new SSHClient(jumpAddress, 22);
            client.connect(ConfigurationService.TEST_USER);
            client.forwardConnect(nodeAddress, ConfigurationService.TEST_USER, 22);
            client.sendPipedCommand(nodeAddress, systemCommand);
            for(String line = client.readForwardPipedCommandOutputLine(nodeAddress);
                line != null && client.forwardPipedCommandOutputAvailable();
                line = client.readForwardPipedCommandOutputLine(nodeAddress)) {
                measurementQueue.add(getMeasure(line));
            }
            client.terminateForwardPipedCommand(nodeAddress);
        } catch(SSHException e) {
            // TODO do something here
        } catch(IOException e) {
            // TODO do something here
        }
    }

    protected abstract Measure getMeasure(String line);
}
