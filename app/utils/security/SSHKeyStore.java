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

import conf.PlayConfiguration;
import utils.file.FileLockException;
import utils.file.FileUtils;
import utils.gce.GoogleComputeEngineException;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by ricardolorenzo on 25/07/2014.
 */
public class SSHKeyStore {
    private static final String applicationDirectory;
    private static ConcurrentMap<String, SSHKey> store;

    static {
        applicationDirectory = PlayConfiguration.getProperty("application.directory");
    }

    public SSHKeyStore() throws IOException, ClassNotFoundException {
        if(store == null) {
            File  f = getStoreFile();
            if(f.exists()) {
                store = ConcurrentMap.class.cast(FileUtils.readObjectFile(f));
            } else {
                store = new ConcurrentHashMap<>();
            }
        }
    }

    public void addKey(String alias, SSHKey key) throws GoogleComputeEngineException {
        if(alias == null || alias.isEmpty()) {
            throw new IllegalArgumentException("invalid argument [alias]");
        }
        store.put(alias, key);
        store();
    }

    public SSHKey getKey(String alias) {
        return store.get(alias);
    }

    private static File getStoreFile() {
        return new File(applicationDirectory + "/ssh.store");
    }

    private void store() throws GoogleComputeEngineException {
        File f = getStoreFile();
        try {
            FileUtils.writeObjectFile(f, store);
        } catch(IOException e) {
            throw new GoogleComputeEngineException("cannot persist refresh-token on file [" + f.getAbsolutePath() + "] - " + e.getMessage());
        } catch(FileLockException e) {
            throw new GoogleComputeEngineException("cannot persist refresh-token on file [" + f.getAbsolutePath() + "] - " + e.getMessage());
        }
    }
}
