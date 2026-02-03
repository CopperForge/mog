package org.copperforge.mog.cli;

import java.util.Optional;

import org.copperforge.mog.Mog;
import org.copperforge.mog.MogOptions;
import org.copperforge.mog.config.MogConfig;
import org.copperforge.mog.config.Mogf;
import org.copperforge.mog.runtime.MogCliOptionsView;
import org.copperforge.mog.runtime.MogEncryptionOptionsView;
import org.copperforge.mog.runtime.MogRuntime;
import org.copperforge.mog.runtime.MogRuntimeContext;

public final class RuntimeBridge {

    private static CliRuntimeContext context;

    private RuntimeBridge() {
    }

    public static void initialize(Mog mog, MogOptions options) {
        context = new CliRuntimeContext(mog, options);
        MogRuntime.register(context);
    }

    public static CliRuntimeContext context() {
        return context;
    }

    public static final class CliRuntimeContext implements MogRuntimeContext {
        private final Mog mog;
        private final MogOptions options;
        private MogEncryptionOptionsView encryptionOptions;

        private CliRuntimeContext(Mog mog, MogOptions options) {
            this.mog = mog;
            this.options = options;
        }

        public void setEncryptionOptions(MogEncryptionOptionsView encryptionOptions) {
            this.encryptionOptions = encryptionOptions;
        }

        public void clearEncryptionOptions() {
            this.encryptionOptions = null;
        }

        @Override
        public Optional<MogConfig> config() {
            return Optional.ofNullable(mog.config());
        }

        @Override
        public Optional<Mogf> mogf() {
            return Optional.ofNullable(mog.mogf());
        }

        @Override
        public Optional<? extends MogCliOptionsView> cliOptions() {
            return Optional.ofNullable(options);
        }

        @Override
        public Optional<? extends MogEncryptionOptionsView> encryptionOptions() {
            return Optional.ofNullable(encryptionOptions);
        }
    }
}
