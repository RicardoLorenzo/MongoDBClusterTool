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
import com.google.api.services.compute.model.Operation;
import org.springframework.beans.factory.annotation.Qualifier;
import play.libs.Json;
import play.mvc.WebSocket;
import services.GoogleComputeEngineService;

import javax.inject.Inject;

/**
 * Created by ricardolorenzo on 18/07/2014.
 */
public class GoogleComputeEngineConnection extends UntypedActor {
    private static GoogleComputeEngineService googleService;
    private final WebSocket.Out<JsonNode> out;
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public GoogleComputeEngineConnection(WebSocket.Out<JsonNode> out) {
        this.out = out;
    }

    @Inject
    void setGoogleService(@Qualifier("gce-service") GoogleComputeEngineService googleService) {
        GoogleComputeEngineConnection.googleService = googleService;
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if(message instanceof Operation) {
            Operation operation = Operation.class.cast(message);
            ObjectNode updateOperation = Json.newObject();
            updateOperation.put("type", "operation");
            updateOperation.put("name", operation.getName());
            updateOperation.put("date", operation.getInsertTime());
            updateOperation.put("start", operation.getStartTime());
            updateOperation.put("end", operation.getEndTime());
            updateOperation.put("progress", operation.getProgress());
            updateOperation.put("status", operation.getStatus());
            updateOperation.put("user", operation.getUser());
            updateOperation.put("optype", operation.getOperationType());
            String target = operation.getTargetLink();
            if(target != null && target.contains("/")) {
                target = target.substring(target.lastIndexOf("/") + 1);
            }
            updateOperation.put("target", target);

            //log.info("Received operation: " + operation.getName(), updateOperation);
            out.write(updateOperation);
        } else {
            unhandled(message);
        }
    }
}
