package utils.ycsb;

import services.ConfigurationService;
import utils.puppet.PuppetConfigurationException;

/**
 * Created by ricardolorenzo on 30/07/2014.
 */
public class TestConfiguration {

    public static String getNodeStartupScriptContent(String serverName) throws PuppetConfigurationException {
        String user = ConfigurationService.TEST_USER;
        if(user.contains("@")) {
            user = user.substring(0, user.lastIndexOf("@"));
        }

        StringBuilder sb = new StringBuilder();
        sb.append("#!/bin/bash\n\n");
        sb.append("export http_proxy=http://");
        sb.append(serverName);
        sb.append("\necho \"Acquire::http::Proxy \\\"http://");
        sb.append(serverName);
        sb.append("\\\";\n\" > /etc/apt/apt.conf.d/90proxy\n\n");
        sb.append("for i in $(seq 1 1 100); do\n");
        sb.append("  wget -O /dev/null http://");
        sb.append(serverName);
        sb.append(" http://http.debian.net/debian/dists/wheezy/Release.gpg;\n  if [ \"$?\" -eq 0 ]; then\n");
        sb.append("    break;\nfi\n  sleep 1;\ndone\nsleep 4\n");
        sb.append("if [ -z \"$(dpkg -l | grep openjdk-7-jdk)\" ]; then\n");
        sb.append("  apt-get update && apt-get install -o DPkg::options::=\"--force-confdef\"");
        sb.append(" -o DPkg::options::=\"--force-confold\" -o Dpkg::Options::=\"--force-overwrite\" -y openjdk-7-jdk\nfi\n\n");

        sb.append("PKG=$(dpkg -l | grep \" git \")\n");
        sb.append("if [ -z \"$PKG\" ]; then\n");
        sb.append("  apt-get update && apt-get install -o DPkg::options::=\"--force-confdef\"");
        sb.append(" -o DPkg::options::=\"--force-confold\" -o Dpkg::Options::=\"--force-overwrite\" -y git\nfi\n\n");

        sb.append("PKG=$(dpkg -l | grep \" maven \")\n");
        sb.append("if [ -z \"$PKG\" ]; then\n");
        sb.append("  apt-get update && apt-get --no-install-recommends install -o DPkg::options::=\"--force-confdef\"");
        sb.append(" -o DPkg::options::=\"--force-confold\" -o Dpkg::Options::=\"--force-overwrite\" -y maven\nfi\n\n");

        /**
         * An assumption here, is the fact that the SSH user is created
         */
        sb.append("cd /home/");
        sb.append(user);
        sb.append("\ngit config --global http.proxy http://");
        sb.append(serverName);
        sb.append("\ngit clone http://github.com/RicardoLorenzo/YCSB.git\n");
        sb.append("cd YCSB\nmvn package -Dmaven.test.skip=true\n");
        sb.append("chown ");
        sb.append(user);
        sb.append(":");
        sb.append(user);
        sb.append(" ../YCSB -R\n");
        return sb.toString();
    }

    public static String getJumpServerStartupScriptContent(String networkRange) {
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

        return sb.toString();
    }
}
