package org.copperforge.mog.env;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.copperforge.mog.MogVariable;
import org.copperforge.mog.annotations.MogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MogService(name = "environmentService")
public class MogEnvironmentService implements EnvironmentService {

    private Logger log = LoggerFactory.getLogger(MogEnvironmentService.class);

    public String envsubst(String source) {
        log.info("Running envsubst over '" + source + "'");
        String subst = new String(source);
        Set<String> variables = findVariables(source);
        log.info("variables = " + variables);

        String val;
        String var;
        for (String variable : variables) {
            var = "${" + variable + "}";
            val = get(variable);
            log.info("Replacing " + var + " with " + val);
            subst = subst.replace(var, val);
        }

        log.info("returning '" + subst + "'");
        return subst;
    }

    protected Set<String> findVariables(String source) {
        Set<String> variables = new HashSet<>();

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
        return System.getenv(key) != null ? System.getenv(key) : "";
    }

    @Override
    public MogVariable asVariable(String key) {
        MogVariable variable = new MogVariable();
        variable.setKey(key);
        variable.setValue(get(key));
        return variable;
    }

}
