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

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Created by ricardolorenzo on 27/07/2014.
 */
@Service(value = "conf-service")
@Configurable
public class ConfigurationService {
    public static final String NODE_NAME_PUPPET = "puppetmaster";
    public static final String NODE_NAME_CONF = "conf";
    public static final String NODE_NAME_SHARD = "shard";
    public static final String NODE_TAG_PUPPET = "puppet";
    public static final String NODE_TAG_CONF = "config";
    public static final String NODE_TAG_SHARD = "shard";
    //public static final String PUPPET_AUTOSTART_SCRIPT = "mongodb_master_autostart.sh";
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
            if(authService.getCredential() == null) {
                throw new GoogleComputeEngineException("not authenticated on Google");
            }
            googleStorageClient = new GoogleCloudStorageClient(authService.getAuthentication());
        }
    }

    private static File getClusterNameFile() {
        return new File(applicationDirectory + "/cluster.name");
    }

    private static File getNodeStartupScriptFile() {
        return new File(applicationDirectory + "/node-startup.sh");
    }

    private static String getPuppetServerName(String clusterName) throws GoogleComputeEngineException {
        StringBuilder puppetServer = new StringBuilder();
        puppetServer.append(clusterName);
        puppetServer.append("-");
        puppetServer.append(ConfigurationService.NODE_NAME_PUPPET);
        puppetServer.append(".c.");
        puppetServer.append(ConfigurationService.projectId);
        puppetServer.append(".internal");
        return puppetServer.toString();
    }

    public static String generatePuppetStartupScript(String clusterName, String networkName) throws PuppetConfigurationException, GoogleComputeEngineException, GoogleCloudStorageException {
        checkGoogleAuthentication();
        if(bucketId == null || bucketId.isEmpty()) {
            throw new GoogleCloudStorageException("parameter 'google.bucketId' not specified in the configuration");
        }
        StringBuilder scriptPath = new StringBuilder();
        scriptPath.append("autostart/");
        scriptPath.append(clusterName);
        scriptPath.append("/puppetmaster_autostart.sh");
        String puppetScriptContent = PuppetConfiguration.getPuppetStartupScriptContent(clusterName, googleComputeService.getNetworkRange(networkName));
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

    public static File getNodeStartupScript(String clusterName) throws GoogleComputeEngineException {
        File f = getNodeStartupScriptFile();
        try {
            FileUtils.writeFile(f, PuppetConfiguration.getNodeStartupScriptContent(clusterName));
        } catch(IOException e) {
            throw new GoogleComputeEngineException("cannot write the startup script: " + e.toString());
        } catch(FileLockException e) {
            throw new GoogleComputeEngineException("cannot write the startup script: " + e.toString());
        } catch(PuppetConfigurationException e) {
            throw new GoogleComputeEngineException("cannot write the startup script: " + e.toString());
        }
        return f;
    }

    public static String getPuppetFile(final Integer type, String name) throws PuppetConfigurationException, GoogleComputeEngineException {
        String serverName = googleComputeService.getClusterPublicAddress();
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
            destinationPath.append(name);
            try {
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

    public static void uploadPuppetFile(final Integer type, String name, byte[] data) throws PuppetConfigurationException, GoogleComputeEngineException {
        String serverName = googleComputeService.getClusterPublicAddress();
        try {
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
            destinationPath.append(name);
            try {
                client.sendFile(data, destinationPath.toString(), permissions);
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
