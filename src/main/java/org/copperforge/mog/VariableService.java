package org.copperforge.mog;

public interface VariableService {

    String envsubst(String source);

    String varsubst(String source);

    String get(String key);

    MogVariable asVariable(String key);

    String timestamp();

}
