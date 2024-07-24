package org.copperforge.mog.security;

import org.copperforge.mog.MogOptions;

import picocli.CommandLine;
import picocli.CommandLine.Option;

public class MogEncryptOptions extends MogOptions {

    @Option(names = "--value", description = "the value to encrypt")
    private String value;

    @Option(names = "--password", description = "the password to use during encryption")
    private String password;

    public static MogEncryptOptions parse(MogOptions options) {
        MogEncryptOptions encryptOptions = new MogEncryptOptions();
        new CommandLine(encryptOptions).parseArgs(options.rawArgs().toArray(new String[0]));
        return encryptOptions;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
