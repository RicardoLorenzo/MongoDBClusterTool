package utils.puppet.manifest;

import utils.puppet.PuppetConfigurationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ricardolorenzo on 03/08/2014.
 */
public class PuppetClass {
    private String name;
    private Map<String, PuppetModule> modules;

    public PuppetClass(String name) {
        this.name = name;
        modules = new HashMap<>();
    }

    private static String getModuleKey(final String type, final String name) {
        StringBuilder sb = new StringBuilder();
        sb.append(type);
        sb.append("@");
        sb.append(name);
        return sb.toString();
    }

    public List<PuppetModule> getModules() {
        return new ArrayList<>(this.modules.values());
    }

    public void setModule(PuppetModule puppetModule) throws PuppetConfigurationException {
        String key = getModuleKey(puppetModule.getType(), puppetModule.getName());
        if(this.modules.containsKey(key)) {
            throw new PuppetConfigurationException("module name already exists for the type");
        }
        this.modules.put(key, puppetModule);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ");
        sb.append(this.name);
        sb.append(" {\n");
        for(PuppetModule m: modules.values()) {
            sb.append(m.toString());
        }
        sb.append("}\n");
        return sb.toString();
    }
}
