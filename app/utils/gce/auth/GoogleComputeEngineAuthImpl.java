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

package utils.gce.auth;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.ComputeScopes;
import com.google.api.services.compute.model.ServiceAccount;
import conf.PlayConfiguration;
import controllers.routes;
import org.springframework.stereotype.Component;
import play.mvc.Http;
import scala.Option;
import utils.file.FileLockException;
import utils.file.FileUtils;
import utils.gce.GoogleComputeEngineException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Ricardo Lorenzo <ricardo.lorenzo@mongodb.com>.
 */
@Component(value = "gce-auth")
public class GoogleComputeEngineAuthImpl implements GoogleComputeEngineAuth {
    public static final List<String> SCOPES;
    private static final JsonFactory JSON_FACTORY;
    private static final HttpTransport HTTP_TRANSPORT;
    private static final GoogleAuthorizationCodeFlow authorizationCodeFlow;
    private static final String clientId;
    private static final String clientEmail;
    private static final String clientSecret;
    private static final String applicationDirectory;
    private static Logger log = Logger.getLogger(GoogleComputeEngineAuthImpl.class.toString());
    private Credential credential;
    private String refreshToken;

    static {
        SCOPES = Arrays.asList(ComputeScopes.COMPUTE, ComputeScopes.DEVSTORAGE_READ_WRITE);
        JSON_FACTORY = new JacksonFactory();
        HTTP_TRANSPORT = new NetHttpTransport();

        clientId = PlayConfiguration.getProperty("google.clientId");
        clientEmail = PlayConfiguration.getProperty("google.clientEmail");
        clientSecret = PlayConfiguration.getProperty("google.clientSecret");
        applicationDirectory = PlayConfiguration.getProperty("application.directory");
        authorizationCodeFlow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT,
                JSON_FACTORY, clientId, clientSecret, SCOPES).build();
    }

    public GoogleComputeEngineAuthImpl() throws GoogleComputeEngineException {
        File f = getRefreshTokenFile();
        try {
            if(f.exists()) {
                refreshToken = FileUtils.readFileAsString(f);
            }
        } catch(IOException e) {
            throw new GoogleComputeEngineException(e);
        }
    }

    private static File getRefreshTokenFile() throws GoogleComputeEngineException {
        File f;
        if(applicationDirectory != null && !applicationDirectory.isEmpty()) {
            f = new File(applicationDirectory + "/google.token");
            if(f.isDirectory()) {
                throw new GoogleComputeEngineException("invalid access to refresh-token - google.token is a directory [" + f.getAbsolutePath() + "]");
            }
        } else {
            try {
                f = File.createTempFile("google", ".token");
            } catch(IOException e) {
                throw new GoogleComputeEngineException("cannot access to refresh-token - " + e.getMessage());
            }
        }
        return f;
    }

    public static String getCallBackURL(Http.Request request) {
        if(request == null) {
            return null;
        }

        String url = routes.GoogleComputeEngineApplication.gceIndex(Option.empty()).url();
        if(url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("http://");
            sb.append(request.host());
            if(!url.startsWith("/")) {
                sb.append("/");
            }
            sb.append(url);
            return sb.toString();
        }
    }

    public List<ServiceAccount> getInstanceServiceAccounts() {
        ServiceAccount sa = new ServiceAccount();
        sa.setScopes(SCOPES);
        sa.setEmail(clientEmail);
        return Arrays.asList(sa);
    }

    @Override
    public Credential getCredential() {
        return credential;
    }

    public Credential authorize(final String userId, final String code, final String redirectUri) throws GoogleComputeEngineException {
        try {
            credential = authorizationCodeFlow.loadCredential(userId);
            GoogleTokenResponse tokenResponse;
            if(refreshToken == null || refreshToken.isEmpty()) {
                if(code == null) {
                    return null;
                }
                tokenResponse = getAccessToken(code, redirectUri);
                if(tokenResponse != null) {
                    persistRefreshToken(tokenResponse.getRefreshToken());
                }
            } else {
                tokenResponse = getAccessToken();
            }

            if(tokenResponse != null) {
                credential = authorizationCodeFlow.createAndStoreCredential(tokenResponse, userId);
                return credential;
            }
            return credential;
        } catch(IOException e) {
            throw new GoogleComputeEngineException(e);
        }
    }

    public String getRefreshTokenURL(final String redirectUri) throws GoogleComputeEngineException {
        GoogleAuthorizationCodeRequestUrl url = authorizationCodeFlow.newAuthorizationUrl();
        //GoogleAuthorizationCodeRequestUrl url = new GoogleAuthorizationCodeRequestUrl(clientId, redirectUri, SCOPES).setAccessType("offline");
        url.setRedirectUri(redirectUri);
        url.setApprovalPrompt("force");
        url.setAccessType("offline");
        url.setClientId(clientId);
        url.setScopes(SCOPES);
        return url.build();
    }

    private GoogleTokenResponse getAccessToken(final String code, final String redirectUri) {
        if(code == null) {
            return null;
        }
        try {
            return authorizationCodeFlow.newTokenRequest(code).setRedirectUri(redirectUri).execute();
        } catch(IOException e) {
        }
        return null;
    }

    private GoogleTokenResponse getAccessToken() throws IOException {
        GoogleRefreshTokenRequest refreshTokenRequest = new GoogleRefreshTokenRequest(HTTP_TRANSPORT, JSON_FACTORY, refreshToken, clientId, clientSecret);
        return refreshTokenRequest.execute();
    }

    private void persistRefreshToken(final String refreshToken) throws GoogleComputeEngineException {
        if(refreshToken == null || refreshToken.isEmpty()) {
            throw new GoogleComputeEngineException("cannot persist refresh-token because is empty");
        }
        this.refreshToken = refreshToken;
        File f = getRefreshTokenFile();
        try {
            FileUtils.writeFile(f, refreshToken);
        } catch(IOException e) {
            throw new GoogleComputeEngineException("cannot persist refresh-token on file [" + f.getAbsolutePath() + "] - " + e.getMessage());
        } catch(FileLockException e) {
            throw new GoogleComputeEngineException("cannot persist refresh-token on file [" + f.getAbsolutePath() + "] - " + e.getMessage());
        }
    }
}