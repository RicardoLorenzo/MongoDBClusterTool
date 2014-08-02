package utils.test.data;

/**
 * Created by ricardolorenzo on 01/08/2014.
 */
public class YCSBWorkload {
    private Integer recordcount;
    private Integer operationcount;
    private String workload;
    private Boolean readallfields;
    private Float readproportion;
    private Float updateproportion;
    private Float scanproportion;
    private Float insertproportion;
    private String requestdistribution;

    public YCSBWorkload() {
        /**
         * Default values
         */
        this.recordcount = 1000;
        this.operationcount = 1000;
        this.workload = "com.yahoo.ycsb.workloads.CoreWorkload";
        this.readallfields = true;
        this.readproportion = 0.75F;
        this.updateproportion = 0.25F;
        this.scanproportion = 0F;
        this.insertproportion = 0F;
        this.requestdistribution = "zipfian";
    }

    public Integer getRecordcount() {
        return recordcount;
    }

    public void setRecordcount(Integer recordcount) {
        this.recordcount = recordcount;
    }

    public Integer getOperationcount() {
        return operationcount;
    }

    public void setOperationcount(Integer operationcount) {
        this.operationcount = operationcount;
    }

    public String getWorkload() {
        return workload;
    }

    public void setWorkload(String workload) {
        this.workload = workload;
    }

    public Boolean getReadallfields() {
        return readallfields;
    }

    public void setReadallfields(Boolean readallfields) {
        this.readallfields = readallfields;
    }

    public Float getReadproportion() {
        return readproportion;
    }

    public void setReadproportion(Float readproportion) {
        this.readproportion = readproportion;
    }

    public Float getUpdateproportion() {
        return updateproportion;
    }

    public void setUpdateproportion(Float updateproportion) {
        this.updateproportion = updateproportion;
    }

    public Float getScanproportion() {
        return scanproportion;
    }

    public void setScanproportion(Float scanproportion) {
        this.scanproportion = scanproportion;
    }

    public Float getInsertproportion() {
        return insertproportion;
    }

    public void setInsertproportion(Float insertproportion) {
        this.insertproportion = insertproportion;
    }

    public String getRequestdistribution() {
        return requestdistribution;
    }

    public void setRequestdistribution(String requestdistribution) {
        this.requestdistribution = requestdistribution;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("#\nrecordcount=");
        sb.append(recordcount);
        sb.append("\noperationcount=");
        sb.append(operationcount);
        sb.append("\nworkload=");
        sb.append(workload);
        sb.append("\n\nreadallfields=");
        sb.append(readallfields);
        sb.append("\n\nreadproportion=");
        sb.append(readproportion);
        sb.append("\nupdateproportion=");
        sb.append(updateproportion);
        sb.append("\nscanproportion=");
        sb.append(scanproportion);
        sb.append("\ninsertproportion=");
        sb.append(insertproportion);
        sb.append("\n\nrequestdistribution=zipfian\n");
        return sb.toString();
    }
}
