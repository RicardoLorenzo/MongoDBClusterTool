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

package utils.ssh;

import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.security.SSHKey;
import utils.security.SSHKeyStore;

import java.io.*;
import java.util.Date;
import java.util.Properties;

/**
 * Created by ricardolorenzo on 25/07/2014.
 */
public class SSHClient {
    private static Logger log = LoggerFactory.getLogger(SSHClient.class);
    public static final String DEFAULT_USER = "mongodb@localhost";
    private SSHKeyStore keyStore;
    private JSch client;
    private String host;
    private Integer port;
    private Session session;
    private byte[] output;

    public SSHClient(String host, int port) throws IOException {
        JSch.setLogger(new SSHLogger());
        client = new JSch();
        try {
            keyStore = new SSHKeyStore();
        } catch(ClassNotFoundException e) {
            throw new IOException(e.getMessage());
        }
        this.host = host;
        this.port = port;
    }

    public void connect(String user) throws SSHException {
        if(user == null || user.isEmpty()) {
            user = DEFAULT_USER;
        }
        Session session;
        try {
            SSHKey key = keyStore.getKey(user);
                client.addIdentity(user, key.getSSHPrivateKey().getBytes(), key.getSSHPublicKey(user).getBytes(), null);
            if(user.contains("@")) {
                user = user.substring(0, user.indexOf("@"));
            }
            session = client.getSession(user, this.host, this.port);
            Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
        } catch(JSchException e) {
            log.error("connection error: " + e.getMessage());
            throw new SSHException(e);
        }
        this.session = session;
    }

    public void disconnect() {
        if(this.session != null && this.session.isConnected()) {
            this.session.disconnect();
        }
    }

    private boolean verifyResponse(InputStream in) throws IOException {
        /**
         * 0 for success and >= 1 for error
         */
        int b = in.read();
        if(b == 0) {
            return true;
        }
        if(b == -1) {
            throw new IOException("unknown error establishing transfer file connection");
        }

        if(b == 1 || b == 2) {
            StringBuilder sb = new StringBuilder();
            int c;
            do {
                c = in.read();
                sb.append((char)c);
            } while(c != '\n');
            if(b >= 1) {
                throw new IOException("transfer file connection returned an error reply");
            }
        }
        return false;
    }

    public byte[] getOutput() {
        return this.output;
    }

    public String getStringOutput() {
        return new String(this.output);
    }

    public int sendCommand(String... command) throws SSHException {
        if(this.session == null || !this.session.isConnected()) {
            throw new SSHException("not connected, please connect first");
        }
        try {
            Channel channel = null;
            InputStream in = null;
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            try {
                StringBuilder sb = new StringBuilder();
                for(String tok : command) {
                    if(sb.length() > 0) {
                        sb.append(" ");
                    }
                    sb.append(tok);
                }
                channel = this.session.openChannel("exec");
                ChannelExec.class.cast(channel).setCommand(sb.toString());
                channel.setOutputStream(null);
                in = channel.getInputStream();
                channel.connect();

                byte[] buffer = new byte[1024];
                while(true) {
                    for(int length = in.read(buffer, 0, buffer.length); length > 0; length = in.read(buffer, 0, buffer.length)) {
                        out.write(buffer, 0, length);
                    }
                    if(channel.isClosed()) {
                        if(in.available() > 0) {
                            continue;
                        }
                        break;
                    }
                    try {
                        Thread.sleep(250);
                    } catch(InterruptedException e) {}
                }
                this.output = out.toByteArray();
                return channel.getExitStatus();
            } finally {
                if(in != null) {
                    in.close();
                }
                if(out != null) {
                    out.close();
                }
                if(channel != null) {
                    channel.disconnect();
                }
            }
        } catch(IOException e) {
            log.error("command error: " + e.getMessage());
            throw new SSHException(e);
        } catch(JSchException e) {
            log.error("connection error: " + e.getMessage());
            throw new SSHException(e);
        }
    }

    public void sendFile(byte[] data, String destinationPath, FilePermissions permissions)
            throws SSHException {
        if(this.session == null || !this.session.isConnected()) {
            throw new SSHException("not connected, please connect first");
        }
        try {
            Channel channel = null;
            OutputStream out = null;
            InputStream in = null;

            try {
                /**
                 * remote execution of 'scp -t destinationPath' command
                 */
                StringBuilder sb = new StringBuilder();
                sb.append("scp -p -t ");
                sb.append(destinationPath);
                channel = this.session.openChannel("exec");
                ChannelExec.class.cast(channel).setCommand(sb.toString());

                out = channel.getOutputStream();
                in = channel.getInputStream();
                channel.connect();
                verifyResponse(in);

                /**
                 * Send modified and access time
                 */
                sb = new StringBuilder();
                Date time = new Date();
                sb.append("T ");
                sb.append(time.getTime() / 1000);
                sb.append(" 0");
                sb.append(" ");
                sb.append(time.getTime() / 1000);
                sb.append(" 0\n");
                out.write(sb.toString().getBytes());
                out.flush();
                verifyResponse(in);

                sb = new StringBuilder();
                sb.append("C0");
                sb.append(permissions.getUserPermission());
                sb.append(permissions.getGroupPermission());
                sb.append(permissions.getAllPermission());
                sb.append(" ");
                sb.append(data.length);
                sb.append(" ");
                if(destinationPath.contains("/")) {
                    sb.append(destinationPath.substring(destinationPath.indexOf("/") + 1));
                } else {
                    sb.append(destinationPath);
                }
                sb.append("\n");
                out.write(sb.toString().getBytes());
                out.flush();
                verifyResponse(in);

                /**
                 * Send file
                 */
                byte[] buffer = new byte[1024];
                ByteArrayInputStream sourceStream = new ByteArrayInputStream(data);
                try {
                    for(int length = sourceStream.read(buffer, 0, buffer.length); length > 0; length = sourceStream.read(buffer, 0, buffer.length)) {
                        out.write(buffer, 0, length);
                    }
                } finally {
                    sourceStream.close();
                }

                /**
                 * Send the final '\0'
                 */
                buffer[0] = 0;
                out.write(buffer, 0, 1);
                out.flush();
                verifyResponse(in);
            } finally {
                if(in != null) {
                    in.close();
                }
                if(out != null) {
                    out.close();
                }
                if(channel != null) {
                    channel.disconnect();
                }
            }
        } catch(IOException e) {
            log.error("file send error: " + e.getMessage());
            throw new SSHException(e);
        } catch(JSchException e) {
            log.error("file send error: " + e.getMessage());
            throw new SSHException(e);
        }
    }
}

class SSHLogger implements com.jcraft.jsch.Logger {
    private static Logger log = LoggerFactory.getLogger(SSHClient.class);
    public boolean isEnabled(int level){
        return true;
    }
    public void log(int level, String message){
        switch(level) {
            case INFO:
                log.info(message);
                break;
            case WARN:
                log.warn(message);
                break;
            case ERROR:
                log.error(message);
                break;
            case FATAL:
                log.error(message);
                break;
            default:
                log.debug(message);
                break;
        }
    }
}
