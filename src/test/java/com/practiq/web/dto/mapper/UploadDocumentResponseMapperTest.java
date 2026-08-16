package com.practiq.web.dto.mapper;

import static com.practiq.web.dto.mapper.UploadDocumentResponseMapper.toUploadDocumentResponse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.practiq.service.document.dto.response.DocumentPresignUpload;
import com.practiq.web.dto.response.UploadDocumentResponse;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UploadDocumentResponseMapperTest {

    @Test
    void documentPresignUploadMapsToUploadDocumentResponse() {
        long id = 99L;
        String url = "https://s3.example.com/cf5c1c1e-8a7d-4b0e-9f2a-6d3b7c8e1f04.pdf?X-Amz-Signature=abc";
        Instant expiresAt = Instant.parse("2026-08-12T18:10:00Z");

        DocumentPresignUpload documentPresignUpload = new DocumentPresignUpload(id, url, expiresAt);

        UploadDocumentResponse uploadDocumentResponse = toUploadDocumentResponse(documentPresignUpload);

        assertThat(uploadDocumentResponse.id(), equalTo(id));
        assertThat(uploadDocumentResponse.url(), equalTo(url));
        assertThat(uploadDocumentResponse.expiresAt(), equalTo(expiresAt));
    }
}
