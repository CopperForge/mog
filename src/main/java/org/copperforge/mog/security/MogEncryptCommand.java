package org.copperforge.mog.security;

import org.copperforge.mog.MogException;
import org.copperforge.mog.MogOptions;
import org.copperforge.mog.annotations.MogCommand;
import org.copperforge.mog.annotations.Moglet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MogCommand
public class MogEncryptCommand {

    private Logger log = LoggerFactory.getLogger(MogEncryptCommand.class);

    @Moglet
    private MogSecurityService securityService;

    @MogCommand(name="encrypt", description = "encrypt the given value")
    public String encrypt(MogOptions mogOptions) throws MogException {
        MogEncryptDecryptOptions options = MogEncryptDecryptOptions.parse(mogOptions);
        String encrypted = securityService.encryptor().encrypt(options.getValue());
        if (log.isTraceEnabled()) log.trace(String.format("encrypted = '%s'", encrypted));
        return encrypted;
    }

    @MogCommand(name = "decrypt", description = "decrypt the given value")
    public String decrypt(MogOptions mogOptions) throws MogException {
        MogEncryptDecryptOptions options = MogEncryptDecryptOptions.parse(mogOptions);
        String decrypted = securityService.decryptor().decrypt(options.getValue());
        if (log.isTraceEnabled()) log.trace(String.format("decrypted = '%s'", decrypted));
        return decrypted;
    }

}
