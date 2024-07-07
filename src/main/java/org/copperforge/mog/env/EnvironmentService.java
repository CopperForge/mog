package org.copperforge.mog.env;

import org.copperforge.mog.MogVariable;

public interface EnvironmentService {

    String envsubst(String source);

    String get(String key);

    MogVariable asVariable(String key);

}
