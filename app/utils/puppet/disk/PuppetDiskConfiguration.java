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

package utils.puppet.disk;

/**
 * Created by ricardolorenzo on 22/07/2014.
 */
public class PuppetDiskConfiguration {
    private String name;
    private String description;
    private String puppetClass;

    public PuppetDiskConfiguration(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getPuppetClass() {
        return puppetClass != null ? puppetClass : "";
    }

    public void setPuppetClass(String puppetClass) {
        this.puppetClass = puppetClass;
    }

    public String getName() {
        return name != null ? name : "";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if(o != null) {
            if(o.getClass().isInstance(this.getClass())) {
                PuppetDiskConfiguration dc = this.getClass().cast(o);
                if(name != null && name.equals(dc.getName())) {
                    return true;
                }
            }
        }
        return false;
    }
}
