package utils.test.data;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by ricardolorenzo on 31/07/2014.
 */
public class YCSBMeasure implements Measure {
    private String nodeAddress;
    private TimeUnit timeUnit;
    private Map<Integer, Long> operations;

    public YCSBMeasure(String nodeAddress) {
        setNodeAddress(nodeAddress);
        timeUnit = TimeUnit.SECONDS;
        operations = new HashMap<>();
    }
    
    @Override
    public String getNodeAddress() {
        return nodeAddress;
    }

    @Override
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    @Override
    public Long getTotalOperationsByType(int type) {
        Long total = operations.get(type);
        if(total == null) {
            return 0L;
        }
        return total;
    }

    @Override
    public void setNodeAddress(String address) {
        this.nodeAddress = address;
    }

    @Override
    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    @Override
    public void setTotalOperationsByType(int type, Long total) {
        this.operations.put(type, total);
    }
}
