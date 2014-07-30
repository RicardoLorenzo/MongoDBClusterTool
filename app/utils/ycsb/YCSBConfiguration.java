package utils.ycsb;

import utils.puppet.PuppetConfigurationException;

/**
 * Created by ricardolorenzo on 30/07/2014.
 */
public class YCSBConfiguration {

    public static String getNodeStartupScriptContent(String serverName) throws PuppetConfigurationException {
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

        sb.append("if [ -z \"$(dpkg -l | grep git)\" ]; then\n");
        sb.append("  apt-get update && apt-get install -o DPkg::options::=\"--force-confdef\"");
        sb.append(" -o DPkg::options::=\"--force-confold\" -o Dpkg::Options::=\"--force-overwrite\" -y git\nfi\n\n");

        sb.append("if [ -z \"$(dpkg -l | grep maven)\" ]; then\n");
        sb.append("  apt-get update && apt-get --no-install-recommends install -o DPkg::options::=\"--force-confdef\"");
        sb.append(" -o DPkg::options::=\"--force-confold\" -o Dpkg::Options::=\"--force-overwrite\" -y maven\nfi\n\n");

        sb.append("cd /home/mongodb");
        sb.append("git clone https://github.com/RicardoLorenzo/YCSB.git\n");
        sb.append("mvn package -Dmaven.test.skip=true\n");
        return sb.toString();
    }
}
