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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        result = prime * result + ((password == null) ? 0 : password.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        MogEncryptDecryptOptions other = (MogEncryptDecryptOptions) obj;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        if (password == null) {
            if (other.password != null)
                return false;
        } else if (!password.equals(other.password))
            return false;
        return true;
    }

}
