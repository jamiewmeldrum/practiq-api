package com.practiq.service.document;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class DocumentFileTypeTest {

    @Test
    void everyFileTypeResolvesAContentType() {
        for (DocumentFileType fileType : DocumentFileType.values()) {
            assertNotNull(fileType.contentType(), fileType + " has no content type");
        }
    }
}
