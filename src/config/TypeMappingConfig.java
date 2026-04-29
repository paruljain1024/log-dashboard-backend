package config;

import java.util.HashMap;
import java.util.Map;

/**
 * This class maps LOG type keys to user-friendly names.
 * In future you can load from config.properties also.
 */
public class TypeMappingConfig {

    private final Map<String, String> typeToDisplayName = new HashMap<>();

    public TypeMappingConfig() {
        // default mapping
        typeToDisplayName.put("EXRCTRFREQ", "Recharge Request");
        typeToDisplayName.put("BALREQ", "Balance Request");
        typeToDisplayName.put("LASTTRFREQ", "Last Transaction Request");
        typeToDisplayName.put("C2CTRFREQ", "C2C Transfer Request");
        typeToDisplayName.put("O2CTRFREQ", "O2C Transfer Request");
    }

    public String getDisplayName(String typeKey) {
        return typeToDisplayName.getOrDefault(typeKey, typeKey);
    }

    public Map<String, String> getAllMappings() {
        return typeToDisplayName;
    }

    public void overrideMapping(String typeKey, String displayName) {
        typeToDisplayName.put(typeKey, displayName);
    }
}
