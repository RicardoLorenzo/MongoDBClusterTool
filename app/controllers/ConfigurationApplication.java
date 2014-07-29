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
import play.libs.F;
import play.mvc.Controller;
import play.mvc.Result;
import services.ConfigurationService;
import services.GoogleAuthenticationService;
import services.GoogleComputeEngineService;
import utils.gce.GoogleComputeEngineException;
import utils.gce.auth.GoogleComputeEngineAuthImpl;
import utils.puppet.PuppetConfiguration;
import utils.puppet.disk.PuppetDiskConfiguration;
import views.data.ClusterCreationForm;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@org.springframework.stereotype.Controller
public class ConfigurationApplication extends Controller {
    private static GoogleAuthenticationService googleAuth;
    private static ConfigurationService configurationService;
    private static GoogleComputeEngineService googleService;

    @Inject
    void setGoogleService(@Qualifier("gauth-service") GoogleAuthenticationService googleAuth) {
        ConfigurationApplication.googleAuth = googleAuth;
    }

    @Inject
    void setGoogleService(@Qualifier("conf-service") ConfigurationService confService) {
        ConfigurationApplication.configurationService = confService;
    }

    @Inject
    void setGoogleService(@Qualifier("gce-service") GoogleComputeEngineService googleService) {
        ConfigurationApplication.googleService = googleService;
    }

    public static F.Promise<Result> getManifestFile() {
        String callBackUrl = GoogleComputeEngineAuthImpl.getCallBackURL(request());
        try {
            //configurationService.;


            String result = googleAuth.authenticate(callBackUrl, null);
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
}
