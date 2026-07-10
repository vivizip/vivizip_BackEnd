package com.example.vivizip.S3.enums;

public enum S3Folder {
    PROFILE("profile"),
    CHAT("chat"),
    REPORT("report"),
    CONTRACT("contract");

    private final String path;
    S3Folder(String path) { this.path = path; }
    public String getPath() { return path; }
}
