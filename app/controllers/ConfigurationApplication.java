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

import org.springframework.beans.factory.annotation.Qualifier;
import play.data.Form;
import play.libs.F;
import play.mvc.Controller;
import play.mvc.Result;
import scala.Option;
import services.ConfigurationService;
import services.GoogleAuthenticationService;
import services.GoogleComputeEngineService;
import utils.gce.GoogleComputeEngineException;
import utils.gce.auth.GoogleComputeEngineAuthImpl;
import utils.puppet.PuppetConfiguration;
import utils.puppet.PuppetConfigurationException;
import views.data.ClusterEditionForm;

import javax.inject.Inject;
import java.util.Comparator;
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

    public static F.Promise<Result> getManifestFile(Option<String> fileName) {
        String callBackUrl = GoogleComputeEngineAuthImpl.getCallBackURL(request());
        try {
            String result = GoogleAuthenticationService.authenticate(callBackUrl, null);
            if(result != null) {
                return F.Promise.promise(() -> redirect(result));
            }

            if(!fileName.nonEmpty()) {
                ok();
            }

            String fileContent = ConfigurationService.getPuppetFile(PuppetConfiguration.PUPPET_MANIFEST,
                    fileName.get());

            return F.Promise.promise(() -> ok(fileContent));
        } catch(GoogleComputeEngineException e) {
            return F.Promise.promise(() -> ok(views.html.error.render(e.getMessage())));
        } catch(PuppetConfigurationException e) {
            return F.Promise.promise(() -> ok(views.html.error.render(e.getMessage())));
        }
    }

    public static Result updatePuppetConfiguration() {
        String callBackUrl = GoogleComputeEngineAuthImpl.getCallBackURL(request());
        try {
            String result = GoogleAuthenticationService.authenticate(callBackUrl, null);
            if(result != null) {
                return redirect(result);
            }

            ClusterEditionForm clusterEdition = new ClusterEditionForm();
            Form<ClusterEditionForm> formData = Form.form(ClusterEditionForm.class).fill(clusterEdition);
            Map<String, Boolean> fileNames = new TreeMap<>(Comparator.<String>naturalOrder());
            fileNames.put("mongodb-base.pp", false);
            fileNames.put("mongodb-conf.pp", false);
            fileNames.put("mongodb-shard.pp", false);
            return ok(views.html.cluster_edition.render(
                    formData,
                    fileNames

            ));
        } catch(GoogleComputeEngineException e) {
            return ok(views.html.error.render(e.getMessage()));
        }
    }
}
