package org.copperforge.mog.web.model;

import java.time.Instant;

public record RunSummary(String runId, String status, Instant startedAt, Instant finishedAt, String artifactName) {}
