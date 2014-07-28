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

package services;

import com.google.api.client.auth.oauth2.Credential;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import utils.gce.GoogleComputeEngineException;
import utils.gce.auth.GoogleComputeEngineAuth;

import javax.inject.Inject;

/**
 * Created by ricardolorenzo on 28/07/2014.
 */
@Service(value = "gauth-service")
@Configurable
public class GoogleAuthenticationService {
    private static GoogleComputeEngineAuth auth;

    @Inject
    void setGoogleService(@Qualifier("gce-auth") GoogleComputeEngineAuth googleAuth) {
        GoogleAuthenticationService.auth = googleAuth;
    }

    public static String authenticate(String callBackUrl, String authorizationCode) throws GoogleComputeEngineException {
        Credential credential = auth.authorize("default", authorizationCode, callBackUrl);
        if(credential == null) {
            return auth.getRefreshTokenURL(callBackUrl);
        }
        return null;
    }

    public static GoogleComputeEngineAuth getAuthentication() {
        return auth;
    }

    public static Credential getCredential() {
        return auth.getCredential();
    }
}
