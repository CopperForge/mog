package org.copperforge.mog.runtime;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.copperforge.mog.MogException;
import org.copperforge.mog.config.MogConfig;
import org.copperforge.mog.config.Mogf;
import org.copperforge.mog.io.MogFileNameBuilder;
import org.copperforge.mog.reader.MogReader;
import org.copperforge.mog.reporting.definition.Report;
import org.copperforge.mog.reporting.definition.ReportService;
import org.copperforge.mog.reporting.writer.ReportWriter;
import org.copperforge.mog.reporting.writer.ReportWriterService;
import org.copperforge.mog.security.MogSecurityService;
import org.copperforge.mog.var.MogVariableService;

public final class MogRuntime {
    private static final AtomicReference<MogContext> CONTEXT = new AtomicReference<>();

    private MogRuntime() {
    }

    public static void bootstrap(MogContext context) {
        CONTEXT.set(context);
    }

    public static Optional<MogContext> context() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static void clear() {
        CONTEXT.set(null);
    }

    public static MogConfig loadConfig(String rawConfigPath) throws MogException {
        MogVariableService vars = new MogVariableService();
        String configFile = vars.envsubst(rawConfigPath);
        return new MogReader<MogConfig>(MogConfig.class).read(configFile);
    }

    public static Mogf loadMogf(MogConfig config) throws MogException {
        File mogFile = new File(config.userHome() + "/.mog");
        if (!mogFile.isFile()) {
            mogFile = new File(config.mogHome() + "/.mog");
            if (!mogFile.isFile()) {
                return new Mogf();
            }
        }
        return new MogReader<Mogf>(Mogf.class).read(mogFile);
    }

    public static Report loadReportDefinition(String reference) throws MogException {
        return ReportService.instance().parse(reference, ensureContext());
    }

    public static String generateReport(Report definition) throws MogException {
        ReportWriter writer = ReportWriterService.instance().builder(definition.getType());
        writer.build(definition);
        String filename = MogFileNameBuilder.build(definition.getFilename());
        writer.save(filename);
        return filename;
    }

    public static String encrypt(String value, String passwordOverride) throws MogException {
        MogSecurityService securityService = new MogSecurityService(ensureContext(), passwordOverride);
        return securityService.encryptor().encrypt(value);
    }

    public static String decrypt(String value, String passwordOverride) throws MogException {
        MogSecurityService securityService = new MogSecurityService(ensureContext(), passwordOverride);
        return securityService.decryptor().decrypt(value);
    }

    private static MogContext ensureContext() {
        return context().orElseThrow(() -> new IllegalStateException("MogContext has not been initialized"));
    }
}
