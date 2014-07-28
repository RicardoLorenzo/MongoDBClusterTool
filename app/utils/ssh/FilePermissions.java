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

package utils.ssh;

/**
 * Created by ricardolorenzo on 27/07/2014.
 */
public class FilePermissions {
    public static final Integer EXEC = 1;
    public static final Integer WRITE = 2;
    public static final Integer READ = 4;
    private Integer user;
    private Integer group;
    private Integer all;

    public FilePermissions(Integer user, Integer group, Integer all) {
        setUserPermission(user);
        setGroupPermission(group);
        setAllPermission(all);
    }

    private static void validatePermission(Integer permission) throws IllegalArgumentException {
        if(permission < 0 || permission > 7) {
            throw new IllegalArgumentException("incorrect permission value");
        }
    }

    public Integer getUserPermission() {
        return user;
    }

    public void setUserPermission(Integer user) {
        validatePermission(user);
        this.user = user;
    }

    public Integer getGroupPermission() {
        return group;
    }

    public void setGroupPermission(Integer group) {
        validatePermission(group);
        this.group = group;
    }

    public Integer getAllPermission() {
        return all;
    }

    public void setAllPermission(Integer all) {
        validatePermission(all);
        this.all = all;
    }
}
