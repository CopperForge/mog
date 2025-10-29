package org.copperforge.mog.security;

import org.copperforge.mog.Mog;
import org.copperforge.mog.MogException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MogSecurityService {

    private Logger log = LoggerFactory.getLogger(MogSecurityService.class);

    final MogAESEncryptorDecryptor encryptorDecryptor;

    public MogSecurityService() throws MogException {
        MogEncryptDecryptOptions options = MogEncryptDecryptOptions.parse(Mog.mog().options());
        if (log.isTraceEnabled())
            log.trace(String.format("options = %s", options));

        if (options.getPassword() != null)
            encryptorDecryptor = new MogAESEncryptorDecryptor(options.getPassword());
        else if (Mog.mog().mogf().getEncryptionPassword() != null)
            encryptorDecryptor = new MogAESEncryptorDecryptor(Mog.mog().mogf().getEncryptionPassword());
        else {
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
