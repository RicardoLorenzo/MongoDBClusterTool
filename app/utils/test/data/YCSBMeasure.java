package utils.test.data;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by ricardolorenzo on 31/07/2014.
 */
public class YCSBMeasure implements Measure {
    private String nodeAddress;
    private Long time;
    private TimeUnit timeUnit;
    private Map<Integer, Integer> operations;
    private Map<Integer, Float> latency;

    public YCSBMeasure(String nodeAddress) {
        setNodeAddress(nodeAddress);
        timeUnit = TimeUnit.SECONDS;
        operations = new HashMap<>();
        latency = new HashMap<>();
    }
    
    @Override
    public String getNodeAddress() {
        return nodeAddress;
    }

    @Override
    public Long getTime() {
        return time;
    }

    @Override
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    @Override
    public Float getAverageLatencyByType(int type) {
        Float total = latency.get(type);
        if(total == null) {
            return 0F;
        }
        return total;
    }

    @Override
    public Integer getTotalOperationsByType(int type) {
        Integer total = operations.get(type);
        if(total == null) {
            return 0;
        }
        return total;
    }

    @Override
    public void setNodeAddress(String address) {
        this.nodeAddress = address;
    }

    @Override
    public void setTime(Long time) {
        this.time = time;
    }

    @Override
    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    @Override
    public void setAverageLatencyByType(int type, Float average) {
        this.latency.put(type, average);
    }

    @Override
    public void setTotalOperationsByType(int type, Integer total) {
        this.operations.put(type, total);
    }

    public static YCSBMeasure parseMeasure(String nodeAddress, String data) {
        if(data == null || data.isEmpty() || !data.startsWith("[") || !data.contains("]") || !data.contains(",")) {
            return null;
        }
        data = data.trim();
        if(data.contains("\n")) {
            data = data.substring(data.indexOf("\n"));
        }

        String[] tokens = data.split(",");
        if(tokens.length < 4) {
            return null;
        }
        for(int i = 0; i < tokens.length; i++) {
            tokens[i] = tokens[i].trim();
        }
        if(!tokens[0].startsWith("[") || !tokens[0].endsWith("]")) {
            return null;
        }

        Float average;
        Long second;
        Integer count;
        YCSBMeasure measure = new YCSBMeasure(nodeAddress);
        if(tokens[2].matches("^\\d+(\\.\\d)?$")) {
            try {
                count = Integer.parseInt(tokens[2]);
            } catch(NumberFormatException e) {
                return null;
            }
        } else {
            return null;
        }
        if(tokens[1].matches("^\\d+(\\.\\d)?$")) {
            try {
                average = Float.parseFloat(tokens[1]);
            } catch(NumberFormatException e) {
                return null;
            }
        } else {
            return null;
        }
        if(tokens[0].matches("^\\d+(\\.\\d)?$")) {
            try {
                second = Long.parseLong(tokens[0]);
            } catch(NumberFormatException e) {
                return null;
            }
        } else {
            return null;
        }

        measure.setTime(second);
        measure.setTimeUnit(TimeUnit.SECONDS);
        switch(tokens[0].substring(1, data.indexOf("]"))) {
            case "INSERT":
                measure.setTotalOperationsByType(Measure.INSERT, count);
                measure.setAverageLatencyByType(Measure.INSERT, average);
                break;
            case "UPDATE":
                measure.setTotalOperationsByType(Measure.UPDATE, count);
                measure.setAverageLatencyByType(Measure.UPDATE, average);
                break;
            case "DELETE":
                measure.setTotalOperationsByType(Measure.DELETE, count);
                measure.setAverageLatencyByType(Measure.DELETE, average);
                break;
            case "SCAN":
                measure.setTotalOperationsByType(Measure.SCAN, count);
                measure.setAverageLatencyByType(Measure.SCAN, average);
                break;
            case "READ":
                measure.setTotalOperationsByType(Measure.READ, count);
                measure.setAverageLatencyByType(Measure.READ, average);
                break;
            default:
                return null;
        }
        return measure;
    }
}
