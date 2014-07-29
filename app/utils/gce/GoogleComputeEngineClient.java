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

package utils.gce;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.*;
import conf.PlayConfiguration;
import play.libs.Json;
import utils.CappedList;
import utils.gce.auth.GoogleComputeEngineAuth;
import utils.security.GoogleComputeEngineFingerprint;

import java.io.IOException;
import java.util.*;

/**
 * Created by ricardolorenzo on 18/07/2014.
 */
public class GoogleComputeEngineClient {
    private static final String APPLICATION_NAME = "Ricardo Lorenzo GCE Client/1.0";
    private static final Long OPERATIONS_PER_PAGE = 50L;
    private static final JsonFactory JSON_FACTORY;
    private static final HttpTransport HTTP_TRANSPORT;
    private static final List<String> IMAGE_PROJECTS;
    private static final String projectId;
    private static Optional<String> zoneName;
    private Compute compute;
    private GoogleComputeEngineAuth auth;

    static {
        JSON_FACTORY = new JacksonFactory();
        HTTP_TRANSPORT = new NetHttpTransport();
        //IMAGE_PROJECTS = Arrays.asList(new String[]{ "debian-cloud", "centos-cloud" });
        IMAGE_PROJECTS = Arrays.asList(new String[] { "debian-cloud" });
        projectId = PlayConfiguration.getProperty("google.projectId");
        if(PlayConfiguration.hasProperty("google.zoneName")) {
            zoneName = Optional.ofNullable(PlayConfiguration.getProperty("google.zoneName"));
        }
    }

    public GoogleComputeEngineClient(final GoogleComputeEngineAuth auth) {
        this.auth = auth;
        compute = new Compute.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, null).setApplicationName(APPLICATION_NAME)
                .setHttpRequestInitializer(auth.getCredential()).build();
    }

    public String getProjectId() {
        return projectId;
    }

    public String getZone() {
        return zoneName.get();
    }

    public void setZone(String zoneName) {
        GoogleComputeEngineClient.zoneName = Optional.ofNullable(zoneName);
    }

    public List<Disk> getDisks() throws GoogleComputeEngineException {
        if(!zoneName.isPresent()) {
            throw new GoogleComputeEngineException("zone name not specified");
        }

        try {
            Compute.Disks.List list = compute.disks().list(projectId, zoneName.get());
            return list.execute().getItems();
        } catch(IOException e) {
            throw new GoogleComputeEngineException(e);
        }
    }

    public List<DiskType> getDiskTypes() throws GoogleComputeEngineException {
        if(!zoneName.isPresent()) {
            throw new GoogleComputeEngineException("zone name not specified");
        }

        try {
            Compute.DiskTypes.List list = compute.diskTypes().list(projectId, zoneName.get());
            return list.execute().getItems();
        } catch(IOException e) {
            throw new GoogleComputeEngineException(e);
        }
    }

    public List<Instance> getInstances(List<String> tags) throws GoogleComputeEngineException {
        if(!zoneName.isPresent()) {
            throw new GoogleComputeEngineException("zone name not specified");
        }

        try {
            Compute.Instances.List list = compute.instances().list(projectId, zoneName.get());
            if(tags != null && !tags.isEmpty()) {
                List<Instance> instances = new ArrayList<>();
                for(Instance i : list.execute().getItems()) {
                    int tag_match = 0;
                    Optional<List<String>> tagsItems = Optional.ofNullable(i.getTags()!=null?i.getTags().getItems():null);
                    for(String tag : tags) {
                        if(tagsItems.isPresent() && tagsItems.get().contains(tag)) {
                            tag_match++;
                        }
                    }
                    if(tag_match == tags.size()) {
                        instances.add(i);
                    }
                }
                return instances;
            } else {
                return list.execute().getItems();
            }
        } catch(IOException e) {
            throw new GoogleComputeEngineException(e);
        }
    }

    public List<Zone> getZones() throws GoogleComputeEngineException {
        try {
            Compute.Zones.List list = compute.zones().list(projectId);
            return list.execute().getItems();
        } catch(IOException e) {
            throw new GoogleComputeEngineException(e);
        }
    }

    public List<Image> getImages() throws GoogleComputeEngineException {
        try {
            List<Image> images = new ArrayList<>();
            for(String project : IMAGE_PROJECTS) {
                Compute.Images.List list = compute.images().list(project);
                List<Image> res_images = list.execute().getItems();
                if(res_images != null) {
                    images.addAll(res_images);
                }
            }
            return images;
        } catch(IOException e) {
            throw new GoogleComputeEngineException(e);
        }
    }

    public List<MachineType> getMachineTypes() throws GoogleComputeEngineException {
        if(!zoneName.isPresent()) {
            throw new GoogleComputeEngineException("zone name not specified");
        }

        try {
            Compute.MachineTypes.List list = compute.machineTypes().list(projectId, zoneName.get());
            return list.execute().getItems();
        } catch(IOException e) {
            throw new GoogleComputeEngineException(e);
        }
    }

    public List<Network> getNetworks() throws GoogleComputeEngineException {
        try {
            Compute.Networks.List list = compute.networks().list(projectId);
            return list.execute().getItems();
        } catch(IOException e) {
            throw new GoogleComputeEngineException(e);
        }
    }

    public List<Operation> getOperations() throws GoogleComputeEngineException {
        try {
            List<Operation> operations = getLastOperationsPage();
            return operations;
        } catch(IOException e) {
            throw new GoogleComputeEngineException(e);
        }
    }

    private List<Operation> getLastOperationsPage() throws IOException, GoogleComputeEngineException {
        if(!zoneName.isPresent()) {
            throw new GoogleComputeEngineException("zone name not specified");
        }

        Compute.ZoneOperations.List list = compute.zoneOperations().list(projectId, zoneName.get());
        /**
         * TODO get the maximum from the configuration
         */
        list.setMaxResults(OPERATIONS_PER_PAGE);

        CappedList<Operation> operations = new CappedList<>(OPERATIONS_PER_PAGE.intValue());
        OperationList pageList = list.execute();
        operations.addAll(pageList.getItems());
        while(pageList.getNextPageToken() != null) {
            list.setPageToken(pageList.getNextPageToken());
            pageList = list.execute();
            operations.addAll(pageList.getItems());
        }
        return new ArrayList<>(operations);
    }

    public Disk getDisk(String diskName) throws GoogleComputeEngineException {
        if(!zoneName.isPresent()) {
            throw new GoogleComputeEngineException("zone name not specified");
        }

        try {
            Compute.Disks.Get get = compute.disks().get(projectId, zoneName.get(), diskName);
            return get.execute();
        } catch(IOException e) {
            throw new GoogleComputeEngineException(e);
        }

    }

    public void attachDiskToInstance(String instanceName, String diskName) throws GoogleComputeEngineException {
        if(!zoneName.isPresent()) {
            throw new GoogleComputeEngineException("zone name not specified");
        }

        Disk disk = getDisk(diskName);
        AttachedDisk attachedDisk = new AttachedDisk();
        attachedDisk.setBoot(false);
        attachedDisk.setAutoDelete(true);
        attachedDisk.setDeviceName(diskName);
        attachedDisk.setMode("READ_WRITE");
        attachedDisk.setSource(disk.getSelfLink());

        try {
            Compute.Instances.AttachDisk attachDisk = compute.instances().attachDisk(projectId, zoneName.get(), instanceName, attachedDisk);
            attachDisk.execute();
        } catch(IOException e) {
            throw new GoogleComputeEngineException(e);
        }
    }

    public String createDisk(String name, String diskType, Integer sizeGb) throws GoogleComputeEngineException {
        if(!zoneName.isPresent()) {
            throw new GoogleComputeEngineException("zone name not specified");
        }

        try {
            Disk disk = new Disk();
            disk.setName(name);
            disk.setZone(zoneName.get());
            disk.setSizeGb(sizeGb.longValue());
            disk.setType(diskType);

            Compute.Disks.Insert insert = compute.disks().insert(projectId, zoneName.get(), disk);
            return insert.execute().getTargetLink();
        } catch(IOException e) {
            throw new GoogleComputeEngineException(e);
        }
    }

    public void deleteDisk(String diskName) throws GoogleComputeEngineException {
        if(!zoneName.isPresent()) {
            throw new GoogleComputeEngineException("zone name not specified");
        }

        try {
            Compute.Disks.Delete delete = compute.disks().delete(projectId, zoneName.get(), diskName);
            delete.execute();
        } catch(IOException e) {
            throw new GoogleComputeEngineException(e);
        }
    }

    private static AttachedDisk createAttachedDisk(String name, Integer sizeGb, String diskType, String sourceImage) {
        AttachedDiskInitializeParams paramaters = new AttachedDiskInitializeParams();
        paramaters.setDiskName(name);
        paramaters.setDiskSizeGb(sizeGb.longValue());
        if(diskType != null && !diskType.isEmpty()) {
            paramaters.setDiskType(diskType);
        }
        if(sourceImage != null && !sourceImage.isEmpty()) {
            paramaters.setSourceImage(sourceImage);
        }

        AttachedDisk disk = new AttachedDisk();
        disk.setBoot(true);
        //disk.setType("PERSISTENT");
        disk.setMode("READ_WRITE");
        disk.setAutoDelete(true);
        disk.setInitializeParams(paramaters);

        return disk;
    }

    public void setInstanceTags(String instanceName, Map<String, String> tags) throws GoogleComputeEngineException {
        if(!zoneName.isPresent()) {
            throw new GoogleComputeEngineException("zone name not specified");
        }

        Tags t = new Tags();
        if(tags != null && !tags.isEmpty()) {
            for(Map.Entry<String, String> e : tags.entrySet()) {
                t.set(e.getKey(), e.getValue());
            }
        }
        t = t.encodeFingerprint(GoogleComputeEngineFingerprint.getSha1Hash(t.toString()));

        try {
            Compute.Instances.SetTags setTags = compute.instances().setTags(projectId, zoneName.get(), instanceName, t);
            setTags.execute();
        } catch(IOException e) {
            throw new GoogleComputeEngineException(e);
        }
    }

    public void setInstanceMetaData(String instanceName, Map<String, String> metadata) throws GoogleComputeEngineException {
        if(!zoneName.isPresent()) {
            throw new GoogleComputeEngineException("zone name not specified");
        }

        Metadata m = new Metadata();
        if(metadata != null && !metadata.isEmpty()) {
            for(Map.Entry<String, String> e : metadata.entrySet()) {
                m.set(e.getKey(), e.getValue());
            }
        }
        m = m.encodeFingerprint(GoogleComputeEngineFingerprint.getSha1Hash(m.toString()));

        try {
            Compute.Instances.SetMetadata setMetadata = compute.instances().setMetadata(projectId, zoneName.get(), instanceName, m);
            setMetadata.execute();
        } catch(IOException e) {
            throw new GoogleComputeEngineException(e);
        }
    }

    public Network getNetwork(String networkName) throws GoogleComputeEngineException {
        try {
            Compute.Networks.Get get = compute.networks().get(projectId, networkName);
            return get.execute();
        } catch(IOException e) {
            throw new GoogleComputeEngineException(e);
        }
    }

    public String createNetwork(String name, String description, String ip4Range, String ip4Gateway) throws GoogleComputeEngineException {
        Network net = new Network();
        net.setName(name);
        if(Optional.ofNullable(description).isPresent()) {
            net.setDescription(description);
        }
        net.setIPv4Range(ip4Range);
        if(Optional.ofNullable(ip4Gateway).isPresent()) {
            net.setGatewayIPv4(ip4Gateway);
        }
        try {
            Compute.Networks.Insert insert = compute.networks().insert(projectId, net);
            return insert.execute().getTargetLink();
        } catch(IOException e) {
            throw new GoogleComputeEngineException(e);
        }
    }

    public void deleteNetwork(String networkName) throws GoogleComputeEngineException {
        try {
            Compute.Networks.Delete delete = compute.networks().delete(projectId, networkName);
            delete.execute();
        } catch(IOException e) {
            throw new GoogleComputeEngineException(e);
        }
    }

    public Instance getInstance(String instanceName) throws GoogleComputeEngineException {
        if(!zoneName.isPresent()) {
            throw new GoogleComputeEngineException("zone name not specified");
        }

        try {
            Compute.Instances.Get get = compute.instances().get(projectId, zoneName.get(), instanceName);
            return get.execute();
        } catch(IOException e) {
            String message = e.getMessage();
            if(message != null && message.contains("{") && message.contains("}")) {
                message = message.substring(message.indexOf("{"));
                message = message.substring(0, message.lastIndexOf("}") + 1);
                JsonNode node = Json.parse(message).get("code");
                if(node != null && node.asInt() == 404) {
                    return null;
                }
            }
            throw new GoogleComputeEngineException(e);
        }
    }

    public boolean instanceExists(String instanceName) throws GoogleComputeEngineException {
        if(!zoneName.isPresent()) {
            throw new GoogleComputeEngineException("zone name not specified");
        }

        try {
            Compute.Instances.Get get = compute.instances().get(projectId, zoneName.get(), instanceName);
            if(get.execute() != null) {
                return true;
            }
            return false;
        } catch(IOException e) {
            String message = e.getMessage();
            if(message != null && message.contains("{") && message.contains("}")) {
                message = message.substring(message.indexOf("{"));
                message = message.substring(0, message.lastIndexOf("}") + 1);
                JsonNode node = Json.parse(message).get("code");
                if(node != null && node.asInt() == 404) {
                    return false;
                }
            }
            throw new GoogleComputeEngineException(e);
        }
    }

    public String createInstance(String name, String machineType, List<String> network, Integer sizeGb, String sourceImage,
                               Map<String, String> dataDisks, List<String> tags, List<String> sshKeys, String startupScriptUrl,
                               boolean publicAddress)
            throws GoogleComputeEngineException {
        if(!zoneName.isPresent()) {
            throw new GoogleComputeEngineException("zone name not specified");
        }

        try {
            Instance instance = new Instance();
            instance.setFactory(JSON_FACTORY);
            instance.setMachineType(machineType);
            instance.setName(name);
            instance.setZone(zoneName.get());
            instance.setCanIpForward(publicAddress);
            instance.setServiceAccounts(auth.getInstanceServiceAccounts());
            if(network != null && !network.isEmpty()) {
                int count = 0;
                List<NetworkInterface> networkInterfaces = new ArrayList<>();
                for(String n : network) {
                    NetworkInterface iface = new NetworkInterface();
                    iface.setFactory(JSON_FACTORY);
                    iface.setName("eth" + count);
                    iface.setNetwork(n);
                    if(publicAddress) {
                        AccessConfig ac = new AccessConfig();
                        ac.setType("ONE_TO_ONE_NAT");
                        iface.setAccessConfigs(Arrays.asList(ac));
                    }
                    networkInterfaces.add(iface);
                    count++;
                }
                instance.setNetworkInterfaces(networkInterfaces);
            }
            List<AttachedDisk> attachedDisks = new ArrayList<>();
            attachedDisks.add(createAttachedDisk(name + "-disk-root", sizeGb, null, sourceImage));
            if(dataDisks != null && !dataDisks.isEmpty()) {
                for(Map.Entry<String, String> dataDisk : dataDisks.entrySet()) {
                    AttachedDisk attachedDisk = new AttachedDisk();
                    attachedDisk.setBoot(false);
                    attachedDisk.setAutoDelete(true);
                    attachedDisk.setDeviceName(dataDisk.getKey());
                    attachedDisk.setMode("READ_WRITE");
                    attachedDisk.setSource(dataDisk.getValue());
                    attachedDisks.add(attachedDisk);
                }
            }
            instance.setDisks(attachedDisks);
            if(tags != null && !tags.isEmpty()) {
                Tags t = new Tags();
                t.setItems(tags);
                instance.setTags(t.encodeFingerprint(GoogleComputeEngineFingerprint.getSha1Hash(t.toString())));
            }
            if(Optional.ofNullable(startupScriptUrl).isPresent() ||
                    (sshKeys != null && !sshKeys.isEmpty())) {
                Metadata m = new Metadata();
                List<Metadata.Items> itemList = new ArrayList<>();
                if(Optional.ofNullable(startupScriptUrl).isPresent()) {
                    Metadata.Items items = new Metadata.Items();
                    items.setKey("startup-script-url");
                    items.setValue(startupScriptUrl);
                    itemList.add(items);
                }
                if(sshKeys != null && !sshKeys.isEmpty()) {
                    for(String key : sshKeys) {
                        Metadata.Items items = new Metadata.Items();
                        items.setKey("sshKeys");
                        items.setValue(key);
                        itemList.add(items);
                    }
                }
                m.setItems(itemList);
                instance.setMetadata(m.encodeFingerprint(GoogleComputeEngineFingerprint.getSha1Hash(m.toString())));
            }

            Compute.Instances.Insert insert = compute.instances().insert(projectId, zoneName.get(), instance);
            return insert.execute().getTargetLink();
        } catch(IOException e) {
            throw new GoogleComputeEngineException(e);
        }
    }

    public void deleteInstance(String instanceName) throws GoogleComputeEngineException {
        if(!zoneName.isPresent()) {
            throw new GoogleComputeEngineException("zone name not specified");
        }

        try {
            Compute.Instances.Delete delete = compute.instances().delete(projectId, zoneName.get(), instanceName);
            delete.execute();
        } catch(IOException e) {
            throw new GoogleComputeEngineException(e);
        }
    }
}
