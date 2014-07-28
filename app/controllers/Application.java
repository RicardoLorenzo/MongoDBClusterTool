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

package controllers;

import play.libs.EventSource;
import play.libs.F;
import play.libs.WS;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import scala.Option;

import java.util.concurrent.TimeUnit;

@org.springframework.stereotype.Controller
public class Application extends Controller {

    public static Result index() {
        return ok(views.html.index.render(request().path(), Option.empty()));
    }

    public static Result syncFoo() {
        return ok("sync foo");
    }

    public static F.Promise<Result> asyncFoo() {
        return F.Promise.promise(() -> ok("async foo"));
    }

    public static F.Promise<Result> asyncNonBlockingFoo() {
        return F.Promise.delayed(() -> ok("async non-blocking foo"), 5, TimeUnit.SECONDS);
    }

    public static F.Promise<Result> reactiveRequest() {
        F.Promise<WS.Response> typesafePromise = WS.url("http://www.typesafe.com").get();
        return typesafePromise.map(response -> ok(response.getBody()));
    }

    public static F.Promise<Result> reactiveComposition() {
        final F.Promise<WS.Response> twitterPromise = WS.url("http://www.twitter.com").get();
        final F.Promise<WS.Response> typesafePromise = WS.url("http://www.typesafe.com").get();

        return twitterPromise.flatMap((twitter) ->
                typesafePromise.map((typesafe) ->
                        ok(twitter.getBody() + typesafe.getBody())));
    }

    public static Result events() {
        EventSource eventSource = new EventSource() {
            public void onConnected() {
                sendData("hello");
            }
        };
        return ok(eventSource);
    }

    public static WebSocket<String> echo() {
        return new WebSocket<String>() {
            public void onReady(final In<String> in, final Out<String> out) {
                in.onMessage(out::write);
            }
        };
    }
}
