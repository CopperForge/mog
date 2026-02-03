package org.copperforge.mog.web.model;

public record ArtifactDownload(byte[] data, String filename, String contentType) {}
