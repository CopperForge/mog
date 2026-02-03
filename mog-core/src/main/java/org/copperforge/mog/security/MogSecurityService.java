package org.copperforge.mog.security;

import org.copperforge.mog.MogException;
import org.copperforge.mog.config.Mogf;
import org.copperforge.mog.runtime.MogEncryptionOptionsView;
import org.copperforge.mog.runtime.MogRuntime;
import org.copperforge.mog.runtime.MogRuntimeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MogSecurityService {

    private Logger log = LoggerFactory.getLogger(MogSecurityService.class);

    final MogAESEncryptorDecryptor encryptorDecryptor;

    public MogSecurityService() throws MogException {
        MogEncryptionOptionsView options = MogRuntime.current()
                .flatMap(MogRuntimeContext::encryptionOptions)
                .orElse(null);
        if (log.isTraceEnabled()) {
            log.trace("options = {}", options);
        }

        if (options != null && options.getPassword() != null) {
            encryptorDecryptor = new MogAESEncryptorDecryptor(options.getPassword());
        } else {
            String mogfPassword = MogRuntime.current()
                    .flatMap(MogRuntimeContext::mogf)
                    .map(Mogf::getEncryptionPassword)
                    .orElse(null);
            if (mogfPassword != null) {
                encryptorDecryptor = new MogAESEncryptorDecryptor(mogfPassword);
            } else {
                encryptorDecryptor = null;
                throw new MogException("Unable to determine encryption key; please check with your MOG administrator");
            }
        }
    }

    public MogDecryptor decryptor() {
        return encryptorDecryptor;
    }

    public MogEncryptor encryptor() {
        return encryptorDecryptor;
    }

}
