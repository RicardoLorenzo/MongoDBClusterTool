/*
 * Copyright 2014 Ricardo Lorenzo<unshakablespirit@gmail.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package services;

import conf.PlayConfiguration;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import utils.file.FileLockException;
import utils.file.FileUtils;
import utils.gce.GoogleComputeEngineException;
import utils.gce.storage.GoogleCloudStorageClient;
import utils.gce.storage.GoogleCloudStorageException;
import utils.puppet.PuppetConfiguration;
import utils.puppet.PuppetConfigurationException;
import utils.ssh.FilePermissions;
import utils.ssh.SSHClient;
import utils.ssh.SSHException;
import utils.test.TestConfiguration;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Created by ricardolorenzo on 27/07/2014.
 */
@Service(value = "conf-service")
@Configurable
public class ConfigurationService {
    public static final String CLUSTER_USER = "mongodb@localhost";
    public static final String TEST_USER = "test@localhost";
    public static final String NODE_NAME_PUPPET = "puppetmaster";
    public static final String NODE_NAME_CONF = "conf";
    public static final String NODE_NAME_SHARD = "shard";
    public static final String NODE_NAME_TEST = "test";
    public static final String NODE_NAME_TEST_JUMP = "test-jump";
    public static final String NODE_TAG_PUPPET = "puppet";
    public static final String NODE_TAG_CONF = "config";
    public static final String NODE_TAG_SHARD = "shard";
    public static final String NODE_TAG_TEST = "test";
    public static final String NODE_TAG_TEST_JUMP = "test-jump";
    private static GoogleAuthenticationService authService;
    private static GoogleComputeEngineService googleComputeService;
    private static GoogleCloudStorageClient googleStorageClient;
    private static final String projectId;
    private static final String bucketId;
    private static final String applicationDirectory;

    @Inject
    void setConfigurationService(@Qualifier("gce-service") GoogleComputeEngineService googleService) {
        ConfigurationService.googleComputeService = googleService;
    }

    @Inject
    void setConfigurationService(@Qualifier("gauth-service") GoogleAuthenticationService googleService) {
        ConfigurationService.authService = googleService;
    }

    static {
        applicationDirectory = PlayConfiguration.getProperty("application.directory");
        projectId = PlayConfiguration.getProperty("google.projectId");
        bucketId = PlayConfiguration.getProperty("google.bucketId");
    }

    private static void checkGoogleAuthentication() throws GoogleComputeEngineException {
        if(googleStorageClient == null) {
            if(GoogleAuthenticationService.getCredential() == null) {
                throw new GoogleComputeEngineException("not authenticated on Google");
            }
            googleStorageClient = new GoogleCloudStorageClient(GoogleAuthenticationService.getAuthentication());
        }
    }

    private static File getClusterNameFile() {
        return new File(applicationDirectory + "/cluster.name");
    }

    private static File getTestNodeListFile() {
        return new File(applicationDirectory + "/test-nodes.list");
    }

    private static File getStartupScriptFile() throws IOException {
        return File.createTempFile("startup-script", ".sh");
    }

    private static String getInternalServerName(String instanceName) throws GoogleComputeEngineException {
        StringBuilder name = new StringBuilder();
        name.append(instanceName);
        name.append(".c.");
        name.append(ConfigurationService.projectId);
        name.append(".internal");
        return name.toString();
    }

    public static String getServerName(String clusterName, String nodeName) {
        StringBuilder name = new StringBuilder();
        name.append(clusterName);
        name.append("-");
        name.append(nodeName);
        return name.toString();
    }

    public static String generatePuppetMasterStartupScript(String clusterName, String networkName, String diskRaid,
                                                           String dataFileSystem) throws IOException,
            GoogleComputeEngineException, GoogleCloudStorageException {
        checkGoogleAuthentication();
        if(bucketId == null || bucketId.isEmpty()) {
            throw new GoogleCloudStorageException("parameter 'google.bucketId' not specified in the configuration");
        }
        StringBuilder scriptPath = new StringBuilder();
        scriptPath.append("autostart/");
        scriptPath.append(clusterName);
        scriptPath.append("/puppetmaster_autostart.sh");
        /**
         * There is a limit of 32K for the metadata in Google Compute Engine. To avoid any size problems
         * the application upload the file to Google Storage and store the link in the metadata.
         */
        String puppetScriptContent = PuppetConfiguration.getPuppetStartupScriptContent(clusterName,
                googleComputeService.getNetworkRange(networkName), diskRaid, dataFileSystem);
        return googleStorageClient.putFile(bucketId, scriptPath.toString(), "plain/text", puppetScriptContent.getBytes());
    }

    public static String getClusterName() throws GoogleComputeEngineException {
        File f = getClusterNameFile();
        if(!f.exists()) {
            return null;
        }
        try {
            return FileUtils.readFileAsString(f);
        } catch(IOException e) {
            throw new GoogleComputeEngineException("cannot read cluster name file: " + e.getMessage());
        }
    }

    public static File getPuppetNodeStartupScriptFile(String clusterName) throws GoogleComputeEngineException {
        try {
            File f = getStartupScriptFile();
            FileUtils.writeFile(f, PuppetConfiguration.getNodeStartupScriptContent(
                    getInternalServerName(getServerName(clusterName, NODE_NAME_PUPPET))));
            return f;
        } catch(IOException e) {
            throw new GoogleComputeEngineException("cannot write the startup script: " + e.toString());
        } catch(FileLockException e) {
            throw new GoogleComputeEngineException("cannot write the startup script: " + e.toString());
        }
    }

    public static String getPuppetFile(final Integer type, String name) throws PuppetConfigurationException, GoogleComputeEngineException {
        String serverName = googleComputeService.getClusterPublicAddress();
        try {
            if(serverName == null || serverName.isEmpty()) {
                return "";
            }
            StringBuilder destinationPath = new StringBuilder();
            SSHClient client = new SSHClient(serverName, 22);
            switch(type) {
                case PuppetConfiguration.PUPPET_MANIFEST:
                    destinationPath.append(PuppetConfiguration.getPuppetManifestsDirectory());
                    break;
                case PuppetConfiguration.PUPPET_FILE:
                    destinationPath.append(PuppetConfiguration.getPuppetFilesDirectory());
                    break;
                default:
                    throw new PuppetConfigurationException("incorrect puppet file type");
            }
            destinationPath.append("/");
            destinationPath.append(name);
            try {
                client.connect(CLUSTER_USER);
                for(Map.Entry<String, byte[]> e : client.getFiles(destinationPath.toString()).entrySet()) {
                    return new String(e.getValue());
                }
                return null;
            } catch(SSHException e) {
                throw new GoogleComputeEngineException(e);
            } finally {
                client.disconnect();
            }
        } catch(IOException e) {
            throw new GoogleComputeEngineException(e);
        }
    }

    public static File getTestJumpNodeStartupScriptFile(String networkName) throws GoogleComputeEngineException {
        checkGoogleAuthentication();
        try {
            File f = getStartupScriptFile();
            FileUtils.writeFile(f, TestConfiguration.getJumpServerStartupScriptContent(
                    googleComputeService.getNetworkRange(networkName)));
            return f;
        } catch(IOException e) {
            throw new GoogleComputeEngineException("cannot write the startup script: " + e.toString());
        } catch(FileLockException e) {
            throw new GoogleComputeEngineException("cannot write the startup script: " + e.toString());
        }
    }

    public static File getTestNodeStartupScriptFile(String clusterName) throws GoogleComputeEngineException {
        try {
            File f = getStartupScriptFile();
            FileUtils.writeFile(f, TestConfiguration.getNodeStartupScriptContent(
                    getInternalServerName(getServerName(clusterName, NODE_NAME_TEST_JUMP))));
            return f;
        } catch(IOException e) {
            throw new GoogleComputeEngineException("cannot write the startup script: " + e.toString());
        } catch(FileLockException e) {
            throw new GoogleComputeEngineException("cannot write the startup script: " + e.toString());
        }
    }

    public static List<String> listPuppetFiles(final Integer type) throws PuppetConfigurationException, GoogleComputeEngineException {
        String serverName = googleComputeService.getClusterPublicAddress();
        List<String> files = new ArrayList<String>();
        try {
            if(serverName == null || serverName.isEmpty()) {
                return files;
            }
            StringBuilder destinationPath = new StringBuilder();
            SSHClient client = new SSHClient(serverName, 22);
            switch(type) {
                case PuppetConfiguration.PUPPET_MANIFEST:
                    destinationPath.append(PuppetConfiguration.getPuppetManifestsDirectory());
                    break;
                case PuppetConfiguration.PUPPET_FILE:
                    destinationPath.append(PuppetConfiguration.getPuppetFilesDirectory());
                    break;
                default:
                    throw new PuppetConfigurationException("incorrect puppet file type");
            }
            try {
                client.connect(CLUSTER_USER);
                if(client.sendCommand("ls", destinationPath.toString()) > 0) {
                    throw new GoogleComputeEngineException("cannot list the files for directory: " + destinationPath.toString());
                }
                StringTokenizer st = new StringTokenizer(client.getStringOutput());
                while(st.hasMoreTokens()) {
                    files.add(st.nextToken());
                }
                return files;
            } catch(SSHException e) {
                throw new GoogleComputeEngineException(e);
            } finally {
                client.disconnect();
            }
        } catch(IOException e) {
            throw new GoogleComputeEngineException(e);
        }
    }

    public static void uploadPuppetFile(final Integer type, String fileName, File file)
            throws PuppetConfigurationException, GoogleComputeEngineException {
        String serverName = googleComputeService.getClusterPublicAddress();
        try {
            StringBuilder temporaryFile = new StringBuilder();
            temporaryFile.append("/tmp/");
            temporaryFile.append(fileName);
            StringBuilder destinationPath = new StringBuilder();
            SSHClient client = new SSHClient(serverName, 22);
            FilePermissions permissions = new FilePermissions(FilePermissions.READ, FilePermissions.READ,
                    FilePermissions.READ);
            switch(type) {
                case PuppetConfiguration.PUPPET_MANIFEST:
                    destinationPath.append(PuppetConfiguration.getPuppetManifestsDirectory());
                    break;
                case PuppetConfiguration.PUPPET_FILE:
                    destinationPath.append(PuppetConfiguration.getPuppetFilesDirectory());
                    break;
                default:
                    throw new PuppetConfigurationException("incorrect puppet file type");
            }
            destinationPath.append("/");
            destinationPath.append(fileName);
            try {
                client.connect(CLUSTER_USER);
                client.sendFile(file, temporaryFile.toString(), permissions);
                client.sendCommand("sudo", "mv", temporaryFile.toString(), destinationPath.toString(), "&&",
                        "puppet", "kick", "--all");
            } catch(SSHException e) {
                throw new GoogleComputeEngineException(e);
            } finally {
                client.disconnect();
            }
        } catch(IOException e) {
            throw new GoogleComputeEngineException(e);
        }
    }

    public static void deletePuppetFile(final Integer type, String fileName)
            throws PuppetConfigurationException, GoogleComputeEngineException {
        String serverName = googleComputeService.getClusterPublicAddress();
        if(fileName == null || fileName.isEmpty()) {
            throw new PuppetConfigurationException("file to be deleted is not defined");
        }
        try {
            StringBuilder destinationPath = new StringBuilder();
            SSHClient client = new SSHClient(serverName, 22);
            switch(type) {
                case PuppetConfiguration.PUPPET_MANIFEST:
                    destinationPath.append(PuppetConfiguration.getPuppetManifestsDirectory());
                    break;
                case PuppetConfiguration.PUPPET_FILE:
                    destinationPath.append(PuppetConfiguration.getPuppetFilesDirectory());
                    break;
                default:
                    throw new PuppetConfigurationException("incorrect puppet file type");
            }
            destinationPath.append("/");
            destinationPath.append(fileName);
            try {
                client.connect(CLUSTER_USER);
                System.out.println("Delete: " + destinationPath.toString());
                client.sendCommand("sudo", "rm", "-f", destinationPath.toString(), "&&", "puppet", "kick", "--all");
            } catch(SSHException e) {
                throw new GoogleComputeEngineException(e);
            } finally {
                client.disconnect();
            }
        } catch(IOException e) {
            throw new GoogleComputeEngineException(e);
        }
    }

    public static void setClusterName(String name) throws GoogleComputeEngineException {
        File f = getClusterNameFile();
        if(name == null) {
            f.delete();
        } else {
            try {
                FileUtils.writeFile(f, name);
            } catch(IOException e) {
                throw new GoogleComputeEngineException("cannot write cluster name file: " + e.getMessage());
            } catch(FileLockException e) {
                throw new GoogleComputeEngineException("cannot write cluster name file: " + e.getMessage());
            }
        }
    }
}
