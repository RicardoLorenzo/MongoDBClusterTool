package utils.test;

import conf.PlayConfiguration;
import services.ConfigurationService;

import java.io.IOException;

/**
 * Created by ricardolorenzo on 30/07/2014.
 */
public class TestConfiguration {
    private static final String YCSB_REPOSITORY = "https://github.com/RicardoLorenzo/YCSB.git";
    public static final String YCSB_DIRECTORY = "YCSB";

    public static String getNodeStartupScriptContent(String serverName) throws IOException {
        String user = ConfigurationService.TEST_USER;
        if(user.contains("@")) {
            user = user.substring(0, user.lastIndexOf("@"));
        }
        StringBuilder homeDirectory = new StringBuilder();
        homeDirectory.append("/home/");
        homeDirectory.append(user);
        homeDirectory.append("/");
        homeDirectory.append(YCSB_DIRECTORY);

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
        sb.append("/");
        sb.append(YCSB_DIRECTORY);
        sb.append(" -R\n");
        sb.append("  runCommandAsUser ");
        sb.append(user);
        sb.append(" sed -i '/<module>mapkeeper<\\/module>/d' pom.xml\n");
        sb.append("  runCommandAsUser ");
        sb.append(user);
        sb.append(" \"cd ");
        sb.append(homeDirectory.toString());
        sb.append("/");
        sb.append(YCSB_DIRECTORY);
        sb.append(" && mvn clean package -Dmaven.test.skip=true -DproxySet=true -DproxyHost=");
        sb.append(serverName);
        sb.append(" -DproxyPort=80\"\n");
        sb.append("fi");
        return sb.toString();
    }

    public static String getJumpServerStartupScriptContent(String networkRange) throws IOException {
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

        return sb.toString();
    }
}
