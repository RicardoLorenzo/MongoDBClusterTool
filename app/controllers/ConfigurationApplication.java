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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Qualifier;
import play.data.Form;
import play.libs.F;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import scala.Option;
import services.ConfigurationService;
import services.GoogleAuthenticationService;
import services.GoogleComputeEngineService;
import utils.file.FileLockException;
import utils.file.FileUtils;
import utils.gce.GoogleComputeEngineException;
import utils.gce.auth.GoogleComputeEngineAuthImpl;
import utils.play.BugWorkaroundForm;
import utils.puppet.PuppetConfiguration;
import utils.puppet.PuppetConfigurationException;
import views.data.ClusterEditionForm;
import views.data.FileDeletionForm;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

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

    public static Result deletePuppetFile(Option<String> fileName) {
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

    public static Result deletePuppetFilePost() {
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
                    flash("success", "File successfully deleted.");
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

    public static F.Promise<Result> getManifestFile(Option<String> fileName) {
        String callBackUrl = GoogleComputeEngineAuthImpl.getCallBackURL(request());
        try {
            String result = GoogleAuthenticationService.authenticate(callBackUrl, null);
            if(result != null) {
                return F.Promise.promise(() -> redirect(result));
            }
            if(!fileName.nonEmpty()) {
                return F.Promise.promise(() -> ok());
            }

            String fileContent = ConfigurationService.getPuppetFile(PuppetConfiguration.PUPPET_MANIFEST,
                    fileName.get());

            return F.Promise.delayed(() -> ok(fileContent), 1, TimeUnit.SECONDS);
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
            List<String> fileList = ConfigurationService.listPuppetFiles(PuppetConfiguration.PUPPET_FILE);
            fileList.sort(Comparator.<String>naturalOrder());
            fileNames.put("mongodb-base.pp", false);
            fileNames.put("mongodb-conf.pp", false);
            fileNames.put("mongodb-shard.pp", false);
            return ok(views.html.cluster_edition.render(
                    formData,
                    fileNames,
                    fileList
            ));
        } catch(GoogleComputeEngineException e) {
            return ok(views.html.error.render(e.getMessage()));
        } catch(PuppetConfigurationException e) {
            return ok(views.html.error.render(e.getMessage()));
        }
    }

    public static Result updatePuppetConfigurationPost() {
        String callBackUrl = GoogleComputeEngineAuthImpl.getCallBackURL(request());
        try {
            String result = GoogleAuthenticationService.authenticate(callBackUrl, null);
            if(result != null) {
                return redirect(result);
            }

            Form<ClusterEditionForm> formData = new BugWorkaroundForm<>(ClusterEditionForm.class).bindFromRequest();
            ClusterEditionForm clusterForm = formData.get();

            if(formData.hasErrors()) {
                flash("error", "Please correct errors above.");
            } else {
                try {
                    File f = File.createTempFile(clusterForm.getFileName(), "tmp");
                    FileUtils.writeFile(f, clusterForm.getFileContent());
                    ConfigurationService.uploadPuppetFile(PuppetConfiguration.PUPPET_MANIFEST, clusterForm.getFileName(),
                            f);
                    flash("success", "Configuration file updated.");
                } catch(IOException e) {
                    flash("error", e.getMessage());
                } catch(FileLockException e) {
                    flash("error", e.getMessage());
                }
            }

            Map<String, Boolean> fileNames = new TreeMap<>(Comparator.<String>naturalOrder());
            List<String> fileList = ConfigurationService.listPuppetFiles(PuppetConfiguration.PUPPET_FILE);
            fileList.sort(Comparator.<String>naturalOrder());
            fileNames.put("mongodb-base.pp", (clusterForm.getFileName() != null && clusterForm.getFileName().equals("mongodb-base.pp")));
            fileNames.put("mongodb-conf.pp", (clusterForm.getFileName() != null && clusterForm.getFileName().equals("mongodb-conf.pp")));
            fileNames.put("mongodb-shard.pp", (clusterForm.getFileName() != null && clusterForm.getFileName().equals("mongodb-shard.pp")));

            return ok(views.html.cluster_edition.render(
                    formData,
                    fileNames,
                    fileList
            ));
        } catch(GoogleComputeEngineException e) {
            return ok(views.html.error.render(e.getMessage()));
        } catch(PuppetConfigurationException e) {
            return ok(views.html.error.render(e.getMessage()));
        }
    }

    public static Result uploadPuppetFile() {
        String callBackUrl = GoogleComputeEngineAuthImpl.getCallBackURL(request());
        ObjectNode returnMessage = Json.newObject();
        try {
            String result = GoogleAuthenticationService.authenticate(callBackUrl, null);
            if(result != null) {
                return redirect(result);
            }

            ArrayNode fileList = new ArrayNode(JsonNodeFactory.instance);
            for(Http.MultipartFormData.FilePart filePart : request().body().asMultipartFormData().getFiles()) {
                ConfigurationService.uploadPuppetFile(PuppetConfiguration.PUPPET_FILE, filePart.getFilename(),
                        filePart.getFile());
                fileList.add(filePart.getFilename());
            }
            returnMessage.set("result", Json.newObject().textNode("ok"));
            returnMessage.set("files", fileList);
            return ok(returnMessage);
        } catch(GoogleComputeEngineException e) {
            returnMessage.set("result", Json.newObject().textNode("error"));
            returnMessage.set("message", Json.newObject().textNode(e.getMessage()));
            return ok(returnMessage);
        } catch(PuppetConfigurationException e) {
            returnMessage.set("result", Json.newObject().textNode("error"));
            returnMessage.set("message", Json.newObject().textNode(e.getMessage()));
            return ok(returnMessage);
        }
    }
}
