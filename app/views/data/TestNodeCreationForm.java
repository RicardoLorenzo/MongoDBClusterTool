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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ricardolorenzo on 21/07/2014.
 */
public class TestNodeCreationForm {
    private Integer testNodes;
    private String machineType;
    private String image;
    private Integer rootDiskSizeGb;

    /**
     * Required for form instantiation.
     */
    public TestNodeCreationForm() {
    }

    public TestNodeCreationForm(Integer testNodes, String machineType, String image, Integer rootDiskSizeGb) {
        this.testNodes = testNodes;
        this.machineType = machineType;
        this.image = image;
        this.rootDiskSizeGb = rootDiskSizeGb;
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

    public Integer getTestNodes() {
        return testNodes;
    }

    public void setTestNodes(Integer testNodes) {
        this.testNodes = testNodes;
    }

    public Integer getRootDiskSizeGb() {
        return rootDiskSizeGb;
    }

    public void setRootDiskSizeGb(Integer rootDiskSizeGb) {
        this.rootDiskSizeGb = rootDiskSizeGb;
    }

    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList<>();
        if(testNodes == null || testNodes < 1) {
            errors.add(new ValidationError("testNodes", "Incorrect number of shards"));
        }
        if(machineType == null || machineType.isEmpty()) {
            errors.add(new ValidationError("machineType", "You must choose the machine type"));
        }
        if(image == null || image.isEmpty()) {
            errors.add(new ValidationError("image", "You must choose an OS image"));
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
