package utils.test;

import services.ConfigurationService;
import utils.file.FileLockException;
import utils.file.FileUtils;
import utils.ssh.FilePermissions;
import utils.ssh.SSHClient;
import utils.ssh.SSHException;
import utils.test.data.YCSBWorkload;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

/**
 * Created by ricardolorenzo on 01/08/2014.
 */
public class YCSBTestRunner extends TestRunner {
    /**
     * Every node contain his own mongos process
     */
    private static final String databaseUrl = "mongodb://localhost";
    private static final String WORKLOAD_FILE_PATH = "/tmp/workload.test";
    private YCSBWorkload workload;
    private Integer threads;
    private Integer bulkCount;

    public YCSBTestRunner(YCSBWorkload workload, Integer threads, Integer bulkCount) {
        super();
        this.workload = workload;
    }

    @Override
    protected Test getTest() {
        StringBuilder sb = new StringBuilder();
        sb.append("/home/");
        sb.append(ConfigurationService.TEST_USER);
        sb.append("/");
        sb.append(TestConfiguration.YCSB_DIRECTORY);
        sb.append("/bin");
        Test t = new YCSBTest(sb.toString(), "ycsb", WORKLOAD_FILE_PATH, threads, bulkCount);
        t.setDatabaseUrl(databaseUrl);
        return t;
    }

    @Override
    protected void preRunTask(String jumpAddress, List<String> testNodeAddresses) throws TestException {
        Random r = new Random();
        String randomTestNode = testNodeAddresses.get(r.nextInt(testNodeAddresses.size() - 1));
        try {
            SSHClient client = new SSHClient(jumpAddress, 22);
            File f = File.createTempFile("init-test", ".js");
            try {
                FilePermissions permissions = new FilePermissions(FilePermissions.READ + FilePermissions.WRITE,
                        FilePermissions.READ, FilePermissions.READ);
                client.connect(ConfigurationService.TEST_USER);
                FileUtils.writeFile(f, this.workload);
                client.forwardConnect(randomTestNode, ConfigurationService.TEST_USER, 22);
                StringBuilder sb = new StringBuilder();
                sb.append("db = db.getSiblingDB(\"ycsb\");\n");
                sb.append("db.dropDatabase()");
                sb.append("sh.enableSharding(\"ycsb\")");
                sb.append("sh.shardCollection(\"ycsb.usertable\", { \"_id\": \"hashed\" })");
                FileUtils.writeFile(f, sb.toString());
                client.sendForwardFile(randomTestNode, f, "/tmp/init-test.js", permissions);
                client.sendForwardCommand(randomTestNode, "mongo /tmp/init-test.js");
            } catch(FileLockException e) {
                throw new TestException(e);
            } finally {
                f.delete();
                client.disconnect();
            }
        } catch(SSHException e) {
            throw new TestException(e);
        } catch(IOException e) {
            throw new TestException(e);
        }
    }

    @Override
    protected void preRunTask(String jumpAddress, String testNodeAddress) throws TestException {
        try {
            FilePermissions permissions = new FilePermissions(FilePermissions.READ + FilePermissions.WRITE,
                    FilePermissions.READ, FilePermissions.READ);
            SSHClient client = new SSHClient(jumpAddress, 22);
            File f = File.createTempFile("workload", ".test");
            try {
                client.connect(ConfigurationService.TEST_USER);
                FileUtils.writeFile(f, this.workload);
                client.forwardConnect(testNodeAddress, ConfigurationService.TEST_USER, 22);
                client.sendForwardFile(testNodeAddress, f, WORKLOAD_FILE_PATH, permissions);
            } catch(FileLockException e) {
                throw new TestException(e);
            } finally {
                f.delete();
                client.disconnect();
            }
        } catch(SSHException e) {
            throw new TestException(e);
        } catch(IOException e) {
            throw new TestException(e);
        }
    }
}
