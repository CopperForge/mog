package org.copperforge.mog.config;

import java.util.List;
import java.util.Map;

public class Mogf {

    private String encryptionPassword;
    private List<Map<String, String>> apiTokens;

    public String getEncryptionPassword() {
        return encryptionPassword;
    }

    public void setEncryptionPassword(String encryptionPassword) {
        this.encryptionPassword = encryptionPassword;
    }

    public List<Map<String, String>> getApiTokens() {
        return apiTokens;
    }

    public void setApiTokens(List<Map<String, String>> apiTokens) {
        this.apiTokens = apiTokens;
    }

    
}
