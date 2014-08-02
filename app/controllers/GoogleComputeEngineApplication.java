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

import actors.GoogleComputeEngineConnection;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import play.data.Form;
import play.libs.Akka;
import play.libs.F;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import scala.Option;
import services.ConfigurationService;
import services.GoogleAuthenticationService;
import services.GoogleComputeEngineService;
import utils.gce.GoogleComputeEngineException;
import utils.gce.auth.GoogleComputeEngineAuthImpl;
import utils.gce.storage.GoogleCloudStorageException;
import utils.play.BugWorkaroundForm;
import utils.puppet.PuppetConfiguration;
import utils.puppet.disk.PuppetDiskConfiguration;
import utils.security.SSHKey;
import utils.security.SSHKeyStore;
import views.data.ClusterCreationForm;
import views.data.ClusterDeletionForm;

import javax.inject.Inject;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@org.springframework.stereotype.Controller
public class GoogleComputeEngineApplication extends Controller {
    private static GoogleAuthenticationService googleAuth;
    private static GoogleComputeEngineService googleService;

    @Inject
    void setGoogleService(@Qualifier("gauth-service") GoogleAuthenticationService googleAuth) {
        GoogleComputeEngineApplication.googleAuth = googleAuth;
    }

    @Inject
    void setGoogleService(@Qualifier("gce-service") GoogleComputeEngineService googleService) {
        GoogleComputeEngineApplication.googleService = googleService;
    }

    public static Result gceIndex(Option<String> code) {
        String callBackUrl = GoogleComputeEngineAuthImpl.getCallBackURL(request());
        try {
            String result = GoogleAuthenticationService.authenticate(callBackUrl, code.isDefined() ? code.get() : null);
            if(result != null) {
                return redirect(result);
            }
            return ok(views.html.index.render(request().path(), code));
        } catch(GoogleComputeEngineException e) {
            return ok(views.html.error.render(e.getMessage()));
        }
    }

    public static F.Promise<Result> gceDisks() {
        String callBackUrl = GoogleComputeEngineAuthImpl.getCallBackURL(request());
        try {
            String result = GoogleAuthenticationService.authenticate(callBackUrl, null);
            if(result != null) {
                return F.Promise.promise(() -> redirect(result));
            }
            return F.Promise.promise(() -> ok(GoogleComputeEngineService.listDisks()));
        } catch(GoogleComputeEngineException e) {
            return F.Promise.promise(() -> ok(views.html.error.render(e.getMessage())));
        }
    }

    public static F.Promise<Result> gceDiskTypes() {
        String callBackUrl = GoogleComputeEngineAuthImpl.getCallBackURL(request());
        try {
            String result = googleAuth.authenticate(callBackUrl, null);
            if(result != null) {
                return F.Promise.promise(() -> redirect(result));
            }
            return F.Promise.promise(() -> ok(GoogleComputeEngineService.listDiskTypes()));
        } catch(GoogleComputeEngineException e) {
            return F.Promise.promise(() -> ok(views.html.error.render(e.getMessage())));
        }
    }

    public static F.Promise<Result> gceInstances(Option<String> type) {
        String callBackUrl = GoogleComputeEngineAuthImpl.getCallBackURL(request());
        try {
            String result = GoogleAuthenticationService.authenticate(callBackUrl, null);
            if(result != null) {
                return F.Promise.promise(() -> redirect(result));
            }
            if(type.isDefined()) {
                return F.Promise.promise(() -> ok(GoogleComputeEngineService.listInstances(Arrays.asList(new String[]{type.get()}))));
            } else {
                return F.Promise.promise(() -> ok(GoogleComputeEngineService.listInstances(null)));
            }
        } catch(GoogleComputeEngineException e) {
            return F.Promise.promise(() -> ok(views.html.error.render(e.getMessage())));
        }
    }

    public static F.Promise<Result> gceNetworks() {
        String callBackUrl = GoogleComputeEngineAuthImpl.getCallBackURL(request());
        try {
            String result = googleAuth.authenticate(callBackUrl, null);
            if(result != null) {
                return F.Promise.promise(() -> redirect(result));
            }
            return F.Promise.promise(() -> ok(GoogleComputeEngineService.listNetworks()));
        } catch(GoogleComputeEngineException e) {
            return F.Promise.promise(() -> ok(views.html.error.render(e.getMessage())));
        }
    }

    public static WebSocket<JsonNode> gceOperations() {
        return new WebSocket<JsonNode>() {
            public void onReady(final In<JsonNode> in, final Out<JsonNode> out) {
                final ActorRef computeActor = Akka.system().actorOf(Props.create(GoogleComputeEngineConnection.class, out));

                in.onMessage(new F.Callback<JsonNode>() {
                    @Override
                    public void invoke(JsonNode jsonNode) throws Throwable {
                        if(jsonNode.has("action") && "retrieve".equals(jsonNode.get("action").textValue())) {
                            Date lastOperationDate = null;
                            if(jsonNode.has("lastOperationDate")) {
                                DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
                                try {
                                    lastOperationDate = format.parse(jsonNode.get("lastOperationDate").textValue());
                                } catch(ParseException e) {
                                    System.out.println("Date parse error: " + e.getMessage());
                                }
                            }
                            final ActorRef computeActor = Akka.system().actorOf(Props.create(GoogleComputeEngineConnection.class, out));
                            GoogleComputeEngineService.listOperations(computeActor, lastOperationDate);
                        }
                    }
                });

                in.onClose(new F.Callback0() {
                    @Override
                    public void invoke() throws Throwable {
                        Akka.system().stop(computeActor);
                    }
                });
            }
        };
    }

    public static F.Promise<Result> gceZones() {
        String callBackUrl = GoogleComputeEngineAuthImpl.getCallBackURL(request());
        try {
            String result = GoogleAuthenticationService.authenticate(callBackUrl, null);
            if(result != null) {
                return F.Promise.promise(() -> redirect(result));
            }
            return F.Promise.promise(() -> ok(GoogleComputeEngineService.listZones()));
        } catch(GoogleComputeEngineException e) {
            return F.Promise.promise(() -> ok(views.html.error.render(e.getMessage())));
        }
    }

    public static Result createClusterWizard() {
        String callBackUrl = GoogleComputeEngineAuthImpl.getCallBackURL(request());
        try {
            boolean option_chosen;
            String result = googleAuth.authenticate(callBackUrl, null);
            if(result != null) {
                return redirect(result);
            }
            Map<String, Boolean> machineTypes = new TreeMap<>(Comparator.<String>naturalOrder()),
                    images = new TreeMap<>(Comparator.<String>reverseOrder()),
                    networks = new TreeMap<>(Comparator.<String>reverseOrder()),
                    dataDiskTypes = new TreeMap<>(Comparator.<String>reverseOrder()),
                    dataDiskRaids = new TreeMap<>(Comparator.<String>naturalOrder()),
                    filesystems = new HashMap<>();
            ClusterCreationForm clusterCreation = new ClusterCreationForm();
            Form<ClusterCreationForm> formData = Form.form(ClusterCreationForm.class).fill(clusterCreation);
            for(PuppetDiskConfiguration diskConfig : PuppetConfiguration.getSupportedDiskConfigurations()) {
                dataDiskRaids.put(diskConfig.getName(), "raid0".equals(diskConfig.getName()));
            }
            for(JsonNode n : GoogleComputeEngineService.listDiskTypes().findValues("name")) {
                dataDiskTypes.put(n.textValue(), "pd-ssd".equals(n.textValue()));
            }
            for(String fs : PuppetConfiguration.getSupportedDiskFileSystems()) {
                filesystems.put(fs, "ext4".equals(fs));
            }
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
            for(JsonNode n : GoogleComputeEngineService.listNetworks().findValues("name")) {
                if(!option_chosen) {
                    networks.put(n.textValue(), true);
                    option_chosen = true;
                } else {
                    networks.put(n.textValue(), false);
                }
            }
            return ok(views.html.cluster_creation.render(
                    formData,
                    machineTypes,
                    images,
                    networks,
                    dataDiskTypes,
                    dataDiskRaids,
                    filesystems,
                    Boolean.valueOf(clusterCreation.isCgroups())
            ));
        } catch(GoogleComputeEngineException e) {
            return ok(views.html.error.render(e.getMessage()));
        }
    }

    public static Result createClusterWizardPost() {
        String callBackUrl = GoogleComputeEngineAuthImpl.getCallBackURL(request());
        try {
            String result = GoogleAuthenticationService.authenticate(callBackUrl, null);
            if(result != null) {
                return redirect(result);
            }
        } catch(GoogleComputeEngineException e) {
            return ok(views.html.error.render(e.getMessage()));
        }
        Form<ClusterCreationForm> formData = new BugWorkaroundForm<>(ClusterCreationForm.class).bindFromRequest();
        ClusterCreationForm clusterForm = formData.get();
        if(formData.hasErrors()) {
            Map<String, Boolean> machineTypes = new TreeMap<>(Comparator.<String>naturalOrder()),
                    images = new TreeMap<>(Comparator.<String>reverseOrder()),
                    networks = new TreeMap<>(Comparator.<String>reverseOrder()),
                    dataDiskTypes = new TreeMap<>(Comparator.<String>reverseOrder()),
                    dataDiskRaids = new TreeMap<>(Comparator.<String>naturalOrder()),
                    filesystems = new HashMap<>();
            flash("error", "Please correct errors above.");
            try {
                for(PuppetDiskConfiguration diskConfig : PuppetConfiguration.getSupportedDiskConfigurations()) {
                    dataDiskRaids.put(diskConfig.getName(), (clusterForm.getDiskRaid() != null &&
                            diskConfig.getName().equals(clusterForm.getDiskRaid())));
                }
                for(JsonNode n : GoogleComputeEngineService.listDiskTypes().findValues("name")) {
                    dataDiskTypes.put(n.textValue(), clusterForm.getDataDiskType() != null &&
                            n.textValue().equals(clusterForm.getDataDiskType()));
                }
                for(String fs : PuppetConfiguration.getSupportedDiskFileSystems()) {
                    filesystems.put(fs, (clusterForm.getFileSystem() != null && fs.equals(clusterForm.getFileSystem())));
                }
                for(JsonNode n : GoogleComputeEngineService.listMachineTypes().findValues("name")) {
                    machineTypes.put(n.textValue(), (clusterForm.getMachineType() != null &&
                            n.textValue().equals(clusterForm.getMachineType())));
                }
                for(JsonNode n : GoogleComputeEngineService.listImages().findValues("name")) {
                    images.put(n.textValue(), (clusterForm.getImage() != null &&
                            n.textValue().equals(clusterForm.getImage())));
                }
                for(JsonNode n : GoogleComputeEngineService.listNetworks().findValues("name")) {
                    networks.put(n.textValue(), (clusterForm.getNetwork() != null &&
                            n.textValue().equals(clusterForm.getNetwork())));
                }
                return ok(views.html.cluster_creation.render(
                        formData,
                        machineTypes,
                        images,
                        networks,
                        dataDiskTypes,
                        dataDiskRaids,
                        filesystems,
                        Boolean.valueOf(clusterForm.isCgroups())
                ));
            } catch(GoogleComputeEngineException e) {
                return ok(views.html.error.render(e.getMessage()));
            }
        } else {
            try {
                googleService.createCluster(clusterForm.getClusterName(), clusterForm.getShardNodes(),
                        clusterForm.getProcesses(), clusterForm.getNodeDisks(), clusterForm.getMachineType(),
                        Arrays.asList(clusterForm.getNetwork()), clusterForm.getImage(), clusterForm.getDataDiskType(),
                        clusterForm.getDataDiskType(), clusterForm.getFileSystem(), clusterForm.getDataDiskSizeGb(),
                        clusterForm.getRootDiskSizeGb());
            } catch(GoogleComputeEngineException e) {
                return ok(views.html.error.render(e.getMessage()));
            } catch(GoogleCloudStorageException e) {
                return ok(views.html.error.render(e.getMessage()));
            }
            flash("success", "Cluster creation launched! Please check the running operations in the cluster status page.");
            return ok(views.html.cluster_creation.render(
                    formData,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            ));
        }
    }

    public static Result deleteClusterWizard() {
        String callBackUrl = GoogleComputeEngineAuthImpl.getCallBackURL(request());
        try {
            String result = googleAuth.authenticate(callBackUrl, null);
            if(result != null) {
                return redirect(result);
            }
            ClusterDeletionForm clusterDeletion = new ClusterDeletionForm();
            Form<ClusterDeletionForm> formData = Form.form(ClusterDeletionForm.class).fill(clusterDeletion);
            return ok(views.html.cluster_deletion.render(
                    formData,
                    clusterDeletion.getDelete()
            ));
        } catch(GoogleComputeEngineException e) {
            return ok(views.html.error.render(e.getMessage()));
        }
    }

    public static Result deleteClusterWizardPost() {
        String callBackUrl = GoogleComputeEngineAuthImpl.getCallBackURL(request());
        try {
            String result = googleAuth.authenticate(callBackUrl, null);
            if(result != null) {
                return redirect(result);
            }
            Form<ClusterDeletionForm> formData = new BugWorkaroundForm<>(ClusterDeletionForm.class).bindFromRequest();
            ClusterDeletionForm clusterForm = formData.get();
            if(formData.hasErrors()) {
                flash("error", "Please correct errors above.");
                return ok(views.html.cluster_deletion.render(
                        formData,
                        clusterForm.getDelete()
                ));
            } else {
                try {
                    googleService.deleteCluster();
                } catch(GoogleComputeEngineException e) {
                    return ok(views.html.error.render(e.getMessage()));
                }
                flash("success", "Cluster deletion launched! Please check the running operations in the cluster status page.");
                return ok(views.html.cluster_deletion.render(
                        formData,
                        null
                ));
            }
        } catch(GoogleComputeEngineException e) {
            return ok(views.html.error.render(e.getMessage()));
        }
    }

    public static Result getClusterPrivateKey() {
        try {
            SSHKeyStore store = new SSHKeyStore();
            SSHKey key = store.getKey(ConfigurationService.CLUSTER_USER);
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
