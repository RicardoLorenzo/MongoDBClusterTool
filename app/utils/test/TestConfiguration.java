package utils.test;

import conf.PlayConfiguration;
import services.ConfigurationService;
import utils.gce.GoogleComputeEngineException;

import java.io.IOException;
import java.util.List;

/**
 * Created by ricardolorenzo on 30/07/2014.
 */
public class TestConfiguration {
    private static final String YCSB_REPOSITORY = "https://github.com/RicardoLorenzo/YCSB.git";
    private static final Integer MONGODB_SHARD_INIT_PORT = 27040;
    public static final String YCSB_DIRECTORY = "YCSB";

    public static String getNodeStartupScriptContent(String serverName, String remoteScriptUrl) throws IOException, GoogleComputeEngineException {
        StringBuilder sb = new StringBuilder();
        sb.append(PlayConfiguration.getFileContent("scripts/startup-common.sh"));
        sb.append("\nsetProxy ");
        sb.append(serverName);
        sb.append("\ncheckConnection http://http.debian.net/debian/dists/wheezy/Release.gpg\n");
        sb.append("installPackage openjdk-7-jdk\n");
        sb.append("installPackage git\n");
        sb.append("installPackage maven\n");
        sb.append("runCommand apt-key adv --keyserver keyserver.ubuntu.com --recv 7F0CEB10\n");
        sb.append("echo \"deb http://downloads-distro.mongodb.org/repo/debian-sysvinit dist 10gen\" >");
        sb.append(" /etc/apt/sources.list.d/mongodb.list\n");
        sb.append("installPackage mongodb-org-mongos\n");
        sb.append("installPackage mongodb-org-shell\n\n");

        sb.append("if ! [ -e \"/etc/mongodbconfig.sh\" ]; then\n");
        sb.append("  checkConnection ");
        sb.append(remoteScriptUrl);
        sb.append("\n  wget -O /etc/mongodbconfig.sh ");
        sb.append(remoteScriptUrl);
        sb.append("\n");
        sb.append("  bash /etc/mongodbconfig.sh\n");
        sb.append("fi\n");

        return sb.toString();
    }

    public static String getNodeRemoteStartupScriptContent(String serverName, List<String> configNodeNames,
                                                     List<String> shardNodeNames)
            throws IOException, GoogleComputeEngineException {
        Integer shardPorcesses = ConfigurationService.getClusterNodeProcesses();
        String user = ConfigurationService.TEST_USER;
        if(user.contains("@")) {
            user = user.substring(0, user.lastIndexOf("@"));
        }
        StringBuilder homeDirectory = new StringBuilder();
        homeDirectory.append("/home/");
        homeDirectory.append(user);

        StringBuilder sb = new StringBuilder();
        sb.append(PlayConfiguration.getFileContent("scripts/startup-common.sh"));
        /**
         * An assumption here, is the fact that the SSH user is created,
         * and it is normally
         */
        sb.append("if ! [ -e \"");
        sb.append(homeDirectory.toString());
        sb.append("/");
        sb.append(YCSB_DIRECTORY);
        sb.append("\" ]; then\n");
        sb.append("  checkConnection http://");
        sb.append(serverName);
        sb.append("/YCSB.git/info/refs\n");
        sb.append("  runCommandAsUser ");
        sb.append(user);
        sb.append(" git clone http://");
        sb.append(serverName);
        sb.append("/YCSB.git\n");
        sb.append("  makeDirectory ");
        sb.append(homeDirectory.toString());
        sb.append("/.m2\n");
        sb.append("  echo \"<?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\"?>");
        sb.append("<settings xmlns=\\\"http://maven.apache.org/SETTINGS/1.0.0\\\"");
        sb.append(" xmlns:xsi=\\\"http://www.w3.org/2001/XMLSchema-instance\\\"");
        sb.append(" xsi:schemaLocation=\\\"http://maven.apache.org/SETTINGS/1.0.0 ");
        sb.append(" http://maven.apache.org/xsd/settings-1.0.0.xsd\\\">");
        sb.append("<proxies><proxy><active>true</active><protocol>http</protocol><host>");
        sb.append(serverName);
        sb.append("</host><port>80</port><username></username><password></password></proxy></proxies>");
        sb.append("</settings>\" > ");
        sb.append(homeDirectory.toString());
        sb.append("/.m2/settings.xml\n");
        sb.append("  chown ");
        sb.append(user);
        sb.append(":");
        sb.append(user);
        sb.append(" ");
        sb.append(homeDirectory.toString());
        sb.append("/.m2 -R\n");
        sb.append("  runCommandAsUser ");
        sb.append(user);
        sb.append(" \"sed -i '/<module>mapkeeper<\\/module>/d' ");
        sb.append(homeDirectory.toString());
        sb.append("/");
        sb.append(YCSB_DIRECTORY);
        sb.append("/pom.xml\"\n");
        sb.append("  runCommandAsUser ");
        sb.append(user);
        sb.append(" \"cd ");
        sb.append(homeDirectory.toString());
        sb.append("/");
        sb.append(YCSB_DIRECTORY);
        sb.append(" && mvn clean package -Dmaven.test.skip=true -DproxySet=true -DproxyHost=");
        sb.append(serverName);
        sb.append(" -DproxyPort=80\"\n");
        sb.append("fi\n");

        /**
         * Mongos configuration
         */
        StringBuilder configdb = new StringBuilder();
        sb.append("if ! [ -e \"/etc/mongos.sconf\" ]; then\n");
        sb.append("  echo \"port = 27017\nconfigdb = ");
        for(String confNode : configNodeNames) {
            if(configdb.length() > 0) {
                configdb.append(",");
            }
            configdb.append(confNode);
            configdb.append(":27019");
        }
        sb.append(configdb.toString());
        sb.append("\nlogpath = /var/log/mongos.log\nlogappend = yes\nfork = yes\n");
        sb.append("\" > /etc/mongos.conf\n");
        sb.append("fi\n");
        sb.append("ulimit -n 64000\n");
        sb.append("ulimit -u 64000\n");
        sb.append("mongos -f /etc/mongos.conf\n");

        /**
         * Shard cluster configuration
         */
        sb.append("if ! [ -e \"/etc/mongos.js\" ]; then\n");
        sb.append("  echo \"");
        for(String shardNode : shardNodeNames) {
            for(int port = MONGODB_SHARD_INIT_PORT; port < (MONGODB_SHARD_INIT_PORT + shardPorcesses); port++) {
                sb.append("sh.addShard('");
                sb.append(shardNode);
                sb.append(":");
                sb.append(port);
                sb.append("')\n");
            }
        }
        sb.append("\" > /etc/mongos.js\n");
        sb.append("fi\n");
        sb.append("mongo /etc/mongos.js\n");
        return sb.toString();
    }

    public static String getJumpServerStartupScriptContent(String networkRange, String testNodesStartupScriptUrl) throws IOException {
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
         * YCSB git repository mirror
         */
        sb.append("installPackage git\n");
        sb.append("git clone --mirror ");
        sb.append(YCSB_REPOSITORY);
        sb.append(" /var/www/YCSB.git\n");
        sb.append("PWD=$(pwd);\ncd /var/www/YCSB.git/\ngit update-server-info\ncd $PWD\n");

        /**
         * Startup script for test nodes
         */
        sb.append("wget -O /var/www/startup.sh \"");
        sb.append(testNodesStartupScriptUrl);
        sb.append("\"\n");

        return sb.toString();
    }
}
