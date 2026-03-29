package org.copperforge.mog.runtime;

import java.io.File;

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
    private MogRuntime() {
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

    public static Report loadReportDefinition(String reference, MogContext context) throws MogException {
        Report report = ReportService.instance().parse(reference, context);
        if (report != null) {
            report.setContext(context);
        }
        return report;
    }

    public static String generateReport(Report definition, MogContext context) throws MogException {
        if (definition != null && context != null) {
            definition.setContext(context);
        }
        ReportWriter writer = ReportWriterService.instance().builder(definition.getType());
        if (writer == null) {
            throw new MogException("No report writer registered for type '" + definition.getType() + "'");
        }
        writer.build(definition);
        String filename = MogFileNameBuilder.build(definition.getFilename());
        writer.save(filename);
        return filename;
    }

    public static String encrypt(MogContext context, String value, String passwordOverride) throws MogException {
        MogSecurityService securityService = new MogSecurityService(context, passwordOverride);
        return securityService.encryptor().encrypt(value);
    }

    public static String decrypt(MogContext context, String value, String passwordOverride) throws MogException {
        MogSecurityService securityService = new MogSecurityService(context, passwordOverride);
        return securityService.decryptor().decrypt(value);
    }
}
