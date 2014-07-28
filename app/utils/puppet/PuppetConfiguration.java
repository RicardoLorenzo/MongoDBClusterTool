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
    public static final Integer PUPPET_MANIFEST = 1;
    public static final Integer PUPPET_FILE = 2;
    private static String PUPPET_HOME = "/etc/puppet";

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

    public static String getNodeStartupScriptContent(String serverName) throws PuppetConfigurationException {
        StringBuilder sb = new StringBuilder();
        sb.append("#!/bin/bash\n\n");
        sb.append("echo \"Acquire::http::Proxy \\\"http://");
        sb.append(serverName);
        sb.append("\\\";\n\" > /etc/apt/apt.conf.d/90proxy\n\n");
        sb.append("for i in $(seq 1 1 100); do\n");
        sb.append("  wget http://");
        sb.append(serverName);
        sb.append(";\n  if [ \"$?\" -eq 0 ]; then\n");
        sb.append("    break;\nfi\n  sleep 1;\ndone\n");
        sb.append("if [ -z \"$(dpkg -l | grep puppet)\" ]; then\n");
        sb.append("  apt-get update && apt-get install -y puppet\nfi\n\n");
        sb.append("echo \"[main]\n");
        sb.append("logdir=/var/log/puppet\n");
        sb.append("vardir=/var/lib/puppet\n");
        sb.append("ssldir=/var/lib/puppet/ssl\n");
        sb.append("rundir=/var/run/puppet\n");
        sb.append("factpath=$vardir/lib/facter\n");
        sb.append("templatedir=$confdir/templates\n");
        sb.append("prerun_command=/etc/puppet/etckeeper-commit-pre\n");
        sb.append("postrun_command=/etc/puppet/etckeeper-commit-post\n");
        sb.append("server=mongodb-puppetmaster.c.peta-mongo.internal\n");
        sb.append("listen=true\n\n");
        sb.append("[master]\n");
        sb.append("ssl_client_header = SSL_CLIENT_S_DN\n");
        sb.append("ssl_client_verify_header = SSL_CLIENT_VERIFY\" > /etc/puppet/puppet.conf\n\n");
        sb.append("echo \"# Defaults for puppet - sourced by /etc/init.d/puppet\n\n");
        sb.append("# Start puppet on boot?\n");
        sb.append("START=yes\n\n");
        sb.append("# Startup options\n");
        sb.append("DAEMON_OPTS=\" > /etc/default/puppet\n\n");
        sb.append("echo \"path /run\n");
        sb.append("allow ");
        sb.append(serverName);
        sb.append(" > /etc/puppet/auth.conf\n\n");
        sb.append("puppetd --test --waitforcert 60 --server ");
        sb.append(serverName);
        sb.append("\nservice puppet restart\n");

        return sb.toString();
    }

    public static String getPuppetStartupScriptContent(String serverName, String networkRange) throws PuppetConfigurationException {
        if(networkRange == null || networkRange.isEmpty()) {
            throw new IllegalArgumentException("incorrect network range");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("#!/bin/bash\n\n");

        /**
         * Apache proxy connfiguration
         */
        sb.append("if [ -z \"$(dpkg -l | grep apache2)\" ]; then\n");
        sb.append("  apt-get -y install apache2 \n");
        sb.append("  if ! [ -e /etc/apache2/mods-enabled/proxy.load ]; then\n");
        sb.append("    ln -s /etc/apache2/mods-available/proxy.load /etc/apache2/mods-enabled/proxy.load\n");
        sb.append("    ln -s /etc/apache2/mods-available/proxy_http.load /etc/apache2/mods-enabled/proxy_http.load\n");
        sb.append("    echo \"ProxyRequests On\nProxyPreserveHost On\n\n<Proxy *>\n  Order deny,allow\n");
        sb.append("  Deny from all\n  Allow from ");
        sb.append(networkRange);
        sb.append("\n</Proxy>\" > /etc/apache2/sites-available/proxy");
        sb.append("  fi\n");
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
        sb.append("  mkdir -p /etc/puppet/templates/hosts\n  cat /etc/hosts > /etc/puppet/templates/hosts.erb\n");
        sb.append("  echo \"<%= ipaddress %> <%= fqdn %> <%= hostname %>\" >> /etc/puppet/templates/hosts/hosts.erb\n");
        sb.append("  echo \"[files]\n  path /etc/puppet/files\n  allow *\n\n[plugins]\n\n\"");
        sb.append("> /etc/puppet/fileserver.conf\n");
        sb.append("  echo \"autosign = true\" >> /etc/puppet/puppet.conf\n");
        /**
         * TODO Puppet main.pp configuration into /etc/puppet/manifests/site.pp
         */
        sb.append("  if ! [ -e /etc/puppet/files ]; then\n    mkdir -p /etc/puppet/files\n  fi\n");
        sb.append("  echo \"GRUB_DEFAULT=0\nGRUB_TIMEOUT=0\nGRUB_DISTRIBUTOR=\\\"Debian\\\"\n");
        sb.append("GRUB_CMDLINE_LINUX_DEFAULT=\\\"quiet\\\"\n");
        sb.append("GRUB_CMDLINE_LINUX=\\\"console=ttyS0,38400n8 cgroup_enable=memory swapaccount=1\\\"\n");
        sb.append("\" > /etc/puppet/files/grub\n");
        sb.append("  echo \"configsvr=true\ndbpath=/var/lib/mongodb\nlogpath=/var/log/mongodb/mongodb.log\n");
        sb.append("logappend=true\n\" > /etc/puppet/files/mongodb-configsrv.conf");
        sb.append("  wget -O /etc/puppet/files/mongodb-microshards http://storage.googleapis.com/");
        sb.append("peta-mongo/autostart/mongodb-microshards\n\n  service puppetmaster restart\nfi");

        return sb.toString();
    }
}