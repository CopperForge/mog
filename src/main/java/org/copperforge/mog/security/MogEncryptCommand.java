package org.copperforge.mog.security;

import org.copperforge.mog.MogException;
import org.copperforge.mog.MogOptions;
import org.copperforge.mog.annotations.MogCommand;
import org.copperforge.mog.annotations.Moglet;

@MogCommand
public class MogEncryptCommand {

    @Moglet
    private MogSecurityService securityService;
    
    @MogCommand(name="encrypt", description = "encrypt the given value")
    public String encrypt(MogOptions mogOptions) throws MogException {
        MogEncryptDecryptOptions options = MogEncryptDecryptOptions.parse(mogOptions);

        String encryptedText = securityService.encryptor().encrypt(options.getValue());
        System.out.println("encrypted = '" + encryptedText + "'");

        return encryptedText;
    }

    @MogCommand(name="decrypt", description = "decrypt the given value")
    public String decrypt(MogOptions mogOptions) throws MogException {
        MogEncryptDecryptOptions options = MogEncryptDecryptOptions.parse(mogOptions);

        String decryptedText = securityService.decryptor().decrypt(options.getValue());
        System.out.println("decrypted = '" + decryptedText + "'");

        return decryptedText;
    }


}
