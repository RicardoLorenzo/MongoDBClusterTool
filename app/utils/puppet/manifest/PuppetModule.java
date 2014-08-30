package utils.puppet.manifest;

import utils.puppet.PuppetConfigurationException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by ricardolorenzo on 03/08/2014.
 */
public class PuppetModule {
    public static final String TYPE_CLASS = "Class";
    public static final String TYPE_FILE = "File";
    public static final String TYPE_EXEC = "Exec";
    public static final String TYPE_SERVICE = "Service";
    public static final String TYPE_PACKAGE = "Package";
    public static final String TYPE_USER = "User";
    public static final String TYPE_GROUP = "Group";
    public static final String TYPE_APT_KEY = "Apt::Key";
    public static final String TYPE_APT_SOURCE = "Apt::Source";
    public static final String TYPE_ULIMITS = "ulimit::rule";
    private String type;
    private String name;
    private Map<String, String> properties;
    private Map<String, String> requires;

    public PuppetModule(String type, String name) throws PuppetConfigurationException {
        checkSupported(type);
        this.type = type;
        this.name = name;
        properties = new HashMap<>();
        requires = new HashMap<>();
    }

    private static void checkSupported(String type) throws PuppetConfigurationException {
        switch(type) {
            case TYPE_CLASS:
            case TYPE_FILE:
            case TYPE_EXEC:
            case TYPE_SERVICE:
            case TYPE_PACKAGE:
            case TYPE_USER:
            case TYPE_GROUP:
            case TYPE_APT_KEY:
            case TYPE_APT_SOURCE:
            case TYPE_ULIMITS:
                // is valid type
                break;
            default:
                throw new PuppetConfigurationException("unsupported module type");
        }
    }

    private static String getModuleString(String type, String name) {
        name = name.trim();
        StringBuilder sb = new StringBuilder();
        sb.append(type);
        sb.append("[");
        if(!name.startsWith("'")) {
            sb.append("'");
        }
        sb.append(name);
        if(!name.endsWith("'")) {
            sb.append("'");
        }
        sb.append("]");
        return sb.toString();
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    protected Set<Map.Entry<String, String>> getProperties() {
        return this.properties.entrySet();
    }

    public PuppetModule setProperty(String name, String value) {
        properties.put(name, value);
        return this;
    }

    public PuppetModule setNotify(String type, String name) throws PuppetConfigurationException {
        checkSupported(type);
        setProperty("notify", getModuleString(type, name));
        return this;
    }

    public PuppetModule setRequire(String type, String name) throws PuppetConfigurationException {
        checkSupported(type);
        this.requires.put(type, name);
        return this;
    }

    public PuppetModule setSubscribe(String type, String name) throws PuppetConfigurationException {
        checkSupported(type);
        setProperty("subscribe", getModuleString(type, name));
        return this;
    }

    public PuppetModule setStringProperty(String name, String value) {
        if(value.contains("'")) {
            value = value.replace("'","\\\'");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("'");
        sb.append(value);
        sb.append("'");
        setProperty(name, sb.toString());
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("  ");
        sb.append(this.type.toLowerCase());
        sb.append(" { '");
        sb.append(this.name);
        sb.append("':");
        int count = this.properties.size() - 1;
        for(Map.Entry<String, String> e : this.properties.entrySet()) {
            sb.append("\n    ");
            sb.append(e.getKey());
            sb.append(" => ");
            sb.append(e.getValue());
            if(count > 0) {
                sb.append(",");
            }
            count--;
        }
        if(this.requires.size() > 0) {
            sb.append(",\n    require => ");
            if(this.requires.size() > 1) {
                sb.append("[");
            }
            count = this.requires.size() - 1;
            for(Map.Entry<String, String> e : this.requires.entrySet()) {
                sb.append("\n                 ");
                sb.append(getModuleString(e.getKey(), e.getValue()));
                if(count > 0) {
                    sb.append(",");
                }
                count--;
            }
            if(this.requires.size() > 1) {
                sb.append("\n               ]");
            }
        }
        sb.append("\n  }\n");
        return sb.toString();
    }
}
