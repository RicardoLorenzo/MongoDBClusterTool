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
import utils.file.IOStreamUtils;
import utils.security.SSHKey;
import utils.security.SSHKeyStore;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by ricardolorenzo on 25/07/2014.
 */
public class SSHClient {
    private static Logger log = LoggerFactory.getLogger(SSHClient.class);
    private SSHKeyStore keyStore;
    private JSch client;
    private String host;
    private Integer port;
    private Session session;
    private Map<String, Session> forwardSessions;
    private byte[] output;

    public SSHClient(String host, int port) throws IOException {
        JSch.setLogger(new SSHLogger());
        forwardSessions = new HashMap<>();
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
        if(this.host == null || this.host.isEmpty()) {
            throw new SSHException("ssh host not defined");
        }
        if(user == null || user.isEmpty()) {
            throw new SSHException("ssh user not defined");
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

    public void forwardConnect(String host, String user, int port) throws SSHException {
        if(host == null || host.isEmpty()) {
            throw new SSHException("ssh host not defined");
        }
        if(user == null || user.isEmpty()) {
            throw new SSHException("ssh user not defined");
        }
        Session session;
        try {
            SSHKey key = keyStore.getKey(user);
                client.addIdentity(user, key.getSSHPrivateKey().getBytes(), key.getSSHPublicKey(user).getBytes(), null);
            if(user.contains("@")) {
                user = user.substring(0, user.indexOf("@"));
            }
            int assignedPort = this.session.setPortForwardingL(0, host, port);
            session = this.client.getSession(user, host, assignedPort);
            Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            forwardSessions.put(host, session);
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

    public void forwardDisconnect(String host) {
        Session session = this.forwardSessions.remove(host);
        if(session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    private static int verifyResponse(InputStream in) throws IOException {
        /**
         * 0 for success and 1 or 2 for error
         */
        int b = in.read();
        if(b == 0 || b == -1) {
            return b;
        }
        if(b == 1 || b == 2) {
            StringBuilder sb = new StringBuilder();
            int c;
            do {
                c = in.read();
                sb.append((char)c);
            } while(c != '\n');
            if(b == 1 || b == 2) {
                throw new IOException("transfer file connection returned an error reply");
            }
        }
        return b;
    }

    public byte[] getOutput() {
        return this.output;
    }

    public String getStringOutput() {
        return new String(this.output);
    }

    /**
     * This method read the files from the server and load the content on memory.
     *
     * @param sourcePath
     * @return
     * @throws SSHException
     */
    public Map<String, byte[]> getFiles(String sourcePath) throws SSHException {
        return getFiles(this.session, sourcePath);
    }

    /**
     *
     * @param host
     * @param sourcePath
     * @return
     * @throws SSHException
     */
    public Map<String, byte[]> getForwardFiles(String host, String sourcePath) throws SSHException {
        Session session = this.forwardSessions.get(host);
        return getFiles(session, sourcePath);
    }

    public static Map<String, byte[]> getFiles(Session session, String sourcePath) throws SSHException {
        if(session == null || !session.isConnected()) {
            throw new SSHException("not connected, please connect first");
        }
        Map<String, byte[]> files = new HashMap<>();
        try {
            Channel channel = null;
            OutputStream out = null;
            InputStream in = null;

            try {
                StringBuilder sb = new StringBuilder();
                sb.append("scp -f ");
                sb.append(sourcePath);
                channel = session.openChannel("exec");
                ChannelExec.class.cast(channel).setCommand(sb.toString());

                out = channel.getOutputStream();
                in = channel.getInputStream();
                channel.connect();

                while(true) {
                    /**
                     * Send '\0'
                     */
                    IOStreamUtils.write(new byte[] { 0 }, out);

                    int c = verifyResponse(in);
                    if(c != 'C') {
                        break;
                    }

                    String fileName = null;
                    long fileSize = 0L;
                    /**
                     * Read permissions (ex. '0644 ')
                     */
                    byte[] data = IOStreamUtils.read(in, 5);

                    /**
                     * Read file size
                     */
                    while(true) {
                        data = IOStreamUtils.read(in, 1);
                        if(data[0] == ' ') {
                            break;
                        }
                        fileSize = fileSize * 10L + Long.valueOf(data[0] - '0');
                    }

                    /**
                     * Read file name
                     */
                    data = IOStreamUtils.readUntilDataIsFound(in, new byte[] { (byte) 0x0a } );
                    fileName = new String(data).trim();

                    /**
                     * Send '\0'
                     */
                    IOStreamUtils.write(new byte[] { 0 }, out);

                    /**
                     * Read file content
                     */
                    ByteArrayOutputStream fileContent = new ByteArrayOutputStream();
                    IOStreamUtils.write(in, fileContent, fileSize);
                    verifyResponse(in);

                    files.put(fileName, fileContent.toByteArray());
                }
                return files;
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

    /**
     *
     * @param command
     * @return
     * @throws SSHException
     */
    public int sendCommand(String... command) throws SSHException {
        return sendCommand(this.session, this.output, command);
    }

    /**
     *
     * @param host
     * @param command
     * @return
     * @throws SSHException
     */
    public int sendForwardCommand(String host, String... command) throws SSHException {
        Session session = this.forwardSessions.get(host);
        return sendCommand(session, this.output, command);
    }

    public static int sendCommand(Session session, byte[] output, String... command) throws SSHException {
        if(session == null || !session.isConnected()) {
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
                channel = session.openChannel("exec");
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
                output = out.toByteArray();
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

    /**
     *
     * @param file
     * @param destinationPath
     * @param permissions
     * @throws SSHException
     */
    public void sendFile(File file, String destinationPath, FilePermissions permissions) throws SSHException {
        sendFile(this.session, file, destinationPath, permissions);
    }

    /**
     *
     * @param host
     * @param file
     * @param destinationPath
     * @param permissions
     * @throws SSHException
     */
    public void sendForwardFile(String host, File file, String destinationPath, FilePermissions permissions)
            throws SSHException {
        Session session = this.forwardSessions.get(host);
        sendFile(session, file, destinationPath, permissions);
    }

    public static void sendFile(Session session, File file, String destinationPath, FilePermissions permissions)
            throws SSHException {
        if(session == null || !session.isConnected()) {
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
                channel = session.openChannel("exec");
                ChannelExec.class.cast(channel).setCommand(sb.toString());

                out = channel.getOutputStream();
                in = channel.getInputStream();
                channel.connect();
                verifyResponse(in);


                /**
                 * Send modified and access time
                 */
                /*
                sb = new StringBuilder();
                sb.append("T ");
                sb.append(file.lastModified() / 1000);
                sb.append(" 0");
                sb.append(" ");
                sb.append(file.lastModified() / 1000);
                sb.append(" 0\n");
                out.write(sb.toString().getBytes());
                out.flush();
                verifyResponse(in);
                */

                sb = new StringBuilder();
                sb.append("C0");
                sb.append(permissions.getUserPermission());
                sb.append(permissions.getGroupPermission());
                sb.append(permissions.getAllPermission());
                sb.append(" ");
                sb.append(file.length());
                sb.append(" ");
                if(destinationPath.contains("/")) {
                    sb.append(destinationPath.substring(destinationPath.lastIndexOf("/") + 1));
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
                FileInputStream sourceStream = new FileInputStream(file);
                try {
                    IOStreamUtils.write(sourceStream, out);
                } finally {
                    sourceStream.close();
                }

                /**
                 * Send the final '\0'
                 */
                IOStreamUtils.write(new byte[]{0}, out);
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
