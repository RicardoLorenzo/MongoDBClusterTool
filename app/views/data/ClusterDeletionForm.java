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

import java.util.List;

/**
 * Created by ricardolorenzo on 21/07/2014.
 */
public class ClusterDeletionForm {
    private String delete = "delete";

    /**
     * Required for form instantiation.
     */
    public ClusterDeletionForm() {
    }

    public ClusterDeletionForm(String delete) {
        this.delete = delete;
    }

    public String getDelete() {
        return delete;
    }

    public void setDelete(String delete) {
        this.delete = delete;
    }

    public List<ValidationError> validate() {
        return null;
    }
}
