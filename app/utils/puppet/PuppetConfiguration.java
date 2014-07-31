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

import utils.puppet.disk.PuppetDiskConfiguration;

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
    private static final String PUPPET_HOME_DIR = "/etc/puppet";
    private static final String PUPPET_MANIFESTS_DIR = "manifests";
    private static final String PUPPET_FILES_DIR = "files";
    private static final String PUPPET_TEMPLATES_DIR = "templates";

    static {
        SUPPORTED_FILESYSTEMS = new ArrayList<>();
        SUPPORTED_FILESYSTEMS.add("ext4");
        SUPPORTED_FILESYSTEMS.add("xfs");
        SUPPORTED_FILESYSTEMS.add("btrfs");
        DISK_PER_PROCESS = new PuppetDiskConfiguration("disk per process", "One process per disk, processes not share the disk");
        DISK_RAID0 = new PuppetDiskConfiguration("raid0", "RAID0, all processes share the same disk pool");
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

    public static String generateMongoConfClassManifest() {
        StringBuilder sb = new StringBuilder();
        sb.append("class mongodb-conf {\n");
        sb.append("  service { 'mongodb':\n    ensure => running,\n    enable => true,\n");
        sb.append("    subscribe => File['/etc/mongodb.conf']\n  }\n\n");
        sb.append("  file { '/etc/mongodb.conf':\n    notify => Service['mongodb'],\n    owner => 'root',\n");
        sb.append("    group => 'root',\n    mode => 644,\n    source => 'puppet:///files/mongodb-configsrv.conf',\n");
        sb.append("    require => Exec['mongodb-10gen']\n  }\n");
        sb.append("}\n\n");
        sb.toString();
        return sb.toString();
    }

    public static String generateMongoShardClassManifest() {
        /**
         * TODO the approach is based on microshards init script
         */
        StringBuilder sb = new StringBuilder();
        sb.append("class mongodb-shard {\n");
        sb.append("  file { '/etc/init.d/mongodb-microshards':\n    owner => 'root',\n    group => 'root',\n");
        sb.append("    mode => 755,\n    source => 'puppet:///files/mongodb-microshards',\n  }\n\n");
        sb.append("  file { '/etc/default/grub':\n    notify  => Exec['update-grub'],\n    owner => 'root',\n");
        sb.append("    group => 'root',\n    mode => 644,\n    source => 'puppet:///files/grub',\n");
        sb.append("    require => Exec['mongodb-10gen']\n  }\n\n");
        sb.append("  service { 'mongodb':\n    ensure => stopped,\n    enable => false,\n");
        sb.append("    require => File['/etc/init.d/mongodb-microshards']\n  }\n\n");
        sb.append("#\n#  WARNING: This is a workaround to enable cgroups kernel module. This workaround imply\n");
        sb.append("#  a node restart, but happen only once.\n");
        sb.append("  exec { 'test-mongodb-microshards':\n    command => '/usr/bin/test 0',\n");
        sb.append("    onlyif => \\\"/usr/bin/test 1 -eq \\$(cat /proc/cgroups |");
        sb.append("grep memory | awk '{ print $4 }')\\\",\n");
        sb.append("    require => [\n                 Package['cgroup-bin'],\n");
        sb.append("                 Service['mongodb']\n               ]\n  }\n \n");
        sb.append("  service { 'mongodb-microshards':\n    ensure => running,\n    enable => true,\n");
        sb.append("    require => [\n                  File['/etc/init.d/mongodb-microshards'],\n");
        sb.append("                  Exec['test-mongodb-microshards']\n               ]\n  }\n\n");
        sb.append("  package { 'cgroup-bin':\n    ensure => present\n  }\n\n");
        sb.append("  exec { 'update-grub':\n    notify  => Exec['reboot'],\n    command => '/usr/sbin/update-grub',\n");
        sb.append("    subscribe => File['/etc/default/grub'],\n    refreshonly => true\n  }\n\n");
        sb.append("  exec { 'reboot':\n    command => '/sbin/reboot',\n    subscribe => Exec['update-grub'],\n");
        sb.append("    refreshonly => true\n  }\n");
        sb.append("}\n\n");
        return sb.toString();
    }

    public static String generateMongoBaseClassManifest() {
        StringBuilder sb = new StringBuilder();
        sb.append("class mongodb-base {\n");
        sb.append("  class {\n 'timezone':\n   timezone => 'UTC'\n  }\n\n");
        sb.append("  package { 'ntp':\n    ensure => present\n  }\n\n");
        sb.append("  service { 'ntp':\n    ensure => running,\n    enable => true\n  }\n\n");
        sb.append("  file { '/etc/hosts':\n    owner => 'root',\n    group => 'root',\n");
        sb.append("    mode => 644,\n    content => template('hosts/hosts.erb')\n  }\n\n");
        sb.append("  group { 'mongodb':\n    ensure => present\n  }\n\n");
        sb.append("  user { 'mongodb':\n    ensure => present,\n    gid => 'mongodb',\n");
        sb.append("    shell => '/bin/bash',\n    require => Group['mongodb']\n  }\n\n");
        sb.append("#\n#  WARNING: Is important to perform an apt-get update before try to install\n");
        sb.append("#  any package.\n");
        sb.append("  exec { 'apt-get update':\n    notify => Exec['mongodb-10gen'],\n");
        sb.append("    command => '/usr/bin/apt-get update',\n    subscribe => Apt::Source['mongodb']\n  }\n\n");
        sb.append("  apt::key { 'mongodb':\n    key => '7F0CEB10',\n    key_server => 'keyserver.ubuntu.com'\n  }\n\n");
        sb.append("  apt::source { 'mongodb':\n    notify => Exec['apt-get update'],\n");
        sb.append("    location => 'http://downloads-distro.mongodb.org/repo/debian-sysvinit',\n");
        sb.append("    release => 'dist',\n    repos => '10gen',\n    key => '7F0CEB10',\n");
        sb.append("    key_server => 'keyserver.ubuntu.com',\n    include_src => false,\n");
        sb.append("    require => Apt::Key['mongodb']\n  }\n");
        sb.append("#\n#  WARNING: Cannot use 'package' because of the 10gen repo, so it needs to trigger\n");
        sb.append("#  an 'apt-get update' before.\n");
        sb.append("  exec { 'mongodb-10gen':\n");
        sb.append("    command => '/usr/bin/apt-get install -y --force-yes mongodb-org-server mongodb-org-tools',\n");
        sb.append("    subscribe => Exec['apt-get update'],\n    require => Exec['apt-get update']\n  }\n");
        sb.append("}\n\n");
        return sb.toString();
    }

    public static String getNodeStartupScriptContent(String serverName) throws PuppetConfigurationException {
        StringBuilder sb = new StringBuilder();
        sb.append("#!/bin/bash\n\n");
        sb.append("export http_proxy=http://");
        sb.append(serverName);
        sb.append("\necho \"Acquire::http::Proxy \\\"http://");
        sb.append(serverName);
        sb.append("\\\";\n\" > /etc/apt/apt.conf.d/90proxy\n\n");
        sb.append("for i in $(seq 1 1 100); do\n");
        sb.append("  wget -O /dev/null");
        sb.append(" http://http.debian.net/debian/dists/wheezy/Release.gpg;\n  if [ \"$?\" -eq 0 ]; then\n");
        sb.append("    break;\nfi\n  sleep 1;\ndone\nsleep 4\n");
        sb.append("if [ -z \"$(dpkg -l | grep puppet)\" ]; then\n");
        sb.append("  apt-get update && apt-get install -o DPkg::options::=\"--force-confdef\"");
        sb.append(" -o DPkg::options::=\"--force-confold\" -o Dpkg::Options::=\"--force-overwrite\" -y puppet\nfi\n\n");
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

    public static String getPuppetStartupScriptContent(String clusterName, String networkRange) throws PuppetConfigurationException {
        if(networkRange == null || networkRange.isEmpty()) {
            throw new IllegalArgumentException("incorrect network range");
        }

        //IpNetworkCalculator ipcalc = new IpNetworkCalculator();

        StringBuilder sb = new StringBuilder();
        sb.append("#!/bin/bash\n\n");
        sb.append("# WARNING: Update repositories before start\n");
        sb.append("apt-get update\n");

        /**
         * Apache proxy configuration
         */
        sb.append("if [ -z \"$(dpkg -l | grep apache2)\" ]; then\n");
        sb.append("  apt-get -y install apache2 \n");
        sb.append("  if ! [ -e /etc/apache2/mods-enabled/proxy.load ]; then\n");
        sb.append("    ln -s /etc/apache2/mods-available/proxy.load /etc/apache2/mods-enabled/proxy.load\n");
        sb.append("    ln -s /etc/apache2/mods-available/proxy_http.load /etc/apache2/mods-enabled/proxy_http.load\n");
        sb.append("    echo \"ProxyRequests On\nProxyPreserveHost On\n\n<Proxy *>\n  Order deny,allow\n");
        sb.append("  Deny from all\n  Allow from ");
        sb.append(networkRange);
        sb.append("\n</Proxy>\" > /etc/apache2/conf.d/proxy\n");
        sb.append("  fi\n");
        sb.append("  service apache2 restart\n");
        sb.append("fi\n");

        /**
         * Puppet basic configuration
         */
        sb.append("if [ -z \"$(dpkg -l | grep puppetmaster)\" ]; then\n");
        sb.append("  if [ -z \"$(dpkg -l | grep unzip)\" ]; then\n    apt-get -y install unzip\n  fi\n");
        sb.append("  apt-get -y install puppetmaster\n");
        sb.append("  wget -O /tmp/puppet-timezone.zip https://github.com/saz/puppet-timezone/archive/master.zip\n");
        sb.append("  cd /tmp && unzip puppet-timezone.zip\n  cd puppet-timezone-master && puppet module build .\n");
        sb.append("  puppet module install pkg/saz-timezone-*.tar.gz\n  puppet module install puppetlabs-apt\n");
        sb.append("  mkdir -p ");
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
        sb.append(generateMongoBaseClassManifest());
        sb.append("\" > ");
        sb.append(getPuppetManifestsDirectory());
        sb.append("/mongodb-base.pp\n");
        sb.append("  echo \"");
        sb.append(generateMongoShardClassManifest());
        sb.append("\" > ");
        sb.append(getPuppetManifestsDirectory());
        sb.append("/mongodb-shard.pp\n");
        sb.append("  echo \"");
        sb.append(generateMongoConfClassManifest());
        sb.append("\" > ");
        sb.append(getPuppetManifestsDirectory());
        sb.append("/mongodb-conf.pp\n");

        sb.append("  if ! [ -e ");
        sb.append(getPuppetFilesDirectory());
        sb.append(" ]; then\n    mkdir -p ");
        sb.append(getPuppetFilesDirectory());
        sb.append("\n  fi\n");
        sb.append("  echo \"GRUB_DEFAULT=0\nGRUB_TIMEOUT=0\nGRUB_DISTRIBUTOR=\\\"Debian\\\"\n");
        sb.append("GRUB_CMDLINE_LINUX_DEFAULT=\\\"quiet\\\"\n");
        sb.append("GRUB_CMDLINE_LINUX=\\\"console=ttyS0,38400n8 cgroup_enable=memory swapaccount=1\\\"\n");
        sb.append("\" > ");
        sb.append(getPuppetFilesDirectory());
        sb.append("/grub\n");
        sb.append("  echo \"configsvr=true\ndbpath=/var/lib/mongodb\nlogpath=/var/log/mongodb/mongodb.log\n");
        sb.append("logappend=true\n\" > ");
        sb.append(getPuppetFilesDirectory());
        sb.append("/mongodb-configsrv.conf");
        sb.append("  wget -O ");
        sb.append(getPuppetFilesDirectory());
        sb.append("/mongodb-microshards http://storage.googleapis.com/");
        sb.append("peta-mongo/autostart/mongodb-microshards\n\n  service puppetmaster restart\nfi");

        return sb.toString();
    }
}