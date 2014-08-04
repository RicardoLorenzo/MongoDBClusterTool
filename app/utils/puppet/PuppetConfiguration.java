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

package utils.puppet;

import conf.PlayConfiguration;
import utils.puppet.disk.PuppetDiskConfiguration;
import utils.puppet.manifest.PuppetClass;
import utils.puppet.manifest.PuppetModule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ricardolorenzo on 22/07/2014.
 */
public class PuppetConfiguration {
    public static final PuppetDiskConfiguration DISK_PER_PROCESS;
    public static final PuppetDiskConfiguration DISK_RAID0;
    public static final List<String> SUPPORTED_FILESYSTEMS;
    public static final int PUPPET_MANIFEST = 1;
    public static final int PUPPET_FILE = 2;
    private static final String DEBIAN_INSTALL_OPTS = "-o DPkg::options::=\"--force-confdef\" \\\n" +
            "     -o DPkg::options::=\"--force-confold\" -o Dpkg::Options::=\"--force-overwrite\" -y";
    private static final String PUPPET_HOME_DIR = "/etc/puppet";
    private static final String PUPPET_MANIFESTS_DIR = "manifests";
    private static final String PUPPET_FILES_DIR = "files";
    private static final String PUPPET_TEMPLATES_DIR = "templates";
    private static final String MONGODB_MOUNT_DIR = "/mnt/mongodb";
    private static final String projectId;

    static {
        SUPPORTED_FILESYSTEMS = new ArrayList<>();
        SUPPORTED_FILESYSTEMS.add("ext4");
        SUPPORTED_FILESYSTEMS.add("xfs");
        SUPPORTED_FILESYSTEMS.add("btrfs");
        DISK_PER_PROCESS = new PuppetDiskConfiguration("disk per process", "One process per disk, processes not share the disk");
        DISK_RAID0 = new PuppetDiskConfiguration("raid0", "RAID0, all processes share the same disk pool");
        projectId = PlayConfiguration.getProperty("google.projectId");
    }

    public static List<PuppetDiskConfiguration> getSupportedDiskConfigurations() {
        List<PuppetDiskConfiguration> supported_disk_configs = new ArrayList<>();
        supported_disk_configs.add(DISK_PER_PROCESS);
        supported_disk_configs.add(DISK_RAID0);
        return supported_disk_configs;
    }

    public static List<String> getSupportedDiskFileSystems() {
        return SUPPORTED_FILESYSTEMS;
    }

    public static String getPuppetHomeDirectory() {
        return PUPPET_HOME_DIR;
    }

    public static String getPuppetManifestsDirectory() {
        return PUPPET_HOME_DIR + "/" + PUPPET_MANIFESTS_DIR;
    }

    public static String getPuppetFilesDirectory() {
        return PUPPET_HOME_DIR + "/" + PUPPET_FILES_DIR;
    }

    public static String getPuppetTemplatesDirectory() {
        return PUPPET_HOME_DIR + "/" + PUPPET_TEMPLATES_DIR;
    }

    public static String generateNodeManifest(String clusterName) {
        StringBuilder sb = new StringBuilder();
        sb.append("node mongodb {\n  include mongodb-base\n}\n\n");
        sb.append("node /^");
        sb.append(clusterName);
        sb.append("-conf-node-\\d+$/ inherits mongodb {\n  include mongodb-conf\n}\n\n");
        sb.append("node /^");
        sb.append(clusterName);
        sb.append("-shard-node-\\d+$/ inherits mongodb {\n  include mongodb-shard\n}\n\n");
        sb.append("import 'mongodb-base.pp'\nimport 'mongodb-conf.pp'\nimport 'mongodb-shard.pp'\n");
        return sb.toString();
    }

    public static String generateMongoConfClassManifest() throws PuppetConfigurationException {
        PuppetClass confClass = new PuppetClass("mongodb-conf");
        confClass.setModule(new PuppetModule(PuppetModule.TYPE_SERVICE, "mongodb")
                .setProperty("ensure", "running")
                .setProperty("enable", "true")
                .setSubscribe(PuppetModule.TYPE_FILE, "/etc/mongodb.conf"));
        confClass.setModule(new PuppetModule(PuppetModule.TYPE_FILE, "/etc/mongodb.conf")
                .setStringProperty("owner", "root")
                .setStringProperty("group", "root")
                .setProperty("mode", "644")
                .setStringProperty("source", "puppet:///files/mongodb-config-server.conf")
                .setNotify(PuppetModule.TYPE_SERVICE, "mongodb")
                .setRequire(PuppetModule.TYPE_EXEC, "mongodb-10gen"));
        return confClass.toString();
    }

    public static String generateMongoShardClassManifest(String diskRaid, String dataFileSystem)
            throws PuppetConfigurationException {
        PuppetClass shardClass = new PuppetClass("mongodb-shard");
        shardClass.setModule(new PuppetModule(PuppetModule.TYPE_FILE, "/etc/mongodb-shards.conf")
                .setStringProperty("owner", "root")
                .setStringProperty("group", "root")
                .setProperty("mode", "644")
                .setStringProperty("source", "puppet:///files/mongodb-shards.conf"));
        shardClass.setModule(new PuppetModule(PuppetModule.TYPE_FILE, "/mnt/mongodb")
                .setStringProperty("owner", "mongodb")
                .setStringProperty("group", "mongodb")
                .setProperty("mode", "755")
                .setStringProperty("ensure", "directory"));
        shardClass.setModule(new PuppetModule(PuppetModule.TYPE_FILE, "/usr/local/bin/puppet-disk-format")
                .setStringProperty("owner", "root")
                .setStringProperty("group", "root")
                .setProperty("mode", "755")
                .setStringProperty("source", "puppet:///files/puppet-disk-format.sh")
                .setRequire(PuppetModule.TYPE_FILE, "/etc/mongodb-shards.conf"));
        shardClass.setModule(new PuppetModule(PuppetModule.TYPE_FILE, "/etc/init.d/mongodb-microshards")
                .setStringProperty("owner", "root")
                .setStringProperty("group", "root")
                .setProperty("mode", "755")
                .setStringProperty("source", "puppet:///files/puppet-mongodb-microshards.sh")
                .setRequire(PuppetModule.TYPE_EXEC, "disk-format")
                .setRequire(PuppetModule.TYPE_EXEC, "test-mongodb-microshards"));
        shardClass.setModule(new PuppetModule(PuppetModule.TYPE_EXEC, "mongod")
                .setStringProperty("command", "/usr/sbin/update-rc.d -f mongod remove && /usr/sbin/service mongod stop")
                .setNotify(PuppetModule.TYPE_EXEC, "test-mongodb-microshards")
                .setRequire(PuppetModule.TYPE_EXEC, "mongodb-10gen"));
        shardClass.setModule(new PuppetModule(PuppetModule.TYPE_EXEC, "disk-format")
                .setProperty("onlyif", "\\\"/usr/bin/test -e " + MONGODB_MOUNT_DIR + " -a 0 -eq \\$(ls " +
                        MONGODB_MOUNT_DIR + " | wc -l)\\\"")
                .setStringProperty("command", "/usr/local/bin/puppet-disk-format")
                .setNotify(PuppetModule.TYPE_SERVICE, "mongodb-microshards")
                .setSubscribe(PuppetModule.TYPE_EXEC, "test-mongodb-microshards")
                .setRequire(PuppetModule.TYPE_FILE, "/mnt/mongodb")
                .setRequire(PuppetModule.TYPE_FILE, "/usr/local/bin/puppet-disk-format"));
        shardClass.setModule(new PuppetModule(PuppetModule.TYPE_EXEC, "test-mongodb-microshards")
                .setProperty("onlyif", "\\\"/usr/bin/test 1 -eq \\$(cat /proc/cgroups | grep memory | awk '{ print $4 }')\\\"")
                .setStringProperty("command", "/usr/bin/test 0")
                .setNotify(PuppetModule.TYPE_EXEC, "disk-format")
                .setSubscribe(PuppetModule.TYPE_EXEC, "mongod")
                .setRequire(PuppetModule.TYPE_PACKAGE, "cgroup-bin"));
        shardClass.setModule(new PuppetModule(PuppetModule.TYPE_SERVICE, "mongodb-microshards")
                .setProperty("ensure", "running")
                .setProperty("enable", "true")
                .setSubscribe(PuppetModule.TYPE_EXEC, "disk-format")
                .setRequire(PuppetModule.TYPE_EXEC, "mongod")
                .setRequire(PuppetModule.TYPE_FILE, "/etc/init.d/mongodb-microshards")
                .setRequire(PuppetModule.TYPE_EXEC, "test-mongodb-microshards"));
        shardClass.setModule(new PuppetModule(PuppetModule.TYPE_FILE, "/etc/default/grub")
                .setStringProperty("owner", "root")
                .setStringProperty("group", "root")
                .setProperty("mode", "644")
                .setStringProperty("source", "puppet:///files/grub")
                .setNotify(PuppetModule.TYPE_EXEC, "update-grub")
                .setRequire(PuppetModule.TYPE_EXEC, "mongodb-10gen"));
        shardClass.setModule(new PuppetModule(PuppetModule.TYPE_PACKAGE, "cgroup-bin")
                .setProperty("ensure", "present"));
        shardClass.setModule(new PuppetModule(PuppetModule.TYPE_EXEC, "update-grub")
                .setStringProperty("command", "/usr/sbin/update-grub")
                .setStringProperty("refreshonly", "true")
                .setNotify(PuppetModule.TYPE_EXEC, "reboot")
                .setSubscribe(PuppetModule.TYPE_FILE, "/etc/default/grub"));
        shardClass.setModule(new PuppetModule(PuppetModule.TYPE_EXEC, "reboot")
                .setStringProperty("command", "/sbin/reboot")
                .setStringProperty("refreshonly", "true")
                .setSubscribe(PuppetModule.TYPE_EXEC, "update-grub"));
        return shardClass.toString();
    }

    public static String generateMongoBaseClassManifest() throws PuppetConfigurationException {
        PuppetClass baseClass = new PuppetClass("mongodb-base");
        baseClass.setModule(new PuppetModule(PuppetModule.TYPE_CLASS, "timezone")
                .setStringProperty("timezone", "UTC"));
        baseClass.setModule(new PuppetModule(PuppetModule.TYPE_PACKAGE, "ntp")
                .setProperty("ensure", "present"));
        baseClass.setModule(new PuppetModule(PuppetModule.TYPE_SERVICE, "ntp")
                .setProperty("ensure", "running")
                .setProperty("enable", "true"));
        baseClass.setModule(new PuppetModule(PuppetModule.TYPE_FILE, "/etc/hosts")
                .setStringProperty("owner", "root")
                .setStringProperty("group", "root")
                .setProperty("mode", "644")
                .setProperty("content", "template('hosts/hosts.erb')"));
        baseClass.setModule(new PuppetModule(PuppetModule.TYPE_GROUP, "mongodb")
                .setProperty("ensure", "present"));
        baseClass.setModule(new PuppetModule(PuppetModule.TYPE_USER, "mongodb")
                .setProperty("ensure", "present")
                .setStringProperty("gid", "mongodb")
                .setStringProperty("shell", "/bin/bash")
                .setRequire(PuppetModule.TYPE_GROUP, "mongodb"));
        baseClass.setModule(new PuppetModule(PuppetModule.TYPE_EXEC, "apt-get update")
                .setStringProperty("command", "/usr/bin/apt-get update")
                .setNotify(PuppetModule.TYPE_EXEC, "mongodb-10gen")
                .setSubscribe(PuppetModule.TYPE_APT_SOURCE, "mongodb"));
        baseClass.setModule(new PuppetModule(PuppetModule.TYPE_APT_KEY, "mongodb")
                .setStringProperty("key", "7F0CEB10")
                .setStringProperty("key_server", "keyserver.ubuntu.com"));
        baseClass.setModule(new PuppetModule(PuppetModule.TYPE_APT_SOURCE, "mongodb")
                .setStringProperty("location", "http://downloads-distro.mongodb.org/repo/debian-sysvinit")
                .setStringProperty("release", "dist")
                .setStringProperty("repos", "10gen")
                .setStringProperty("key", "7F0CEB10")
                .setProperty("include_src", "false")
                .setStringProperty("key_server", "keyserver.ubuntu.com")
                .setNotify(PuppetModule.TYPE_EXEC, "apt-get update")
                .setRequire(PuppetModule.TYPE_APT_KEY, "mongodb"));
        baseClass.setModule(new PuppetModule(PuppetModule.TYPE_EXEC, "mongodb-10gen")
                .setStringProperty("command", "/usr/bin/apt-get install -y --force-yes mongodb-org-server mongodb-org-tools mongodb-org-shell")
                .setSubscribe(PuppetModule.TYPE_EXEC, "apt-get update")
                .setRequire(PuppetModule.TYPE_EXEC, "apt-get update")
                .setRequire(PuppetModule.TYPE_APT_SOURCE, "mongodb"));
        return baseClass.toString();
    }

    public static String getNodeStartupScriptContent(String serverName) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(PlayConfiguration.getFileContent("scripts/startup-common.sh"));
        sb.append("\nsetProxy ");
        sb.append(serverName);
        sb.append("\ncheckConnection http://http.debian.net/debian/dists/wheezy/Release.gpg\n");
        sb.append("installPackage mdadm\n");
        sb.append("installPackage xfsprogs\n");
        sb.append("installPackage btrfs-tools\n");
        sb.append("installPackage puppet\n");
        sb.append("echo \"[main]\n");
        sb.append("logdir=/var/log/puppet\nvardir=/var/lib/puppet\nssldir=/var/lib/puppet/ssl\n");
        sb.append("rundir=/var/run/puppet\nfactpath=$vardir/lib/facter\ntemplatedir=$confdir/templates\n");
        sb.append("prerun_command=");
        sb.append(PUPPET_HOME_DIR);
        sb.append("/etckeeper-commit-pre\npostrun_command=");
        sb.append(PUPPET_HOME_DIR);
        sb.append("/etckeeper-commit-post\nserver=");
        sb.append(serverName);
        sb.append("\nlisten=true\n\n[master]\n");
        sb.append("ssl_client_header = SSL_CLIENT_S_DN\nssl_client_verify_header = SSL_CLIENT_VERIFY\" > ");
        sb.append(PUPPET_HOME_DIR);
        sb.append("/puppet.conf\n\necho \"# Defaults for puppet - sourced by /etc/init.d/puppet\n\n");
        sb.append("# Start puppet on boot?\nSTART=yes\n\n");
        sb.append("# Startup options\nDAEMON_OPTS=\" > /etc/default/puppet\n\necho \"path /run\nallow ");
        sb.append(serverName);
        sb.append("\" > ");
        sb.append(PUPPET_HOME_DIR);
        sb.append("/auth.conf\n\npuppetd --test --waitforcert 60 --server ");
        sb.append(serverName);
        sb.append("\nservice puppet restart\n");
        return sb.toString();
    }

    public static String getPuppetStartupScriptContent(String clusterName, String networkRange, Integer processes,
                                                       String diskRaid, String dataFileSystem) throws IOException {
        if(networkRange == null || networkRange.isEmpty()) {
            throw new IllegalArgumentException("incorrect network range");
        }

        StringBuilder sb = new StringBuilder();
        sb.append(PlayConfiguration.getFileContent("scripts/startup-common.sh"));

        /**
         * Apache proxy configuration
         */
        sb.append("function configApache() {\n");
        sb.append("  if ! [ -e /etc/apache2/mods-enabled/proxy.load ]; then\n");
        sb.append("    ln -s /etc/apache2/mods-available/proxy.load /etc/apache2/mods-enabled/proxy.load\n");
        sb.append("    ln -s /etc/apache2/mods-available/proxy_http.load /etc/apache2/mods-enabled/proxy_http.load\n");
        sb.append("    echo \"ProxyRequests On\nProxyPreserveHost On\n\n<Proxy *>\n  Order deny,allow\n");
        sb.append("  Deny from all\n  Allow from ");
        sb.append(networkRange);
        sb.append("\n</Proxy>\" > /etc/apache2/conf.d/proxy\n");
        sb.append("  fi\n");
        sb.append("  service apache2 restart\n");
        sb.append("}\n");
        sb.append("installPackage apache2 configApache\n");

        /**
         * Puppet basic configuration
         */
        sb.append("function configPuppet() {\n");
        sb.append("  installPackage unzip\n");
        sb.append("  wget -O /tmp/puppet-timezone.zip https://github.com/saz/puppet-timezone/archive/master.zip\n");
        sb.append("  cd /tmp && unzip puppet-timezone.zip\n  cd puppet-timezone-master && puppet module build .\n");
        sb.append("  puppet module install pkg/saz-timezone-*.tar.gz\n  puppet module install puppetlabs-apt\n");
        sb.append("  makeDirectory ");
        sb.append(getPuppetTemplatesDirectory());
        sb.append("/hosts\n  cat /etc/hosts > ");
        sb.append(getPuppetTemplatesDirectory());
        sb.append("/hosts/hosts.erb\n");
        sb.append("  echo \"<%= ipaddress %> <%= fqdn %> <%= hostname %>\" >> ");
        sb.append(getPuppetTemplatesDirectory());
        sb.append("/hosts/hosts.erb\n");
        sb.append("  echo \"[files]\n  path ");
        sb.append(getPuppetFilesDirectory());
        sb.append("\n  allow *\n\n[plugins]\n\n\" > ");
        sb.append(PUPPET_HOME_DIR);
        sb.append("/fileserver.conf\n");
        sb.append("  echo \"autosign = true\" >> ");
        sb.append(PUPPET_HOME_DIR);
        sb.append("/puppet.conf\n");

        /**
         * Generate puppet manifest files
         */
        sb.append("  echo \"");
        sb.append(generateNodeManifest(clusterName));
        sb.append("\" > ");
        sb.append(getPuppetManifestsDirectory());
        sb.append("/site.pp\n");
        sb.append("  echo \"");
        try {
            sb.append(generateMongoBaseClassManifest());
        } catch(PuppetConfigurationException e) {}
        sb.append("\" > ");
        sb.append(getPuppetManifestsDirectory());
        sb.append("/mongodb-base.pp\n");
        sb.append("  echo \"");
        try {
            sb.append(generateMongoShardClassManifest(diskRaid, dataFileSystem));
        } catch(PuppetConfigurationException e) {}
        sb.append("\" > ");
        sb.append(getPuppetManifestsDirectory());
        sb.append("/mongodb-shard.pp\n");
        sb.append("  echo \"");
        try {
            sb.append(generateMongoConfClassManifest());
        } catch(PuppetConfigurationException e) {}
        sb.append("\" > ");
        sb.append(getPuppetManifestsDirectory());
        sb.append("/mongodb-conf.pp\n");

        /**
         * Generate puppet files
         */
        sb.append("  makeDirectory ");
        sb.append(getPuppetFilesDirectory());
        sb.append("\n  echo \"GRUB_DEFAULT=0\nGRUB_TIMEOUT=0\nGRUB_DISTRIBUTOR=\\\"Debian\\\"\n");
        sb.append("GRUB_CMDLINE_LINUX_DEFAULT=\\\"quiet\\\"\n");
        sb.append("GRUB_CMDLINE_LINUX=\\\"console=ttyS0,38400n8 cgroup_enable=memory swapaccount=1\\\"\n");
        sb.append("\" > ");
        sb.append(getPuppetFilesDirectory());
        sb.append("/grub\n");
        sb.append("  echo \"configsvr=true\ndbpath=/var/lib/mongodb\nlogpath=/var/log/mongodb/mongodb.log\n");
        sb.append("logappend=true\n\" > ");
        sb.append(getPuppetFilesDirectory());
        sb.append("/mongodb-config-server.conf\n");
        sb.append("  echo \"");
        sb.append(formatScriptForEcho(PlayConfiguration.getFileContent("scripts/puppet-disk-format.sh")));
        sb.append("\" > ");
        sb.append(getPuppetFilesDirectory());
        sb.append("/puppet-disk-format.sh\n");
        sb.append("  echo \"");
        sb.append(formatScriptForEcho(PlayConfiguration.getFileContent("scripts/puppet-mongodb-microshards.sh")));
        sb.append("\" > ");
        sb.append(getPuppetFilesDirectory());
        sb.append("/puppet-mongodb-microshards.sh\n");
        sb.append("  echo \"MOUNT_DIRECTORY=\\\"");
        sb.append(MONGODB_MOUNT_DIR);
        sb.append("\\\"\n");
        sb.append("PROCESSES=");
        sb.append(processes);
        sb.append("\n");
        if(diskRaid != null && !diskRaid.isEmpty()) {
            sb.append("RAID_TYPE=\\\"");
            sb.append(diskRaid.toLowerCase());
            sb.append("\\\"\n");
        }
        if(dataFileSystem != null && !dataFileSystem.isEmpty()) {
            sb.append("FS_TYPE=\\\"");
            sb.append(dataFileSystem.toLowerCase());
            sb.append("\\\"\n");
        }
        sb.append("\" > ");
        sb.append(getPuppetFilesDirectory());
        sb.append("/mongodb-shards.conf\n");
        sb.append("\n  service puppetmaster restart\n");

        sb.append("}\n");
        sb.append("installPackage puppetmaster configPuppet\n");
        return sb.toString();
    }

    private static String formatScriptForEcho(String scriptContent) {
        scriptContent = scriptContent.replace("\"", "\\\"");
        return scriptContent.replace("$", "\\$");
    }
}