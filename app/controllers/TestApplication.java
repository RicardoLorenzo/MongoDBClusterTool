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

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import scala.Option;
import services.ConfigurationService;
import services.GoogleAuthenticationService;
import services.GoogleComputeEngineService;
import utils.gce.GoogleComputeEngineException;
import utils.gce.auth.GoogleComputeEngineAuthImpl;
import utils.play.BugWorkaroundForm;
import utils.puppet.PuppetConfiguration;
import utils.puppet.PuppetConfigurationException;
import utils.security.SSHKey;
import utils.security.SSHKeyStore;
import views.data.FileDeletionForm;
import views.data.TestNodeCreationForm;
import views.data.TestNodeDeletionForm;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

@org.springframework.stereotype.Controller
public class TestApplication extends Controller {
    private static GoogleAuthenticationService googleAuth;
    private static ConfigurationService configurationService;
    private static GoogleComputeEngineService googleService;

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
                    flash("success", "File successfully deleted.");
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

    public static Result runTest(Option<String> fileName) {
        String callBackUrl = GoogleComputeEngineAuthImpl.getCallBackURL(request());
        try {
            String result = GoogleAuthenticationService.authenticate(callBackUrl, null);
            if(result != null) {
                return redirect(result);
            }

            if(fileName.isEmpty()) {
                return redirect(routes.ConfigurationApplication.updatePuppetConfiguration());
            }
            FileDeletionForm fileDeletion = new FileDeletionForm();
            Form<FileDeletionForm> formData = Form.form(FileDeletionForm.class).fill(fileDeletion);
            return ok(views.html.file_deletion.render(
                    formData,
                    fileName.get(),
                    fileDeletion.getDelete()
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
            Form<FileDeletionForm> formData = new BugWorkaroundForm<>(FileDeletionForm.class).bindFromRequest();
            FileDeletionForm fileDeletion = formData.get();
            if(formData.hasErrors()) {
                flash("error", "Please correct errors above.");
                return ok(views.html.file_deletion.render(
                        formData,
                        fileDeletion.getFileName(),
                        fileDeletion.getDelete()
                ));
            } else {
                try {
                    ConfigurationService.deletePuppetFile(PuppetConfiguration.PUPPET_FILE, fileDeletion.getFileName());
                    flash("success", "Test nodes creation launched! Please check the running operations in the cluster status page.");
                } catch(PuppetConfigurationException e) {
                    return ok(views.html.error.render(e.getMessage()));
                }
                return ok(views.html.file_deletion.render(
                        formData,
                        null,
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
