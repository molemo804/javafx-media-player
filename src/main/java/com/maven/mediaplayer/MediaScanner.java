package com.maven.mediaplayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MediaScanner {

    private static final Set<String> AUDIO_EXT =
            Set.of("mp3", "wav", "aac", "m4a", "flac", "ogg");
    private static final Set<String> VIDEO_EXT =
            Set.of("mp4", "mkv", "avi", "mov", "flv", "webm");

    public static List<MediaFile> scan(Path folder) throws IOException {
        if (!Files.isDirectory(folder)) return List.of();

        try (Stream<Path> stream = Files.list(folder)) {
            return stream
                .filter(Files::isRegularFile)
                .map(MediaScanner::classify)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(m -> m.getPath().getFileName().toString()))
                .collect(Collectors.toList());
        }
    }

    /** Also returns a single file as MediaFile (used by Add File dialog). */
    public static MediaFile classifySingle(Path file) {
        if (!Files.isRegularFile(file)) return null;
        return classify(file);
    }

    private static MediaFile classify(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        int dot = name.lastIndexOf('.');
        if (dot < 0) return null;
        String ext = name.substring(dot + 1);

        if (AUDIO_EXT.contains(ext)) return new MediaFile(file, MediaFile.Type.AUDIO);
        if (VIDEO_EXT.contains(ext)) return new MediaFile(file, MediaFile.Type.VIDEO);
        return null;
    }
}
