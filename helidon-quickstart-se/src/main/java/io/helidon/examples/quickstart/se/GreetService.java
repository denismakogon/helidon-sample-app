/*
 * Copyright (c) 2018, 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.examples.quickstart.se;

import java.util.Collections;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import java.net.HttpURLConnection;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import io.helidon.common.http.Http;
import io.helidon.config.Config;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;


public class GreetService implements Service {

    /**
     * The config value for the key {@code greeting}.
     */
    private String greeting;

    private static final JsonBuilderFactory JSON = Json.createBuilderFactory(Collections.emptyMap());

    GreetService(Config config) {
        this.greeting = config.get("app.greeting").asString().orElse("Ciao");
    }

    /**
     * A service registers itself by updating the routine rules.
     * @param rules the routing rules.
     */
    @Override
    public void update(Routing.Rules rules) {
        rules
            .post("/fn/{name}", this::postCallFn)
            .post("/fn", this::postCallFn);
    }

    private void returnExceptionAsResponse(Exception e, ServerResponse response) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String sStackTrace = sw.toString();
        response.send(sw.toString());
    }

    private void postCallFn(ServerRequest request,
                            ServerResponse response) {
        try {
            String name = request.path().param("name");
            String url = new String(System.getenv("FN_INVOKE_URL"));
            HttpClient hs = new HttpClient();
            JsonObject payload = JSON.createObjectBuilder()
                    .add("name", name)
                    .build();

            String res = hs.post(url, payload.toString());
            response.send(res);
        }
        catch (Exception e) {
            this.returnExceptionAsResponse(e, response);
        }
    }

    /**
     * Return a greeting message using the name that was provided.
     * @param request the server request
     * @param response the server response
     */
    private void getMessageHandler(ServerRequest request,
                            ServerResponse response) {
        String name = request.path().param("name");
        sendResponse(response, name);
    }

    private void sendResponse(ServerResponse response, String name) {
        String msg = String.format("%s %s!", greeting, name);

        JsonObject returnObject = JSON.createObjectBuilder()
                .add("message", msg)
                .build();
        response.send(returnObject);
    }

    private void bypassToFn(JsonObject payload, ServerResponse response) {
        try {
            String url = new String(System.getenv("FN_INVOKE_URL"));
            HttpClient hs = new HttpClient();
            String res = hs.post(url, payload.toString());
            response.send(res);
        }
        catch (Exception e) {
            this.returnExceptionAsResponse(e, response);
        }
    }

    private void postBypassFn(ServerRequest request,
                                   ServerResponse response) {
        request.content().as(JsonObject.class).thenAccept(payload -> this.bypassToFn(payload, response));
    }

}
