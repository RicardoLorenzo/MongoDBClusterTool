package utils.test;

/**
 * Created by ricardolorenzo on 31/07/2014.
 */
public interface Test {

    public String  getSystemCommand();

    public String getBinaryDirectory();

    public String getBinaryFile();

    public String getWorkingDirectory();

    public void setBinaryDirectory(String binaryDirectory);

    public void setBinaryFile(String binaryFile);

    public void setWorkingDirectory(String workingDirectory);
}