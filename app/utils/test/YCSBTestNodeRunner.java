package utils.test;

import utils.test.data.Measure;
import utils.test.data.YCSBMeasure;

import java.util.Queue;

/**
 * Created by ricardolorenzo on 31/07/2014.
 */
public class YCSBTestNodeRunner extends TestNodeRunner {

    public YCSBTestNodeRunner(Queue<Measure> measurementQueue, String jumpAddress, String nodeAddress, Test test) {
        super(measurementQueue, jumpAddress, nodeAddress, test);
    }

    @Override
    protected Measure getMeasure(String line) {
        return YCSBMeasure.parseMeasure(getNodeAddress(), line);
    }
}
