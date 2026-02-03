package org.copperforge.mog.security;

import org.copperforge.mog.Mog;
import org.copperforge.mog.MogException;
import org.copperforge.mog.MogOptions;
import org.copperforge.mog.runtime.MogRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "encrypt", description = "Encrypt a value")
public class MogEncryptCommand implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(MogEncryptCommand.class);

    @Mixin
    private MogEncryptDecryptOptions parsedOptions;

    @Override
    public void run() {
        try {
            MogOptions mogOptions = Mog.mog().options();
            MogEncryptDecryptOptions options = parsedOptions != null ? parsedOptions : MogEncryptDecryptOptions.parse(mogOptions);
            if (parsedOptions.getValue() != null) options.setValue(parsedOptions.getValue());

            if (options.getValue() == null) {
                log.info("Usage: mog encrypt --value=<text> [--password=<pwd>]");
                return;
            }

            String encrypted = MogRuntime.encrypt(options.getValue(), options.getPassword());
            System.out.println(encrypted);
        } catch (MogException e) {
            throw new RuntimeException(e);
        }
    }
}
