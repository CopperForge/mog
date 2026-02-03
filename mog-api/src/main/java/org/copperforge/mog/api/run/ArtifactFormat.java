package org.copperforge.mog.api.run;

import java.util.Locale;

enum ArtifactFormat {
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final String extension;
    private final String contentType;

    ArtifactFormat(String extension, String contentType) {
        this.extension = extension;
        this.contentType = contentType;
    }

    public String extension() {
        return extension;
    }

    public String contentType() {
        return contentType;
    }

    public String artifactFileName() {
        return "artifact." + extension;
    }

    public static ArtifactFormat from(String rawFormat) {
        if (rawFormat == null || rawFormat.isBlank()) {
            return XLSX;
        }
        try {
            return ArtifactFormat.valueOf(rawFormat.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported output format '" + rawFormat + "'");
        }
    }
}
