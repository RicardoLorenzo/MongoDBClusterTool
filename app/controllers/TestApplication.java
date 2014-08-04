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

import actors.TestMeasureConnection;
import akka.actor.ActorRef;
import akka.actor.Inbox;
import akka.actor.Props;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import play.data.Form;
import play.libs.Akka;
import play.libs.F;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import services.ConfigurationService;
import services.GoogleAuthenticationService;
import services.GoogleComputeEngineService;
import utils.gce.GoogleComputeEngineException;
import utils.gce.auth.GoogleComputeEngineAuthImpl;
import utils.play.BugWorkaroundForm;
import utils.security.SSHKey;
import utils.security.SSHKeyStore;
import utils.test.Test;
import utils.test.TestException;
import utils.test.TestRunner;
import utils.test.YCSBTestRunner;
import utils.test.data.Measure;
import utils.test.data.YCSBWorkload;
import views.data.RunTestForm;
import views.data.TestNodeCreationForm;
import views.data.TestNodeDeletionForm;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;

@org.springframework.stereotype.Controller
public class TestApplication extends Controller {
    private static TestRunner runner;
    private static GoogleAuthenticationService googleAuth;
    private static ConfigurationService configurationService;
    private static GoogleComputeEngineService googleService;

    @Inject
    void setTestRunner(@Qualifier("test-runner") TestRunner runner) {
        TestApplication.runner = runner;
    }

    @Inject
    void setGoogleService(@Qualifier("gauth-service") GoogleAuthenticationService googleAuth) {
        TestApplication.googleAuth = googleAuth;
    }

    @Inject
    void setGoogleService(@Qualifier("conf-service") ConfigurationService confService) {
        TestApplication.configurationService = confService;
    }

    @Inject
    void setGoogleService(@Qualifier("gce-service") GoogleComputeEngineService googleService) {
        TestApplication.googleService = googleService;
    }

    public static Result createTestNodes() {
        String callBackUrl = GoogleComputeEngineAuthImpl.getCallBackURL(request());
        try {
            boolean option_chosen;
            String result = GoogleAuthenticationService.authenticate(callBackUrl, null);
            if(result != null) {
                return redirect(result);
            }

            Map<String, Boolean> machineTypes = new TreeMap<>(Comparator.<String>naturalOrder()),
                    images = new TreeMap<>(Comparator.<String>reverseOrder());

            TestNodeCreationForm nodeCreation = new TestNodeCreationForm();
            Form<TestNodeCreationForm> formData = Form.form(TestNodeCreationForm.class).fill(nodeCreation);
            for(JsonNode n : GoogleComputeEngineService.listMachineTypes().findValues("name")) {
                machineTypes.put(n.textValue(), "n1-standard-4".equals(n.textValue()));
            }
            option_chosen = false;
            for(JsonNode n : GoogleComputeEngineService.listImages().findValues("name")) {
                if(!option_chosen && n.textValue().startsWith("debian-")) {
                    images.put(n.textValue(), true);
                    option_chosen = true;
                } else {
                    images.put(n.textValue(), false);
                }
            }
            option_chosen = false;
            return ok(views.html.test_nodes_creation.render(
                    formData,
                    machineTypes,
                    images
            ));
        } catch(GoogleComputeEngineException e) {
            return ok(views.html.error.render(e.getMessage()));
        }
    }

    public static Result createTestNodesPost() {
        String callBackUrl = GoogleComputeEngineAuthImpl.getCallBackURL(request());
        try {
            String result = GoogleAuthenticationService.authenticate(callBackUrl, null);
            if(result != null) {
                return redirect(result);
            }

            Map<String, Boolean> machineTypes = new TreeMap<>(Comparator.<String>naturalOrder()),
                    images = new TreeMap<>(Comparator.<String>reverseOrder());

            Form<TestNodeCreationForm> formData = new BugWorkaroundForm<>(TestNodeCreationForm.class).bindFromRequest();
            TestNodeCreationForm nodeCreationForm = formData.get();
            for(JsonNode n : GoogleComputeEngineService.listMachineTypes().findValues("name")) {
                machineTypes.put(n.textValue(), (nodeCreationForm.getMachineType() != null &&
                        n.textValue().equals(nodeCreationForm.getMachineType())));
            }
            for(JsonNode n : GoogleComputeEngineService.listImages().findValues("name")) {
                images.put(n.textValue(), (nodeCreationForm.getImage() != null &&
                        n.textValue().equals(nodeCreationForm.getImage())));
            }
            if(formData.hasErrors()) {
                flash("error", "Please correct errors above.");
                return ok(views.html.test_nodes_creation.render(
                        formData,
                        machineTypes,
                        images
                ));
            } else {
                try {
                    googleService.createTestNodes(nodeCreationForm.getTestNodes(), nodeCreationForm.getMachineType(),
                            nodeCreationForm.getImage(), nodeCreationForm.getRootDiskSizeGb());
                    flash("success", "Test nodes creation launched! Please check the running operations in the cluster status page.");
                    return ok(views.html.test_nodes_creation.render(
                            formData,
                            null,
                            null
                    ));
                } catch(GoogleComputeEngineException e) {
                    return ok(views.html.error.render(e.getMessage()));
                }
            }
        } catch(GoogleComputeEngineException e) {
            return ok(views.html.error.render(e.getMessage()));
        }
    }

    public static WebSocket<JsonNode> testMeasurements() {
        return new WebSocket<JsonNode>() {
            public void onReady(final In<JsonNode> in, final Out<JsonNode> out) {
                final ActorRef measureActor = Akka.system().actorOf(Props.create(TestMeasureConnection.class, out));

                in.onClose(new F.Callback0() {
                    @Override
                    public void invoke() throws Throwable {
                        Akka.system().stop(measureActor);
                    }
                });

                Inbox inbox = Inbox.create(Akka.system());
                while(TestRunner.hasTestNodesRunning()) {
                    Measure m = TestRunner.getMeasureFromQueue();
                    if(m != null) {
                        inbox.send(measureActor, m);
                    } else {
                        try {
                            Thread.sleep(100);
                        } catch(InterruptedException e) {}
                    }
                }
            }
        };
    }

    public static Result runTest() {
        String callBackUrl = GoogleComputeEngineAuthImpl.getCallBackURL(request());
        try {
            String result = GoogleAuthenticationService.authenticate(callBackUrl, null);
            if(result != null) {
                return redirect(result);
            }

            Map<String, Boolean> phases = new HashMap<>();
            phases.put("load", true);
            phases.put("run", false);

            RunTestForm runTest = new RunTestForm();
            runTest.setThreads(1);
            runTest.setRecordCount(1000);
            runTest.setOperationCount(1000);
            runTest.setBulkCount(100);
            runTest.setReadProportion(0.8F);
            runTest.setUpdateProportion(0.2F);
            runTest.setScanProportion(0F);
            runTest.setInsertProportion(0F);
            Form<RunTestForm> formData = Form.form(RunTestForm.class).fill(runTest);
            return ok(views.html.run_test.render(
                    formData,
                    phases
            ));
        } catch(GoogleComputeEngineException e) {
            return ok(views.html.error.render(e.getMessage()));
        }
    }

    public static Result runTestPost() {
        String callBackUrl = GoogleComputeEngineAuthImpl.getCallBackURL(request());
        try {
            String result = GoogleAuthenticationService.authenticate(callBackUrl, null);
            if(result != null) {
                return redirect(result);
            }

            Form<RunTestForm> formData = new BugWorkaroundForm<>(RunTestForm.class).bindFromRequest();
            RunTestForm runTest = formData.get();
            if(formData.hasErrors()) {
                Map<String, Boolean> phases = new HashMap<>();
                phases.put("load", false);
                phases.put("run", false);

                flash("error", "Please correct errors above.");
                return ok(views.html.run_test.render(
                        formData,
                        phases
                ));
            } else {
                Integer phase = Test.PHASE_RUN;
                if("load".equals(runTest.getPhase())) {
                    phase = Test.PHASE_LOAD;
                }
                YCSBWorkload workload = new YCSBWorkload();
                workload.setRecordcount(runTest.getRecordCount());
                workload.setOperationcount(runTest.getOperationCount());
                workload.setReadallfields(runTest.getReadAllFields());
                workload.setReadproportion(runTest.getReadProportion());
                workload.setUpdateproportion(runTest.getUpdateProportion());
                workload.setScanproportion(runTest.getScanProportion());
                workload.setInsertproportion(runTest.getInsertProportion());

                try {
                    TestRunner runner =  new YCSBTestRunner(workload, runTest.getThreads(), runTest.getBulkCount());

                    String jumpServerAddress = googleService.getJumpServerPublicAddress();
                    List<String> testNodesAddresses = googleService.getInstancesNetworkAddresses(
                            Arrays.asList(ConfigurationService.NODE_TAG_TEST));
                    if(jumpServerAddress == null || jumpServerAddress.isEmpty()) {
                        throw new TestException("No jump server found. Have you the test nodes created?");
                    }
                    if(testNodesAddresses == null || testNodesAddresses.isEmpty()) {
                        throw new TestException("No test nodes found. Something weird is happening, please create again the test nodes");
                    }
                    runner.runTest(phase, jumpServerAddress, testNodesAddresses);
                    flash("success", "Test run launched! Please check the outcome in the results page.");
                } catch(GoogleComputeEngineException e) {
                    return ok(views.html.error.render(e.getMessage()));
                } catch(TestException e) {
                    return ok(views.html.error.render(e.getMessage()));
                }
                return ok(views.html.run_test.render(
                        formData,
                        null
                ));
            }
        } catch(GoogleComputeEngineException e) {
            return ok(views.html.error.render(e.getMessage()));
        }
    }

    public static Result deleteTestNodes() {
        String callBackUrl = GoogleComputeEngineAuthImpl.getCallBackURL(request());
        try {
            String result = GoogleAuthenticationService.authenticate(callBackUrl, null);
            if(result != null) {
                return redirect(result);
            }
            TestNodeDeletionForm testNodesDeletion = new TestNodeDeletionForm();
            Form<TestNodeDeletionForm> formData = Form.form(TestNodeDeletionForm.class).fill(testNodesDeletion);
            return ok(views.html.test_nodes_deletion.render(
                    formData,
                    testNodesDeletion.getDelete()
            ));
        } catch(GoogleComputeEngineException e) {
            return ok(views.html.error.render(e.getMessage()));
        }
    }

    public static Result deleteTestNodesPost() {
        String callBackUrl = GoogleComputeEngineAuthImpl.getCallBackURL(request());
        try {
            String result = GoogleAuthenticationService.authenticate(callBackUrl, null);
            if(result != null) {
                return redirect(result);
            }
            Form<TestNodeDeletionForm> formData = new BugWorkaroundForm<>(TestNodeDeletionForm.class).bindFromRequest();
            TestNodeDeletionForm nodeDeletionForm = formData.get();
            if(formData.hasErrors()) {
                flash("error", "Please correct errors above.");
                return ok(views.html.test_nodes_deletion.render(
                        formData,
                        nodeDeletionForm.getDelete()
                ));
            } else {
                try {
                    googleService.deleteTestNodes();
                } catch(GoogleComputeEngineException e) {
                    return ok(views.html.error.render(e.getMessage()));
                }
                flash("success", "Test nodes deletion launched! Please check the running operations in the cluster status page.");
                return ok(views.html.test_nodes_deletion.render(
                        formData,
                        null
                ));
            }
        } catch(GoogleComputeEngineException e) {
            return ok(views.html.error.render(e.getMessage()));
        }
    }

    public static Result getTestNodesPrivateKey() {
        try {
            SSHKeyStore store = new SSHKeyStore();
            SSHKey key = store.getKey(ConfigurationService.TEST_USER);
            if(key == null) {
                return ok(views.html.error.render("no cluster key found"));
            }
            return ok(key.getSSHPrivateKey()).as("text/plain");
        } catch(ClassNotFoundException e) {
            return ok(views.html.error.render(e.getMessage()));
        } catch(IOException e) {
            return ok(views.html.error.render(e.getMessage()));
        }
    }
}
