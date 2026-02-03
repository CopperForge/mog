package org.copperforge.mog.security;

import org.copperforge.mog.MogException;
import org.copperforge.mog.runtime.MogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MogSecurityService {

    private Logger log = LoggerFactory.getLogger(MogSecurityService.class);

    final MogAESEncryptorDecryptor encryptorDecryptor;

    public MogSecurityService(MogContext context, String passwordOverride) throws MogException {
        String password = passwordOverride;
        if (log.isTraceEnabled()) {
            log.trace("options = {}", passwordOverride != null ? "***" : "null");
        }

        if (password == null && context != null && context.getMogf() != null) {
            password = context.getMogf().getEncryptionPassword();
        }

        if (password != null) {
            encryptorDecryptor = new MogAESEncryptorDecryptor(password);
        } else {
            encryptorDecryptor = null;
            throw new MogException("Unable to determine encryption key; please check with your MOG administrator");
        }
    }

    public MogDecryptor decryptor() {
        return encryptorDecryptor;
    }

    public MogEncryptor encryptor() {
        return encryptorDecryptor;
    }

}
