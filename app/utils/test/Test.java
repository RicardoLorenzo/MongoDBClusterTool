package utils.test;

/**
 * Created by ricardolorenzo on 31/07/2014.
 */
public interface Test {
    public static final int PHASE_LOAD = 1;
    public static final int PHASE_RUN = 2;

    public String  getSystemCommand();

    public String getBinaryDirectory();

    public String getBinaryFile();

    public Integer getInsertStart();

    public Integer getInsertCount();

    public Integer getPhase();

    public String getDatabaseUrl();

    public String getWorkingDirectory();

    public void setBinaryDirectory(String binaryDirectory);

    public void setBinaryFile(String binaryFile);

    public void setInsertStart(Integer insertStart);

    public void setInsertCount(Integer insertCount);

    public void setDatabaseUrl(String databaseUrl);

    public void setWorkingDirectory(String workingDirectory);
}
