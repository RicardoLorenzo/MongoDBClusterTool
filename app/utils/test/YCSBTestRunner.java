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
        this.threads = threads;
        this.bulkCount = bulkCount;
    }

    @Override
    protected Test getTest(Integer phase) throws TestException {
        String user = ConfigurationService.TEST_USER;
        if(user.contains("@")) {
            user = user.substring(0, user.indexOf("@"));
        }
        StringBuilder sb = new StringBuilder();
        sb.append("/home/");
        sb.append(user);
        sb.append("/");
        sb.append(TestConfiguration.YCSB_DIRECTORY);
        sb.append("/bin");
        Test t = new YCSBTest(sb.toString(), "ycsb", phase, WORKLOAD_FILE_PATH, threads, bulkCount);
        t.setDatabaseUrl(databaseUrl);
        return t;
    }

    private static SSHClient connect(String address) throws SSHException, IOException {
        if(!hasAttributeObject("ssh-client")) {
            SSHClient client = new SSHClient(address, 22);
            client.connect(ConfigurationService.TEST_USER);
            setAttributeObject("ssh-client", client);
            return client;
        } else {
            return SSHClient.class.cast(getAttributeObject("ssh-client"));
        }
    }

    private static void disconnect() {
        if(hasAttributeObject("ssh-client")) {
            SSHClient.class.cast(getAttributeObject("ssh-client")).disconnect();
        }
    }

    @Override
    protected void finalizeTasks() {
        disconnect();
    }


    @Override
    protected void initializeTasks(Integer phase, String jumpAddress, List<String> testNodeAddresses) throws TestException {
        Random r = new Random();
        String randomTestNode = testNodeAddresses.get(r.nextInt(testNodeAddresses.size() - 1));
        try {
            SSHClient client = connect(jumpAddress);
            switch(phase) {
                case Test.PHASE_LOAD: {
                        File f = File.createTempFile("init-test", ".js");
                        try {
                            FilePermissions permissions = new FilePermissions(FilePermissions.READ + FilePermissions.WRITE,
                                    FilePermissions.READ, FilePermissions.READ);
                            StringBuilder sb = new StringBuilder();
                            sb.append("db = db.getSiblingDB(\"ycsb\");\n");
                            sb.append("db.dropDatabase()");
                            sb.append("sh.enableSharding(\"ycsb\")");
                            sb.append("sh.shardCollection(\"ycsb.usertable\", { \"_id\": \"hashed\" })");
                            FileUtils.writeFile(f, sb.toString());
                            client.forwardConnect(randomTestNode, ConfigurationService.TEST_USER, 22);
                            client.sendForwardFile(randomTestNode, f, "/tmp/init-test.js", permissions);
                            client.sendForwardCommand(randomTestNode, "mongo /tmp/init-test.js");
                        } catch(FileLockException e) {
                            throw new TestException(e);
                        } finally {
                            f.delete();
                            //client.forwardDisconnect(randomTestNode);
                        }
                    }
                    break;
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
            SSHClient client = connect(jumpAddress);
            File f = File.createTempFile("workload", ".test");
            try {
                FileUtils.writeFile(f, this.workload.toString());
                client.forwardConnect(testNodeAddress, ConfigurationService.TEST_USER, 22);
                client.sendForwardFile(testNodeAddress, f, WORKLOAD_FILE_PATH, permissions);
            } catch(FileLockException e) {
                throw new TestException(e);
            } finally {
                f.delete();
                //client.forwardDisconnect(testNodeAddress);
            }
        } catch(SSHException e) {
            throw new TestException(e);
        } catch(IOException e) {
            throw new TestException(e);
        }
    }

    @Override
    protected void postRunTask() {
        // none
    }
}
