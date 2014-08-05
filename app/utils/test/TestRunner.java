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

    protected abstract Test getTest(Integer testPhase, Integer nodeNumber) throws TestException;

    protected static void cleanAttributeObjects() {
        attributeObjects.clear();
    }

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

    public static boolean hasTestNodesRunning() {
        for(Map.Entry<String, Boolean> e : getTestNodesStatuses().entrySet()) {
            if(e.getValue()) {
                return true;
            }
        }
        return false;
    }

    protected abstract void initializeTasks(Integer phase, String jumpAddress, List<String> nodeAddresses)
            throws TestException;

    protected abstract void preRunTask(String jumpAddress, String testNodeAddress) throws TestException;

    public void runTest(Integer phase, String jumpAddress, List<String> testNodeAddresses) throws TestException {
        try {
            Integer nodeCount = 1;
            initializeTasks(phase, jumpAddress, testNodeAddresses);
            for(String testNodeAddress : testNodeAddresses) {
                try {
                    preRunTask(jumpAddress, testNodeAddress);
                    TestNodeRunner runner = new YCSBTestNodeRunner(queue, jumpAddress, testNodeAddress, getTest(phase, nodeCount));
                    nodeCount++;
                    Thread t = new Thread(runner);
                    t.setName(testNodeAddress);
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
