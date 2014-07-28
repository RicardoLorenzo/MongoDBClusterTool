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

package utils.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by ricardolorenzo on 25/07/2014.
 */
public class GoogleComputeEngineFingerprint {

    public static byte[] getSha1Hash(String data) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
            md.reset();
            md.update(data.getBytes());
            return md.digest();
        } catch(NoSuchAlgorithmException e) {
        }
        return new byte[0];
    }
}
