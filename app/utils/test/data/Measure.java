package utils.test.data;

import java.util.concurrent.TimeUnit;

/**
 * Created by ricardolorenzo on 31/07/2014.
 */
public interface Measure {
    public final static int SCAN = 1;
    public final static int INSERT = 2;
    public final static int UPDATE = 3;
    public final static int REMOVE = 4;

    public String getNodeAddress();

    public TimeUnit getTimeUnit();

    public Long getTotalOperationsByType(int type);

    public void setNodeAddress(String address);

    public void setTimeUnit(TimeUnit timeUnit);

    public void setTotalOperationsByType(int type, Long total);
}
