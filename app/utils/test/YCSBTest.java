package utils.test;

/**
 * Created by ricardolorenzo on 31/07/2014.
 */
public class YCSBTest implements Test {
    private Integer phase;
    private String binaryFile;
    private String workingDirectory;
    private String binaryDirectory;
    private Integer threads;
    private String mongoDbUrl;
    private String workloadFilePath;
    private Integer bulkSize;
    private Integer insertStart = 0;
    private Integer insertCount = 0;

    public YCSBTest(String binaryDirectory, String binaryFile, Integer phase, String remoteWorloadFilePath, Integer threads,
                    Integer bulkSize) throws TestException {
        setPhase(phase);
        setBinaryDirectory(binaryDirectory);
        setBinaryFile(binaryFile);
        setWorkloadFilePath(remoteWorloadFilePath);
        setThreads(threads);
        setBulkSize(bulkSize);
    }

    @Override
    public String getSystemCommand() {
        StringBuilder sb = new StringBuilder();
        sb.append(binaryDirectory);
        sb.append("/");
        sb.append(binaryFile);
        sb.append(" ");
        switch(phase) {
            case PHASE_LOAD:
                sb.append("load");
                break;
            default:
                sb.append("run");
                break;
        }
        sb.append(" mongodb -P ");
        sb.append(workloadFilePath);
        sb.append(" -p measurementtype=timeseries");
        sb.append(" -p measurementoutput=live");
        if(insertStart > 0) {
            sb.append(" -p insertstart=");
            sb.append(insertStart);
        }
        if(insertCount > 0) {
            sb.append(" -p insertcount=");
            sb.append(insertCount);
        }
        if(threads > 1) {
            sb.append(" -threads ");
            sb.append(threads);
        }
        sb.append(" -p mongodb.url=");
        sb.append(mongoDbUrl);
        if(bulkSize > 1) {
            sb.append(" -bulk ");
            sb.append(bulkSize);
        }
        return sb.toString();
    }

    @Override
    public String getBinaryDirectory() {
        return binaryDirectory;
    }

    @Override
    public String getBinaryFile() {
        return binaryFile;
    }

    @Override
    public Integer getInsertStart() {
        return insertStart;
    }

    @Override
    public Integer getInsertCount() {
        return insertCount;
    }

    @Override
    public String getWorkingDirectory() {
        return workingDirectory;
    }

    @Override
    public void setBinaryDirectory(String binaryDirectory) {
        this.binaryDirectory = binaryDirectory;
    }

    @Override
    public void setBinaryFile(String binaryFile) {
        this.binaryFile = binaryFile;
    }

    @Override
    public void setInsertStart(Integer insertStart) {
        this.insertStart = insertStart;
    }

    @Override
    public void setInsertCount(Integer insertCount) {
        this.insertCount = insertCount;
    }

    @Override
    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Override
    public String getDatabaseUrl() {
        return mongoDbUrl;
    }

    @Override
    public void setDatabaseUrl(String mongoDbUrl) {
        if(mongoDbUrl == null) {
            return;
        }
        if(!mongoDbUrl.startsWith("mongodb://") && !mongoDbUrl.contains("//")) {
            mongoDbUrl = "mongodb://".concat(mongoDbUrl);
        }
        this.mongoDbUrl = mongoDbUrl;
    }

    @Override
    public Integer getPhase() {
        return phase;
    }

    public void setPhase(Integer phase) throws TestException {
        switch(phase) {
            case PHASE_LOAD:
            case PHASE_RUN:
                break;
            default:
                throw new TestException("invalid phase");
        }
        this.phase = phase;
    }

    public Integer getThreads() {
        return threads;
    }

    public void setThreads(Integer threads) {
        this.threads = threads;
    }

    public Integer getBulkSize() {
        return bulkSize;
    }

    public void setBulkSize(Integer bulkSize) {
        this.bulkSize = bulkSize;
    }

    public String getWorkloadFilePath() {
        return workloadFilePath;
    }

    public void setWorkloadFilePath(String workloadFilePath) {
        this.workloadFilePath = workloadFilePath;
    }
}
