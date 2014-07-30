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

package views.data;

import play.data.validation.ValidationError;
import utils.puppet.PuppetConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ricardolorenzo on 21/07/2014.
 */
public class ClusterCreationForm {
    private String clusterName;
    private Integer shardNodes;
    private Integer processes;
    private Integer nodeDisks;
    private Boolean cgroups = true;
    private String machineType;
    private String image;
    private String network;
    private String dataDiskType;
    private String dataDiskRaid;
    private Integer dataDiskSizeGb;
    private Integer rootDiskSizeGb;
    private String fileSystem;

    /**
     * Required for form instantiation.
     */
    public ClusterCreationForm() {
    }

    public ClusterCreationForm(String clusterName, Integer shardNodes, Integer processes, Integer nodeDisks,
                               String machineType, String image, String network, String dataDiskType,
                               String dataDiskRaid, String fileSystem, Integer dataDiskSizeGb, Integer rootDiskSizeGb,
                               Boolean cgroups) {
        this.clusterName = clusterName;
        this.shardNodes = shardNodes;
        this.processes = processes;
        this.nodeDisks = nodeDisks;
        this.machineType = machineType;
        this.image = image;
        this.network = network;
        this.dataDiskType = dataDiskType;
        this.dataDiskRaid = dataDiskRaid;
        this.fileSystem = fileSystem;
        this.dataDiskSizeGb = dataDiskSizeGb;
        this.rootDiskSizeGb = rootDiskSizeGb;
        this.cgroups = cgroups;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getFileSystem() {
        return fileSystem;
    }

    public void setFileSystem(String fileSystem) {
        this.fileSystem = fileSystem;
    }

    public String getDataDiskType() {
        return dataDiskType;
    }

    public String getDiskRaid() {
        return dataDiskRaid;
    }

    public void setDiskRaid(String dataDiskRaid) {
        this.dataDiskRaid = dataDiskRaid;
    }

    public void setDiskType(String dataDiskType) {
        this.dataDiskType = dataDiskType;
    }

    public String getMachineType() {
        return machineType;
    }

    public void setMachineType(String machineType) {
        this.machineType = machineType;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public Boolean isCgroups() {
        return cgroups;
    }

    public void setCgroups(Boolean cgroups) {
        this.cgroups = cgroups;
    }

    public Integer getProcesses() {
        return processes;
    }

    public void setProcesses(Integer processes) {
        this.processes = processes;
    }

    public Integer getNodeDisks() {
        return nodeDisks;
    }

    public void setNodeDisks(Integer node_disks) {
        this.nodeDisks = node_disks;
    }

    public Integer getShardNodes() {
        return shardNodes;
    }

    public void setShardNodes(Integer shard_nodes) {
        this.shardNodes = shard_nodes;
    }

    public Integer getDataDiskSizeGb() {
        return dataDiskSizeGb;
    }

    public void setDataDiskSizeGb(Integer dataDiskSizeGb) {
        this.dataDiskSizeGb = dataDiskSizeGb;
    }

    public Integer getRootDiskSizeGb() {
        return rootDiskSizeGb;
    }

    public void setRootDiskSizeGb(Integer rootDiskSizeGb) {
        this.rootDiskSizeGb = rootDiskSizeGb;
    }

    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList<>();
        if(clusterName == null || clusterName.isEmpty()) {
            errors.add(new ValidationError("clusterName", "You must specify a name"));
        }
        if(shardNodes == null || shardNodes < 2) {
            errors.add(new ValidationError("shardNodes", "Incorrect number of shards"));
        }
        if(processes == null || processes <= 0) {
            errors.add(new ValidationError("processes", "Incorrect number of processes"));
        }
        if(nodeDisks == null || nodeDisks <= 0) {
            errors.add(new ValidationError("nodeDisks", "Incorrect number of disks per node"));
        }
        if(machineType == null || machineType.isEmpty()) {
            errors.add(new ValidationError("machineType", "You must choose the machine type"));
        }
        if(image == null || image.isEmpty()) {
            errors.add(new ValidationError("image", "You must choose an OS image"));
        }
        if(network == null || network.isEmpty()) {
            errors.add(new ValidationError("network", "You must choose a network"));
        }
        if(dataDiskRaid == null || dataDiskRaid.isEmpty()) {
            errors.add(new ValidationError("dataDiskType", "You must choose the disk type"));
        } else if(dataDiskType.equals(PuppetConfiguration.DISK_PER_PROCESS.getName())) {
            if(processes != nodeDisks) {
                errors.add(new ValidationError("dataDiskType", "The number of processes must match the number of disks for standalone disks mode"));
            }
        }
        if(fileSystem == null || fileSystem.isEmpty() || !PuppetConfiguration.SUPPORTED_FILESYSTEMS.contains(fileSystem)) {
            errors.add(new ValidationError("fileSystem", "You must choose the data filesystem type"));
        }
        if(dataDiskSizeGb == null || dataDiskSizeGb <= 0) {
            errors.add(new ValidationError("dataDiskSizeGb", "Incorrect data disk size"));
        }
        if(rootDiskSizeGb == null || rootDiskSizeGb <= 0) {
            errors.add(new ValidationError("rootDiskSizeGb", "Incorrect OS disk size"));
        }

        if(errors.size() > 0) {
            return errors;
        }
        return null;
    }
}
