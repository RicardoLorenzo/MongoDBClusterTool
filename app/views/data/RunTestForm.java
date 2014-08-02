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
public class RunTestForm {
    private String phase;
    private Integer threads;
    private Integer bulkCount;
    private Integer recordCount;
    private Integer operationCount;
    private Boolean readAllFields;
    private Float readProportion;
    private Float updateProportion;
    private Float scanProportion;
    private Float insertProportion;

    /**
     * Required for form instantiation.
     */
    public RunTestForm() {
    }

    public RunTestForm(String phase, Integer threads, Integer bulkCount, Integer recordCount, Integer operationCount,
                       Boolean readAllFields, Float readProportion, Float updateProportion, Float scanProportion,
                       Float insertProportion) {
        this.phase = phase;
        this.threads = threads;
        this.bulkCount = bulkCount;
        this.recordCount = recordCount;
        this.operationCount = operationCount;
        this.readAllFields = readAllFields;
        this.readProportion = readProportion;
        this.updateProportion = updateProportion;
        this.scanProportion = scanProportion;
        this.insertProportion = insertProportion;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public Integer getThreads() {
        return threads;
    }

    public void setThreads(Integer threads) {
        this.threads = threads;
    }

    public Integer getBulkCount() {
        return bulkCount;
    }

    public void setBulkCount(Integer bulkCount) {
        this.bulkCount = bulkCount;
    }

    public Integer getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(Integer recordCount) {
        this.recordCount = recordCount;
    }

    public Integer getOperationCount() {
        return operationCount;
    }

    public void setOperationCount(Integer operationCount) {
        this.operationCount = operationCount;
    }

    public Boolean getReadAllFields() {
        return readAllFields;
    }

    public void setReadAllFields(Boolean readAllFields) {
        this.readAllFields = readAllFields;
    }

    public Float getReadProportion() {
        return readProportion;
    }

    public void setReadProportion(Float readProportion) {
        this.readProportion = readProportion;
    }

    public Float getUpdateProportion() {
        return updateProportion;
    }

    public void setUpdateProportion(Float updateProportion) {
        this.updateProportion = updateProportion;
    }

    public Float getScanProportion() {
        return scanProportion;
    }

    public void setScanProportion(Float scanProportion) {
        this.scanProportion = scanProportion;
    }

    public Float getInsertProportion() {
        return insertProportion;
    }

    public void setInsertProportion(Float insertProportion) {
        this.insertProportion = insertProportion;
    }

    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList<>();
        if(phase == null || phase.isEmpty()) {
            errors.add(new ValidationError("phase", "Incorrect phase"));
        }
        if(threads == null || threads > 0) {
            errors.add(new ValidationError("threads", "Incorrect number of threads"));
        }
        if(bulkCount == null || bulkCount > 1) {
            errors.add(new ValidationError("bulkCount", "Bulk count must be greater than 1"));
        }
        if(recordCount == null || recordCount > 1) {
            errors.add(new ValidationError("recordCount", "Record count must be greater than 1"));
        }
        if(operationCount == null || operationCount > 1) {
            errors.add(new ValidationError("operationCount", "Operation count must be greater than 1"));
        }
        if(readProportion == null || readProportion > 1) {
            errors.add(new ValidationError("readProportion", "Read proportion must be a fraction of 1 or 0"));
        }
        if(updateProportion == null || updateProportion > 1) {
            errors.add(new ValidationError("updateProportion", "Update proportion must be a fraction of 1 or 0"));
        }
        if(scanProportion == null || scanProportion > 1) {
            errors.add(new ValidationError("scanProportion", "Scan proportion must be a fraction of 1 or 0"));
        }
        if(insertProportion == null || insertProportion > 1) {
            errors.add(new ValidationError("insertProportion", "Insert proportion must be a fraction of 1 or 0"));
        }

        if(errors.size() > 0) {
            return errors;
        }
        return null;
    }
}
