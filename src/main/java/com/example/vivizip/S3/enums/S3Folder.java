package com.example.vivizip.S3.enums;

public enum S3Folder {
    PROFILE("profile"),
    CHAT("chat"),
    REPORT("report"),
    CONTRACT("contract"),
    LEASE_DOCUMENT("lease-document");
    MOVE_IN("move-in");

    private final String path;
    S3Folder(String path) { this.path = path; }
    public String getPath() { return path; }
}
