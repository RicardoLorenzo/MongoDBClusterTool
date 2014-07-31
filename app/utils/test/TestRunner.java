package utils.test;

import utils.test.data.YCSBMeasure;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by ricardolorenzo on 31/07/2014.
 */
public class TestRunner {
    private static Queue<YCSBMeasure> queue;
    private YCSBTest test;

    static {
        queue = new ConcurrentLinkedQueue<>();
    }

    public TestRunner(YCSBTest test) {
        this.test = test;
    }

    public void runTest(String jumpAddress, List<String> testNodeAddresses) {
        List<Thread> threads = new ArrayList<>();
        for(String testNodeAddress : testNodeAddresses) {
            Thread t = new Thread(new TestNodeRunner(queue, jumpAddress, testNodeAddress, test));
            threads.add(t);
            t.start();
        }
    }
}
