package utils.test;

/**
 * Created by ricardolorenzo on 31/07/2014.
 */
public class YCSBTest implements Test {
    public static final int PHASE_LOAD = 1;
    public static final int PHASE_RUN = 2;
    private Integer phase;
    private String binaryFile;
    private String workingDirectory;
    private String binaryDirectory;
    private Integer threads;
    private String mongoDbUrl;
    private String workloadFilePath;
    private Integer bulkSize;

    public YCSBTest(String binaryDirectory, String binaryFile, String remoteWorloadFilePath, Integer threads,
                    Integer bulkSize) {
        setPhase(PHASE_RUN);
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
        sb.append(" -P ");
        sb.append(workloadFilePath);
        if(threads > 1) {
            sb.append(" -threads ");
            sb.append(threads);
        }
        sb.append(" -p mongodb.url=");
        sb.append(mongoDbUrl);
        if(bulkSize > 1) {
            sb.append(" -threads ");
            sb.append(threads);
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

    public Integer getPhase() {
        return phase;
    }

    public void setPhase(Integer phase) {
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
