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

package utils.gce.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.model.Bucket;
import com.google.api.services.storage.model.ObjectAccessControl;
import com.google.api.services.storage.model.StorageObject;
import conf.PlayConfiguration;
import play.libs.Json;
import utils.gce.auth.GoogleComputeEngineAuth;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by ricardolorenzo on 28/07/2014.
 */
public class GoogleCloudStorageClient {
    private static final String APPLICATION_NAME = "Ricardo Lorenzo GCE Client/1.0";
    private static final JsonFactory JSON_FACTORY;
    private static final HttpTransport HTTP_TRANSPORT;
    private static final String applicationDirectory;
    private static final String projectId;
    private GoogleComputeEngineAuth auth;
    private static Storage storage;

    static {
        JSON_FACTORY = new JacksonFactory();
        HTTP_TRANSPORT = new NetHttpTransport();
        applicationDirectory = PlayConfiguration.getProperty("application.directory");
        projectId = PlayConfiguration.getProperty("google.projectId");
    }

    public GoogleCloudStorageClient(final GoogleComputeEngineAuth auth) {
        this.auth = auth;
        storage = new Storage.Builder(HTTP_TRANSPORT, JSON_FACTORY, null).setApplicationName(APPLICATION_NAME)
                .setHttpRequestInitializer(auth.getCredential()).build();
    }

    public boolean fileExists(String bucketName, String fileName) throws GoogleCloudStorageException {
        try {
            Storage.Buckets.Get getBucket = storage.buckets().get(bucketName);
            getBucket.setProjection("full");
            Bucket bucket = getBucket.execute();

            Storage.Objects.Get getObject = storage.objects().get(bucketName, fileName);
            if(getObject.execute() != null) {
                return true;
            }
            return false;
        } catch(IOException e) {
            String message = e.getMessage();
            if(message != null && message.contains("{") && message.contains("}")) {
                message = message.substring(message.indexOf("{"));
                message = message.substring(0, message.lastIndexOf("}") + 1);
                JsonNode node = Json.parse(message).get("code");
                if(node != null && node.asInt() == 404) {
                    return false;
                }
            }
            throw new GoogleCloudStorageException(e);
        }
    }

    public void putFile(String bucketName, String fileName, String contentType, byte[] fileData) throws GoogleCloudStorageException {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(fileData);
            InputStreamContent fileContent = new InputStreamContent(contentType, in);
            fileContent.setLength(fileData.length);

            ObjectAccessControl acl = new ObjectAccessControl();
            acl.setEntity("allUsers");
            acl.setRole("READER");

            StorageObject object = new StorageObject();
            object.setName(fileName);
            object.setContentType(contentType);
            object.setAcl(Arrays.asList(acl));
            object.setContentDisposition("attachment");

            Storage.Objects.Insert insertObject = storage.objects().insert(bucketName, object, fileContent);
            /**
             * Reduce the number of HTTP requests made to the server, for small files (<=2MB).
             */
            if(fileContent.getLength() > 0 && fileContent.getLength() <= (2 * 1000 * 1000)) {
                insertObject.getMediaHttpUploader().setDirectUploadEnabled(true);
            }
            insertObject.execute();
        } catch(IOException e) {
            throw new GoogleCloudStorageException(e);
        }
    }

    public List<Bucket> listBuckets() throws GoogleCloudStorageException {
        try {
            Storage.Buckets.List listBucket = storage.buckets().list(projectId);
            return listBucket.execute().getItems();
        } catch(IOException e) {
            throw new GoogleCloudStorageException(e);
        }
    }

    public List<StorageObject> listFiles(String bucketName) throws GoogleCloudStorageException {
        try {
            Storage.Objects.List listObjects = storage.objects().list(bucketName);
            return listObjects.execute().getItems();
        } catch (IOException e) {
            throw new GoogleCloudStorageException(e);
        }
    }
}
