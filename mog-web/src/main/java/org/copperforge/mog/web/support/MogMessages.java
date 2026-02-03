package org.copperforge.mog.web.support;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class MogMessages {

    private static final List<String> CALLOUTS = List.of(
            "Keep your DSL tidy and Mog will keep smiling!",
            "Remember: configs in, shiny artifacts out.",
            "Feed me datasources and I'll feed you dashboards.",
            "If a run fails, I'll help you find out why.");

    private MogMessages() {
    }

    public static String randomCallout() {
        return CALLOUTS.get(ThreadLocalRandom.current().nextInt(CALLOUTS.size()));
    }
}
