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
import org.springframework.context.annotation.Scope;
import services.MeasurementService;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * An actor that can count using an injected CountingService.
 *
 * @note The scope here is prototype since we want to create a new actor
 * instance for use of this bean.
 */
@Named("MeasureActor")
@Scope("prototype")
class MeasureActor extends UntypedActor {

    // the service that will be automatically injected
    final MeasurementService measurementService;
    private int count = 0;

    @Inject
    public MeasureActor(@Named("MeasurementService") MeasurementService measurementService) {
        this.measurementService = measurementService;
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if(message instanceof Count) {
            count = measurementService.increment(count);
        } else if(message instanceof Get) {
            getSender().tell(count, getSelf());
        } else {
            unhandled(message);
        }
    }

    public static class Count {
    }

    public static class Get {
    }
}
