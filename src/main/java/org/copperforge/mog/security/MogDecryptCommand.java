package org.copperforge.mog.security;

import org.copperforge.mog.Mog;
import org.copperforge.mog.MogException;
import org.copperforge.mog.MogOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "decrypt", description = "Decrypt a value")
public class MogDecryptCommand implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(MogDecryptCommand.class);

    private MogSecurityService securityService;

    @Mixin
    private MogEncryptDecryptOptions parsedOptions;

    @Override
    public void run() {
        try {
            MogOptions mogOptions = Mog.mog().options();
            MogEncryptDecryptOptions options = parsedOptions != null ? parsedOptions : MogEncryptDecryptOptions.parse(mogOptions);
            if (parsedOptions.getValue() != null) options.setValue(parsedOptions.getValue());

            if (options.getValue() == null) {
                log.info("Usage: mog decrypt --value=<text> [--password=<pwd>]");
                return;
            }

            securityService = new MogSecurityService();
            String decrypted = securityService.decryptor().decrypt(options.getValue());
            if (log.isTraceEnabled()) log.trace(String.format("decrypted = '%s'", decrypted));
            System.out.println(decrypted);
        } catch (MogException e) {
            throw new RuntimeException(e);
        }
    }
}

