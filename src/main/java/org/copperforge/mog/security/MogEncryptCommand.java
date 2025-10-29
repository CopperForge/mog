package org.copperforge.mog.security;

import org.copperforge.mog.MogException;
import org.copperforge.mog.MogOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MogEncryptCommand {

    private Logger log = LoggerFactory.getLogger(MogEncryptCommand.class);
    private MogSecurityService securityService;

    public String encrypt(MogOptions mogOptions) throws MogException {
        MogEncryptDecryptOptions options = MogEncryptDecryptOptions.parse(mogOptions);
        String encrypted = securityService.encryptor().encrypt(options.getValue());
        if (log.isTraceEnabled()) log.trace(String.format("encrypted = '%s'", encrypted));
        return encrypted;
    }

    public String decrypt(MogOptions mogOptions) throws MogException {
        MogEncryptDecryptOptions options = MogEncryptDecryptOptions.parse(mogOptions);
        String decrypted = securityService.decryptor().decrypt(options.getValue());
        if (log.isTraceEnabled()) log.trace(String.format("decrypted = '%s'", decrypted));
        return decrypted;
    }

}
