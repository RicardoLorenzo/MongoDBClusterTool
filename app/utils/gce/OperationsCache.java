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

import com.google.api.services.compute.model.Operation;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ricardolorenzo on 24/07/2014.
 */
public class OperationsCache {
    protected static Map<String, Operation> lastRunningOperationsCache;

    static {
        lastRunningOperationsCache = new HashMap<>();
    }

    public static void addOperation(Operation o) {
        if(!"DONE".equals(o.getStatus())) {
            lastRunningOperationsCache.put(o.getName(), o);
        }
    }

    public static boolean statusHasChanged(Operation o) {
        if(lastRunningOperationsCache.containsKey(o.getName())) {
            Operation c = lastRunningOperationsCache.get(o.getName());
            if(!o.getStatus().equals(c.getStatus())) {
                lastRunningOperationsCache.remove(o.getName());
                return true;
            }
        }
        return false;
    }

    public static void cleanOldOperationsFromCache() {
        Calendar timeout = Calendar.getInstance();
        timeout.add(Calendar.HOUR_OF_DAY, -24);
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        for(Operation o : lastRunningOperationsCache.values()) {
            try {
                Date date;
                if(o.getStartTime() != null &&
                        !o.getStartTime().isEmpty()) {
                    date = format.parse(o.getStartTime());
                } else {
                    date = format.parse(o.getInsertTime());
                }
                if(date.before(timeout.getTime())) {
                    lastRunningOperationsCache.remove(o.getName());
                }
            } catch(ParseException e) {
                lastRunningOperationsCache.remove(o.getName());
            }
        }
    }
}
