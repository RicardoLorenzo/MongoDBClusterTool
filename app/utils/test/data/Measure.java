package utils.test.data;

import java.util.concurrent.TimeUnit;

/**
 * Created by ricardolorenzo on 31/07/2014.
 */
public interface Measure {
    public final static int SCAN = 1;
    public final static int INSERT = 2;
    public final static int UPDATE = 3;
    public final static int DELETE = 4;
    public final static int READ = 5;

    public String getNodeAddress();

    public Long getTime();

    public TimeUnit getTimeUnit();

    public Float getOperationsAverageByType(int type);

    public void setNodeAddress(String address);

    public void setTime(Long time);

    public void setTimeUnit(TimeUnit timeUnit);

    public void setTotalOperationsByType(int type, Float total);
}
