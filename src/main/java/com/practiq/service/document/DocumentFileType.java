package com.practiq.service.document;

import io.micronaut.http.MediaType;

// The extensions we accept for upload. Recognising an extension is Micronaut's job — it bundles a
// mime.types table — but accepting one is ours, so this enum is the allow-list and fromExtension
// returning empty is the rejection.
public enum DocumentFileType {
    PDF("pdf"),
    JPG("jpg"),
    JPEG("jpeg"),
    PNG("png"),
    GIF("gif"),
    WEBP("webp"),
    BMP("bmp"),
    TXT("txt"),
    DOC("doc"),
    DOCX("docx");

    private final MediaType contentType;

    DocumentFileType(String extension) {
        this.contentType = MediaType.forExtension(extension)
                .orElseThrow(() -> new IllegalStateException("No content type known for extension ." + extension));
    }

    public MediaType contentType() {
        return contentType;
    }
}
