package org.copperforge.mog.security;

import org.copperforge.mog.MogException;
import org.copperforge.mog.annotations.MogService;

@MogService(name="securityService")
public class MogSecurityService {

    final MogAESEncryptorDecryptor encryptorDecryptor;

    public MogSecurityService() throws MogException {
        encryptorDecryptor = new MogAESEncryptorDecryptor("ickyicky");
    }

    public MogDecryptor decryptor() {
        return encryptorDecryptor;
    }

    public MogEncryptor encryptor() {
        return encryptorDecryptor;
    }

}
