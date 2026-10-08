package com.maven.mediaplayer;

import java.nio.file.Path;

public class MediaFile {
    public enum Type { AUDIO, VIDEO }

    private final Path path;
    private final Type type;

    public MediaFile(Path path, Type type) {
        this.path = path;
        this.type = type;
    }

    public Path getPath() { return path; }
    public Type getType() { return type; }

    @Override
    public String toString() {
        return path.getFileName().toString();
    }
}