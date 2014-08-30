package utils.test;

import utils.test.data.Measure;
import utils.test.data.YCSBMeasure;

import java.util.Queue;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by ricardolorenzo on 31/07/2014.
 */
public class YCSBTestNodeRunner extends TestNodeRunner {

    public YCSBTestNodeRunner(Queue<Measure> measurementQueue, ConcurrentMap<String, String> errors,
                              String jumpAddress, String nodeAddress, Test test) {
        super(measurementQueue, errors, jumpAddress, nodeAddress, test);
    }

    @Override
    protected Measure getMeasure(String line) {
        return YCSBMeasure.parseMeasure(getNodeAddress(), line);
    }
}
