package org.copperforge.mog.runtime;

import java.util.Optional;

import org.copperforge.mog.config.MogConfig;
import org.copperforge.mog.config.Mogf;

public interface MogRuntimeContext {
    Optional<MogConfig> config();
    Optional<Mogf> mogf();
    Optional<? extends MogCliOptionsView> cliOptions();
    Optional<? extends MogEncryptionOptionsView> encryptionOptions();
}
