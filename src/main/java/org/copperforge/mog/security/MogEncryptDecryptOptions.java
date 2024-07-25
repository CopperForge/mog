package org.copperforge.mog.security;

import org.copperforge.mog.MogOptions;

import picocli.CommandLine;
import picocli.CommandLine.Option;

public class MogEncryptDecryptOptions extends MogOptions {

    @Option(names = "--value", description = "the value to encrypt/decrypt")
    private String value;

    @Option(names = "--password", description = "the password to use during encryption")
    private String password;

    public static MogEncryptDecryptOptions parse(MogOptions options) {
        MogEncryptDecryptOptions encryptOptions = new MogEncryptDecryptOptions();
        new CommandLine(encryptOptions).setUnmatchedArgumentsAllowed(true)
                .parseArgs(options.rawArgs().toArray(new String[0]));
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
