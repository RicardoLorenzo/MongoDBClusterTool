package utils.test;

import services.ConfigurationService;
import utils.ssh.SSHClient;
import utils.ssh.SSHException;
import utils.test.data.YCSBMeasure;

import java.io.IOException;
import java.util.Queue;

/**
 * Created by ricardolorenzo on 31/07/2014.
 */
public class TestNodeRunner implements Runnable {
    private final Queue<YCSBMeasure> measurementQueue;
    private String jumpAddress;
    private String nodeAddress;
    private String systemCommand;

    public TestNodeRunner(Queue<YCSBMeasure> measurementQueue, String jumpAddress, String nodeAddress, YCSBTest test) {
        this.measurementQueue = measurementQueue;
        this.jumpAddress = jumpAddress;
        this.nodeAddress = nodeAddress;
        this.systemCommand = test.getSystemCommand();
    }

    @Override
    public void run() {
        try {
            SSHClient client = new SSHClient(jumpAddress, 22);
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

    private static YCSBMeasure getMeasure(String line) {
        /**
         * TODO process the line
         */
        YCSBMeasure m = new YCSBMeasure();
        return m;
    }
}
