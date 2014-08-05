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

package actors;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.libs.Json;
import play.mvc.WebSocket;
import utils.test.data.Measure;

/**
 * Created by ricardolorenzo on 18/07/2014.
 */
public class TestMeasureConnection extends UntypedActor {
    private final WebSocket.Out<JsonNode> out;
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public TestMeasureConnection(WebSocket.Out<JsonNode> out) {
        this.out = out;
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if(message instanceof Measure) {
            Measure measure = Measure.class.cast(message);
            ObjectNode measureData = Json.newObject();
            measureData.put("type", "measure");
            measureData.put("name", measure.getNodeAddress());
            measureData.put("insertcount", measure.getTotalOperationsByType(Measure.INSERT));
            measureData.put("updatecount", measure.getTotalOperationsByType(Measure.UPDATE));
            measureData.put("deletecount", measure.getTotalOperationsByType(Measure.DELETE));
            measureData.put("scancount", measure.getTotalOperationsByType(Measure.SCAN));
            measureData.put("readcount", measure.getTotalOperationsByType(Measure.READ));
            measureData.put("insertaverage", measure.getAverageLatencyByType(Measure.INSERT));
            measureData.put("updateaverage", measure.getAverageLatencyByType(Measure.UPDATE));
            measureData.put("deleteaverage", measure.getAverageLatencyByType(Measure.DELETE));
            measureData.put("scanaverage", measure.getAverageLatencyByType(Measure.SCAN));
            measureData.put("readaverage", measure.getAverageLatencyByType(Measure.READ));
            measureData.put("time", measure.getTime());
            measureData.put("time-unit", measure.getTimeUnit().toString());
            //log.info("Received measure: " + measure.getNodeAddress(), measureData);
            out.write(measureData);
        } else {
            unhandled(message);
        }
    }
}
