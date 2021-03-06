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

import akka.actor.ActorRef;
import akka.actor.Inbox;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.services.compute.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import play.libs.Akka;
import play.libs.Json;
import utils.gce.GoogleComputeEngineClient;
import utils.gce.GoogleComputeEngineException;
import utils.gce.OperationsCache;
import utils.gce.storage.GoogleCloudStorageException;
import utils.security.SSHKey;
import utils.security.SSHKeyFactory;
import utils.security.SSHKeyStore;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by ricardolorenzo on 18/07/2014.
 */
@Service(value = "gce-service")
@Configurable
public class GoogleComputeEngineService {
    private static Logger log = LoggerFactory.getLogger(GoogleComputeEngineService.class);
    private static GoogleAuthenticationService authService;
    protected static GoogleComputeEngineClient client;
    private static ConfigurationService configurationService;

    @Inject
    void setGoogleService(@Qualifier("gauth-service") GoogleAuthenticationService googleAuthService) {
        GoogleComputeEngineService.authService = googleAuthService;
    }

    @Inject
    void setConfigurationService(@Qualifier("conf-service") ConfigurationService confService) {
        GoogleComputeEngineService.configurationService = confService;
    }

    private static void checkAuthentication() throws GoogleComputeEngineException {
        if(client == null) {
            if(GoogleAuthenticationService.getCredential() == null) {
                throw new GoogleComputeEngineException("not authenticated on Google");
            }
            client = new GoogleComputeEngineClient(GoogleAuthenticationService.getAuthentication());
        }
    }

    public static GoogleComputeEngineClient getClient() {
        return client;
    }

    public static JsonNode listDisks() throws GoogleComputeEngineException {
        checkAuthentication();
        ObjectNode disks = Json.newObject();
        for(Disk d : client.getDisks()) {
            ObjectNode disk = Json.newObject()
                    .put("name", d.getName())
                    .put("description", d.getDescription())
                    .put("sizeGb", d.getSizeGb())
                    .put("status", d.getStatus())
                    .put("type", d.getType());
            disks.set(String.valueOf(d.getId()), disk);
        }
        return Json.newObject().set("disks", disks);
    }

    public static JsonNode listDiskTypes() throws GoogleComputeEngineException {
        checkAuthentication();
        ObjectNode diskTypes = Json.newObject();
        for(DiskType d : client.getDiskTypes()) {
            ObjectNode type = Json.newObject()
                    .put("name", d.getName())
                    .put("description", d.getDescription())
                    .put("validDiskSize", d.getValidDiskSize());
            diskTypes.set(d.getName(), type);
        }
        return Json.newObject().set("diskTypes", diskTypes);
    }

    public static JsonNode listInstances(List<String> tags) throws GoogleComputeEngineException {
        checkAuthentication();
        ObjectNode instances = Json.newObject();
        for(Instance i : client.getInstances(tags)) {
            ObjectNode instance = Json.newObject()
                    .put("name", i.getName())
                    .put("description", i.getDescription())
                    .put("type", i.getMachineType())
                    .put("status", i.getStatus());
            ObjectNode disks = Json.newObject();
            for(AttachedDisk d : i.getDisks()) {
                disks.set(String.valueOf(d.getIndex()), Json.newObject().put("name", d.getDeviceName())
                        .put("type", d.getType())
                        .put("mode", d.getMode())
                        .put("boot", d.getBoot())
                        .put("autodelete", d.getAutoDelete()));
            }
            instance.set("disks", disks);
            ObjectNode interfaces = Json.newObject();
            for(NetworkInterface n : i.getNetworkInterfaces()) {
                interfaces.set(n.getName(), Json.newObject().put("network", n.getNetwork())
                        .put("address", n.getNetworkIP()));
            }
            instance.set("network-interfaces", interfaces);
            instance.set("disks", disks);
            ArrayNode tagList = new ArrayNode(JsonNodeFactory.instance);
            if(i.getTags() != null && i.getTags().getItems() != null) {
                i.getTags().getItems().forEach(tagList::add);
            }
            instance.set("tags", tagList);
            instances.set(i.getId().toString(), instance);
        }
        return Json.newObject().set("instances", instances);
    }

    public static JsonNode listMachineTypes() throws GoogleComputeEngineException {
        checkAuthentication();
        ObjectNode types = Json.newObject();
        for(MachineType m : client.getMachineTypes()) {
            ObjectNode type = Json.newObject()
                    .put("name", m.getName())
                    .put("description", m.getDescription())
                    .put("cpus", m.getGuestCpus())
                    .put("memoryMb", m.getMemoryMb());
            types.set(String.valueOf(m.getId()), type);
        }
        return Json.newObject().set("machinetypes", types);
    }

    public static JsonNode listImages() throws GoogleComputeEngineException {
        checkAuthentication();
        ObjectNode images = Json.newObject();
        for(Image i : client.getImages()) {
            ObjectNode image = Json.newObject()
                    .put("name", i.getName())
                    .put("description", i.getDescription())
                    .put("status", i.getStatus());
            images.set(String.valueOf(i.getId()), image);
        }
        return Json.newObject().set("images", images);
    }

    public static JsonNode listNetworks() throws GoogleComputeEngineException {
        checkAuthentication();
        ObjectNode networks = Json.newObject();
        for(Network n : client.getNetworks()) {
            ObjectNode network = Json.newObject()
                    .put("name", n.getName())
                    .put("description", n.getDescription())
                    .put("ip4Range", n.getIPv4Range())
                    .put("ip4Gateway", n.getGatewayIPv4());
            networks.set(String.valueOf(n.getId()), network);
        }
        return Json.newObject().set("networks", networks);
    }

    public static void listOperations(ActorRef actor, Date lastOperationDate) throws GoogleComputeEngineException {
        checkAuthentication();
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        Inbox inbox = Inbox.create(Akka.system());
        if(lastOperationDate != null) {
            lastOperationDate = new Date(lastOperationDate.getTime() + 1);
        }
        OperationsCache.cleanOldOperationsFromCache();
        for(Operation o : client.getOperations()) {
            if(lastOperationDate != null) {
                if(!OperationsCache.statusHasChanged(o) && !"RUNNING".equalsIgnoreCase(o.getStatus())) {
                    if(o.getStartTime() == null) {
                        continue;
                    }
                    try {
                        Date date = format.parse(o.getStartTime());
                        if(date.before(lastOperationDate)) {
                            continue;
                        }
                    } catch(ParseException e) {
                        continue;
                    }
                }
            }
            OperationsCache.addOperation(o);
            inbox.send(actor, o);
        }
    }

    public static JsonNode listZones() throws GoogleComputeEngineException {
        checkAuthentication();
        ObjectNode zones = Json.newObject();
        for(Zone z : client.getZones()) {
            ObjectNode zone = Json.newObject()
                    .put("name", z.getName())
                    .put("description", z.getDescription())
                    .put("region", z.getRegion())
                    .put("status", z.getStatus());
            zones.set(String.valueOf(z.getId()), zone);
        }
        return Json.newObject().set("zones", zones);
    }

    public static void setZoneName(String name) throws GoogleComputeEngineException {
        checkAuthentication();
        client.setZone(name);
    }

    public boolean clusterExists() throws GoogleComputeEngineException {
        String clusterName = ConfigurationService.getClusterName();
        StringBuilder instance_name = new StringBuilder();
        instance_name.append(clusterName);
        instance_name.append("-");
        instance_name.append(ConfigurationService.NODE_NAME_PUPPET);
        return client.instanceExists(instance_name.toString());
    }

    public List<String> getInstancesNetworkAddresses(List<String> tags) throws GoogleComputeEngineException {
        checkAuthentication();
        List<String> addresses = new ArrayList<>();
        for(Instance instance : client.getInstances(tags)) {
            for(NetworkInterface i : instance.getNetworkInterfaces()) {
                addresses.add(i.getNetworkIP());
            }
        }
        return addresses;
    }

    public List<String> getInstancesNames(List<String> tags) throws GoogleComputeEngineException {
        checkAuthentication();
        List<String> names = new ArrayList<>();
        for(Instance instance : client.getInstances(tags)) {
            names.add(instance.getName());
        }
        return names;
    }

    public String getClusterPublicAddress() throws GoogleComputeEngineException {
        checkAuthentication();
        String clusterName = ConfigurationService.getClusterName();
        StringBuilder instance_name = new StringBuilder();
        instance_name.append(clusterName);
        instance_name.append("-");
        instance_name.append(ConfigurationService.NODE_NAME_PUPPET);
        Instance instance = client.getInstance(instance_name.toString());
        if(instance != null) {
            for(NetworkInterface i : instance.getNetworkInterfaces()) {
                List<AccessConfig> accessConfigList = i.getAccessConfigs();
                if(accessConfigList != null) {
                    for(AccessConfig c : accessConfigList) {
                        return c.getNatIP();
                    }
                }
            }
        }
        return null;
    }

    public String getClusterNetwork() throws GoogleComputeEngineException {
        checkAuthentication();
        String clusterName = ConfigurationService.getClusterName();
        StringBuilder instance_name = new StringBuilder();
        instance_name.append(clusterName);
        instance_name.append("-");
        instance_name.append(ConfigurationService.NODE_NAME_PUPPET);
        Instance instance = client.getInstance(instance_name.toString());
        if(instance != null) {
            for(NetworkInterface i : instance.getNetworkInterfaces()) {
                return i.getNetwork();
            }
        }
        return null;
    }

    public String getJumpServerPublicAddress() throws GoogleComputeEngineException {
        checkAuthentication();
        String clusterName = ConfigurationService.getClusterName();
        StringBuilder instance_name = new StringBuilder();
        instance_name.append(clusterName);
        instance_name.append("-");
        instance_name.append(ConfigurationService.NODE_NAME_TEST_JUMP);
        Instance instance = client.getInstance(instance_name.toString());
        if(instance != null) {
            for(NetworkInterface i : instance.getNetworkInterfaces()) {
                List<AccessConfig> accessConfigList = i.getAccessConfigs();
                if(accessConfigList != null) {
                    for(AccessConfig c : accessConfigList) {
                        return c.getNatIP();
                    }
                }
            }
        }
        return null;
    }

    public String getNetworkRange(String networkName) throws GoogleComputeEngineException{
        Network n = client.getNetwork(networkName);
        return n.getIPv4Range();
    }

    public void createCluster(String clusterName, Integer shards, Integer processes, Integer disksPerShard,
                              String machineType, List<String> network, String sourceImage, String diskType,
                              String diskRaid, String dataFileSystem, Integer dataDiskSizeGb,
                              Integer rootDiskSizeGb) throws GoogleComputeEngineException, GoogleCloudStorageException {
        List<String> tags;
        SSHKey sshKey = SSHKeyFactory.generateKey();
        StringBuilder machinePrefix = new StringBuilder();
        machinePrefix.append("https://www.googleapis.com/compute/v1/projects/");
        machinePrefix.append(client.getProjectId());
        machinePrefix.append("/zones/");
        machinePrefix.append(client.getZone());
        machinePrefix.append("/machineTypes/");

        if(ConfigurationService.getClusterName() != null) {
            throw new GoogleComputeEngineException("cluster [" + ConfigurationService.getClusterName() +
                    "] is already created, delete it first to create another");
        }
        if(clusterName == null) {
            throw new GoogleComputeEngineException("cluster name not defined");
        }
        if(network == null || network.isEmpty()) {
            throw new GoogleComputeEngineException("network not defined");
        }

        File startupScript = ConfigurationService.getPuppetNodeStartupScriptFile(clusterName);
        /**
         * Verify the machine type to get the proper Link
         */
        for(MachineType t : client.getMachineTypes()) {
            if(machineType != null && machineType.equals(t.getName())) {
                machineType = t.getSelfLink();
                break;
            }
        }

        /**
         * Verify the disk type to get the proper Link
         */
        for(DiskType t : client.getDiskTypes()) {
            if(diskType != null && diskType.equals(t.getName())) {
                diskType = t.getSelfLink();
                break;
            }
        }

        /**
         * Verify the source image to get the proper Link
         */
        for(Image i : client.getImages()) {
            if(sourceImage != null && sourceImage.equals(i.getName())) {
                sourceImage = i.getSelfLink();
                break;
            }
        }

        /**
         * Verify the networks to get the proper Link
         */
        if(network != null) {
            for(Network n : client.getNetworks()) {
                for(int i = 0; i < network.size(); i++) {
                    if(network.get(i) != null && network.get(i).equals(n.getName())) {
                        network.set(i, n.getSelfLink());
                    }
                }
            }
        }

        /**
         * Overwrites the SSH key
         */
        try {
            SSHKeyStore store = new SSHKeyStore();
            store.addKey(ConfigurationService.CLUSTER_USER, sshKey);
        } catch(ClassNotFoundException e) {
            log.info("cannot store cluster ssh key on disk: " + e.getMessage());
            throw new GoogleComputeEngineException(e);
        } catch(IOException e) {
            log.info("cannot store cluster ssh key on disk: " + e.getMessage());
            throw new GoogleComputeEngineException(e);
        }

        /**
         * Create the puppetmaster node
         */
        String instanceName = ConfigurationService.getServerName(clusterName, ConfigurationService.NODE_NAME_PUPPET);
        tags = Arrays.asList(ConfigurationService.NODE_TAG_PUPPET, clusterName);
        if(!client.instanceExists(instanceName)) {
            String networkName = network.get(0);
            if(networkName.contains("/")) {
                networkName = networkName.substring(networkName.lastIndexOf("/") + 1);
            }
            try {
                client.createInstance(instanceName, machinePrefix.toString().concat("n1-standard-1"), network,
                        rootDiskSizeGb, sourceImage, null, tags, Arrays.asList(sshKey.getSSHPublicKey(ConfigurationService.CLUSTER_USER)),
                        ConfigurationService.generatePuppetMasterStartupScript(clusterName, networkName, processes, diskRaid, dataFileSystem), true);
            } catch(IOException e) {
                throw new GoogleComputeEngineException(e);
            }
        } else {
            log.info("instance [" + instanceName + "] already exists, not created");
        }
        ConfigurationService.setClusterName(clusterName);
        ConfigurationService.setClusterNodeProcesses(processes);

        /**
         * Create the config nodes
         */
        instanceName = ConfigurationService.getServerName(clusterName, ConfigurationService.NODE_NAME_CONF);
        tags = Arrays.asList(ConfigurationService.NODE_TAG_CONF, clusterName);
        for(Integer i = 1; i <= 3; i++) {
            StringBuilder instance_name = new StringBuilder();
            instance_name.append(instanceName);
            instance_name.append("-node-");
            instance_name.append(i);

            if(client.instanceExists(instance_name.toString())) {
                log.info("instance [" + instance_name.toString() + "] already exists, not created");
                continue;
            }
            client.createInstance(instance_name.toString(), machinePrefix.toString().concat("n1-standard-1"), network,
                    rootDiskSizeGb, sourceImage, null, tags, Arrays.asList(sshKey.getSSHPublicKey(ConfigurationService.CLUSTER_USER)),
                    startupScript.getAbsolutePath(), false);
        }

        /**
         * Put the cluster node shard creation task in a background thread
         */
        final String t_instanceName = ConfigurationService.getServerName(clusterName, ConfigurationService.NODE_NAME_SHARD);
        final List<String> t_tags = Arrays.asList(ConfigurationService.NODE_TAG_SHARD, clusterName);
        final String t_machineType = machineType;
        final String t_sourceImage = sourceImage;
        final String t_diskType = diskType;


        Runnable shardNodesCreation = () -> {
            /**
             * Create the shard nodes data disks
             */
            String lastDiskCreated = null;
            Map<String, Map<String, String>> instancesDataDisks = new HashMap<>();

            for(Integer i = 1; i <= shards; i++) {
                Map<String, String> dataDisks = new HashMap<>();
                StringBuilder instance_name = new StringBuilder();
                instance_name.append(t_instanceName);
                instance_name.append("-node-");
                instance_name.append(i);
                try {
                    for(Integer d = 1; d <= disksPerShard; d++) {
                        StringBuilder disk_name = new StringBuilder();
                        disk_name.append(instance_name.toString());
                        disk_name.append("-disk");
                        disk_name.append(d);
                        dataDisks.put(disk_name.toString(), client.createDisk(disk_name.toString(), t_diskType,
                                dataDiskSizeGb));
                        lastDiskCreated = disk_name.toString();
                    }
                    instancesDataDisks.put(instance_name.toString(), dataDisks);
                } catch(GoogleComputeEngineException e) {
                    log.error("cannot create data disks for [" + instance_name.toString() +
                            "], instance not created: " + e.getMessage());
                    continue;
                }
            }

            /**
             * Time to get all the disk resources ready to be attached
             */
            try {
                Thread.sleep(2000);
            } catch(InterruptedException e) {}

            /**
             * Create the shard nodes instances
             */
            try {
                try {
                    for(Map.Entry<String, Map<String, String>> instance : instancesDataDisks.entrySet()) {
                        if(client.instanceExists(instance.getKey())) {
                            log.info("instance [" + instance.getKey() + "] already exists, not created");
                            continue;
                        }

                        client.createInstance(instance.getKey(), t_machineType, network, rootDiskSizeGb, t_sourceImage,
                                instance.getValue(), t_tags, Arrays.asList(sshKey.getSSHPublicKey(ConfigurationService.CLUSTER_USER)),
                                startupScript.getAbsolutePath(), false);
                    }
                    log.info("Shard nodes creation finished");
                } catch(GoogleComputeEngineException e) {
                    log.error("Shard nodes creation error: " + e.getMessage());
                }
            } finally {
                startupScript.delete();
            }
        };

        Thread thread = new Thread(shardNodesCreation);
        thread.start();
    }

    public void deleteCluster() throws GoogleComputeEngineException {
        checkAuthentication();

        String clusterName = ConfigurationService.getClusterName();
        if(clusterName == null) {
            throw new GoogleComputeEngineException("no cluster previously created");
        }

        for(Instance i : client.getInstances(Arrays.asList(ConfigurationService.NODE_TAG_SHARD, clusterName))) {
            client.deleteInstance(i.getName());
        }
        for(Instance i : client.getInstances(Arrays.asList(ConfigurationService.NODE_TAG_CONF, clusterName))) {
            client.deleteInstance(i.getName());
        }
        for(Instance i : client.getInstances(Arrays.asList(ConfigurationService.NODE_TAG_PUPPET, clusterName))) {
            client.deleteInstance(i.getName());
        }

        ConfigurationService.setClusterName(null);
    }

    public void createTestNodes(Integer testNodes, String machineType, String sourceImage, Integer rootDiskSizeGb)
            throws GoogleComputeEngineException, GoogleCloudStorageException {
        List<String> tags;
        SSHKey sshKey = SSHKeyFactory.generateKey();
        StringBuilder machinePrefix = new StringBuilder();
        machinePrefix.append("https://www.googleapis.com/compute/v1/projects/");
        machinePrefix.append(client.getProjectId());
        machinePrefix.append("/zones/");
        machinePrefix.append(client.getZone());
        machinePrefix.append("/machineTypes/");

        String clusterName = ConfigurationService.getClusterName();
        if(clusterName == null) {
            throw new GoogleComputeEngineException("cluster is not created, you must create a cluster before to create the testing nodes");
        }

        /**
         * Get the cluster network
         */
        String clusterNetwork = getClusterNetwork();

        /**
         * Verify the machine type to get the proper Link
         */
        for(MachineType t : client.getMachineTypes()) {
            if(machineType != null && machineType.equals(t.getName())) {
                machineType = t.getSelfLink();
                break;
            }
        }

        /**
         * Verify the source image to get the proper Link
         */
        for(Image i : client.getImages()) {
            if(sourceImage != null && sourceImage.equals(i.getName())) {
                sourceImage = i.getSelfLink();
                break;
            }
        }

        /**
         * Overwrites the SSH key
         */
        try {
            SSHKeyStore store = new SSHKeyStore();
            store.addKey(ConfigurationService.TEST_USER, sshKey);
        } catch(ClassNotFoundException e) {
            log.info("cannot store cluster ssh key on disk: " + e.getMessage());
            throw new GoogleComputeEngineException(e);
        } catch(IOException e) {
            log.info("cannot store cluster ssh key on disk: " + e.getMessage());
            throw new GoogleComputeEngineException(e);
        }

        /**
         * Creates the jump server
         */
        String instanceName = ConfigurationService.getServerName(clusterName, ConfigurationService.NODE_NAME_TEST_JUMP);
        tags = Arrays.asList(ConfigurationService.NODE_TAG_TEST_JUMP, clusterName);
        if(!client.instanceExists(instanceName)) {
            String networkName = clusterNetwork;
            if(networkName.contains("/")) {
                networkName = networkName.substring(networkName.lastIndexOf("/") + 1);
            }
            File startupScript = ConfigurationService.getTestJumpNodeStartupScriptFile(clusterName, networkName);
            try {
                client.createInstance(instanceName, machinePrefix.toString().concat("n1-standard-1"), Arrays.asList(clusterNetwork),
                        rootDiskSizeGb, sourceImage, null, tags, Arrays.asList(sshKey.getSSHPublicKey(ConfigurationService.TEST_USER)),
                        startupScript.getAbsolutePath(), true);
            } finally {
                startupScript.delete();
            }
        } else {
            log.info("instance [" + instanceName + "] already exists, not created");
        }

        /**
         * Put the test node creation task in a background thread
         */
        final File t_startupScript = ConfigurationService.getTestNodeStartupScriptFile(clusterName);
        final String t_instanceName = ConfigurationService.getServerName(clusterName, ConfigurationService.NODE_NAME_TEST);
        final List<String> t_tags = Arrays.asList(ConfigurationService.NODE_TAG_TEST, clusterName);
        final String t_machineType = machineType;
        final String t_sourceImage = sourceImage;

        Runnable testNodesCreation = () -> {
            /**
             * Time to start and configure the jump server
             */
            try {
                Thread.sleep(10000);
            } catch(InterruptedException e) {}

            try {
                for(Integer i = 1; i <= testNodes; i++) {
                    StringBuilder instance_name = new StringBuilder();
                    instance_name.append(t_instanceName);
                    instance_name.append("-node-");
                    instance_name.append(i);

                    if(client.instanceExists(instance_name.toString())) {
                        log.info("instance [" + instance_name.toString() + "] already exists, not created");
                        continue;
                    }
                    client.createInstance(instance_name.toString(), t_machineType, Arrays.asList(clusterNetwork),
                        rootDiskSizeGb, t_sourceImage, null, t_tags, Arrays.asList(sshKey.getSSHPublicKey(
                        ConfigurationService.TEST_USER)), t_startupScript.getAbsolutePath(), false);
                }
                log.info("Test nodes creation finished");
            } catch(GoogleComputeEngineException e) {
                log.error("Test nodes creation error: " + e.getMessage());
            }
        };

        Thread thread = new Thread(testNodesCreation);
        thread.start();
    }

    public void deleteTestNodes() throws GoogleComputeEngineException {
        checkAuthentication();

        String clusterName = ConfigurationService.getClusterName();
        if(clusterName == null) {
            throw new GoogleComputeEngineException("no cluster previously created");
        }

        for(Instance i : client.getInstances(Arrays.asList(ConfigurationService.NODE_TAG_TEST, clusterName))) {
            client.deleteInstance(i.getName());
        }
        for(Instance i : client.getInstances(Arrays.asList(ConfigurationService.NODE_TAG_TEST_JUMP, clusterName))) {
            client.deleteInstance(i.getName());
        }
    }
}