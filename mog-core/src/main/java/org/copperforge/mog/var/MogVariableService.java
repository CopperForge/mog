package org.copperforge.mog.var;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.copperforge.mog.runtime.MogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MogVariableService implements VariableService {

    private Logger log = LoggerFactory.getLogger(MogVariableService.class);
    private final MogContext context;

    public MogVariableService() {
        this(null);
    }

    public MogVariableService(MogContext context) {
        this.context = context;
    }

    public String envsubst(String source) {
        if (source == null) {
            return null;
        }
        log.trace("Running envsubst over '" + source + "'");
        String subst = new String(source);
        Set<String> variables = findVariables(source);
        log.trace("variables = " + variables);

        String val;
        String var;
        for (String variable : variables) {
            var = "${" + variable + "}";
            val = get(variable);
            if (val == null || val.isEmpty()) {
                continue;
            }
            log.trace("Replacing " + var + " with '" + val + "'");
            subst = subst.replace(var, val);
        }

        log.trace("returning '" + subst + "'");
        return subst;
    }

    public String varsubst(String source) {
        if (source == null) {
            return null;
        }
        log.trace("Running varsubst over '" + source + "'");
        String subst = new String(source);
        Set<String> variables = findVariables(source);
        log.trace("variables = " + variables);

        String var;
        for (String variable : variables) {
            var = "${" + variable + "}";

            if (variable.equals("timestamp")) {
                subst = subst.replace(var, timestamp());
            }
        }

        log.trace("returning '" + subst + "'");
        return subst;
    }

    public Set<String> findVariables(String source) {
        Set<String> variables = new HashSet<>();
        if (source == null || source.isBlank()) {
            return variables;
        }

        String pattern = "\\$\\{([A-Za-z0-9_]+)\\}";
        Pattern expr = Pattern.compile(pattern);
        Matcher matcher = expr.matcher(source);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }

        return variables;
    }

    @Override
    public String get(String key) {
        String contextual = contextualValue(key);
        if (contextual != null) {
            return contextual;
        }
        return System.getenv(key) != null ? System.getenv(key) : "";
    }

    @Override
    public MogVariable asVariable(String key) {
        MogVariable variable = new MogVariable();
        variable.setKey(key);
        variable.setValue(get(key));
        return variable;
    }

    @Override
    public String timestamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date(System.currentTimeMillis()));
    }

    private String contextualValue(String key) {
        if (context == null || key == null || key.isBlank()) {
            return null;
        }

        Map<String, Object> variables = context.getVariables();
        if (variables != null && variables.containsKey(key)) {
            Object value = variables.get(key);
            return value != null ? String.valueOf(value) : null;
        }

        return switch (key) {
            case "MOG_HOME" -> context.getMogHome();
            case "MOG_ETC" -> context.getMogEtc();
            case "MOG_ENV" -> context.getEnvironment();
            case "MOG_WORKING_DIR" -> context.getWorkingDirectory();
            default -> null;
        };
    }

}
