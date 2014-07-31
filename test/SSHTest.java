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

import org.junit.Test;
import play.test.Helpers;
import services.ConfigurationService;
import utils.ssh.SSHClient;
import utils.ssh.SSHException;

import java.io.IOException;

/**
 * Created by ricardolorenzo on 27/07/2014.
 */
public class SSHTest {

    @Test
    public void sshConnectionTest() {
        Helpers.running(Helpers.fakeApplication(), () -> {
            try {
                SSHClient client = new SSHClient("23.236.60.31", 22);
                try {
                    client.connect(ConfigurationService.TEST_USER);
                    //client.sendCommand("ls", "-l", "/");
                    System.out.println("===================================\n");
                    System.out.println("ls -l /\n");
                    System.out.println("===================================\n");
                    client.sendCommand("ls", "-l", "/");
                    System.out.println(client.getStringOutput());
                } finally {
                    client.disconnect();
                }
                assert client.getStringOutput() != null;
            } catch(SSHException e) {
                e.printStackTrace();
                assert false;
            } catch(IOException e) {
                e.printStackTrace();
                assert false;
            }
        });
    }
}
