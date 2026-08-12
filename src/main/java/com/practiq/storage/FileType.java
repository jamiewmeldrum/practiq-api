package com.practiq.storage;

import io.micronaut.http.MediaType;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public enum FileType {
    PDF("pdf", MediaType.APPLICATION_PDF),
    JPG("jpg", MediaType.IMAGE_JPEG),
    JPEG("jpeg", MediaType.IMAGE_JPEG),
    PNG("png", MediaType.IMAGE_PNG),
    GIF("gif", MediaType.IMAGE_GIF),
    WEBP("webp", MediaType.IMAGE_WEBP),
    SVG("svg", MediaType.IMAGE_SVG),
    BMP("bmp", MediaType.IMAGE_BMP),
    TXT("txt", MediaType.TEXT_PLAIN),
    DOC("doc", "application/msword"),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private static final Map<String, FileType> BY_EXTENSION = Arrays.stream(values())
            .collect(java.util.stream.Collectors.toUnmodifiableMap(FileType::extension, type -> type));

    private final String extension;
    private final String contentType;

    FileType(String extension, String contentType) {
        this.extension = extension;
        this.contentType = contentType;
    }

    public String extension() {
        return extension;
    }

    public String contentType() {
        return contentType;
    }

    public static Optional<FileType> fromExtension(String extension) {
        FileType fileType = BY_EXTENSION.get(extension.toLowerCase(Locale.ROOT));

        if (fileType == null) {
            return Optional.empty();
        }

        return Optional.of(fileType);
    }
}
