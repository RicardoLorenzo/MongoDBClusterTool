package utils.test;

import utils.test.data.Measure;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by ricardolorenzo on 31/07/2014.
 */
public abstract class TestRunner {
    private static Queue<Measure> queue = new ConcurrentLinkedQueue<>();
    private static Map<String, Thread> testThreads = new HashMap<>();
    private static Map<String, Object> attributeObjects = new HashMap<>();

    protected abstract Test getTest();

    protected abstract void finalizeTasks();

    protected static Object getAttributeObject(String name) {
        return attributeObjects.get(name);
    }

    public static Measure getMeasureFromQueue() {
        return queue.poll();
    }

    public static Map<String, Boolean> getTestNodesStatuses() {
        Map<String, Boolean> threadStatuses = new TreeMap<>(Comparator.<String>naturalOrder());
        for(Map.Entry<String, Thread> e : testThreads.entrySet()) {
            threadStatuses.put(e.getKey(), e.getValue().isAlive());
        }
        return threadStatuses;
    }

    protected static boolean hasAttributeObject(String name) {
        return attributeObjects.containsKey(name);
    }

    protected abstract void initializeTasks(String jumpAddress, List<String> nodeAddresses) throws TestException;

    protected abstract void preRunTask(String jumpAddress, String testNodeAddress) throws TestException;

    public void runTest(String jumpAddress, List<String> testNodeAddresses) throws TestException {
        try {
            initializeTasks(jumpAddress, testNodeAddresses);
            for(String testNodeAddress : testNodeAddresses) {
                try {
                    preRunTask(jumpAddress, testNodeAddress);
                    TestNodeRunner runner = new YCSBTestNodeRunner(queue, jumpAddress, testNodeAddress, getTest());
                    Thread t = new Thread(runner);
                    testThreads.put(testNodeAddress, t);
                    t.start();
                } catch(TestException e) {
                    terminateAllTasks();
                    throw e;
                } finally {
                    postRunTask();
                }
            }
        } finally {
            finalizeTasks();
        }
    }

    protected abstract void postRunTask();

    protected static void setAttributeObject(String name, Object value) {
        attributeObjects.put(name, value);
    }

    private static void terminateAllTasks() {
        for(Thread t : testThreads.values()) {
            t.interrupt();
        }
    }
}
