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
    private Map<Integer, Float> operations;

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
    public Long getTime() {
        return time;
    }

    @Override
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    @Override
    public Float getOperationsAverageByType(int type) {
        Float total = operations.get(type);
        if(total == null) {
            return 0F;
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
    public void setTotalOperationsByType(int type, Float total) {
        this.operations.put(type, total);
    }

    public static YCSBMeasure parseMeasure(String nodeAddress, String data) {
        if(data == null || data.isEmpty() || !data.startsWith("[") || !data.contains("]")) {
            return null;
        }
        data = data.trim();
        if(data.contains("\n")) {
            data = data.substring(data.indexOf("\n"));
        }

        Float average = 0F;
        Long second = 0L;
        YCSBMeasure measure = new YCSBMeasure(nodeAddress);
        String result = data.substring(data.indexOf("]") + 1);
        if(result.startsWith(",")) {
            result = result.substring(1, result.length());
        }
        if(result.contains(",")) {
            String[] tokens = result.split(",");
            if(tokens[1].trim().matches("^\\d+(\\.\\d)?$")) {
                try {
                    average = Float.parseFloat(tokens[1].trim());
                } catch(NumberFormatException e) {
                    return null;
                }
            } else {
                return null;
            }
            if(tokens[0].trim().matches("^\\d+(\\.\\d)?$")) {
                try {
                    second = Long.parseLong(tokens[0].trim());
                } catch(NumberFormatException e) {
                    return null;
                }
            } else {
                return null;
            }
        } else if(result.matches("[0-9.]+")) {
            try {
                average = Float.parseFloat(result);
            } catch(NumberFormatException e) {
                return null;
            }
        } else {
            return null;
        }

        measure.setTime(second);
        measure.setTimeUnit(TimeUnit.SECONDS);
        switch(data.substring(1, data.indexOf("]"))) {
            case "INSERT":
                measure.setTotalOperationsByType(Measure.INSERT, average);
                break;
            case "UPDATE":
                measure.setTotalOperationsByType(Measure.UPDATE, average);
                break;
            case "DELETE":
                measure.setTotalOperationsByType(Measure.DELETE, average);
                break;
            case "SCAN":
                measure.setTotalOperationsByType(Measure.SCAN, average);
                break;
            case "READ":
                measure.setTotalOperationsByType(Measure.READ, average);
                break;
            default:
                return null;
        }
        return measure;
    }
}
