package com.practiq.service.document;

import com.practiq.dto.request.PostDocumentRequest;
import com.practiq.exception.EntityValidationException;
import io.micronaut.http.MediaType;
import io.micronaut.http.exceptions.ContentLengthExceededException;
import jakarta.inject.Singleton;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import software.amazon.awssdk.utils.StringUtils;

@Singleton
public class DocumentStager {

    public static final int MAX_CONTENT_LENGTH = 1024 * 1024 * 5; // 5Mb

    private static final Set<MediaType> ACCEPTED_CONTENT_TYPES = Arrays.stream(DocumentFileType.values())
            .map(DocumentFileType::contentType)
            .collect(Collectors.toSet());

    public DocumentStager() {}

    public StagedDocumentUpload stageUpload(PostDocumentRequest request) {
        int contentLength = request.contentLength();
        if (contentLength > MAX_CONTENT_LENGTH) {
            throw new ContentLengthExceededException(
                    "Length of content (%s) is greater than %s".formatted(contentLength, MAX_CONTENT_LENGTH));
        }

        String targetContentType = request.contentType();
        if (StringUtils.isBlank(targetContentType)) {
            throw new EntityValidationException("contentType", "must not be blank");
        }

        MediaType requestedContentType = parseContentType(targetContentType);
        if (!isAcceptedContentType(requestedContentType)) {
            throw new EntityValidationException(
                    "contentType", "'%s' is not a supported content type".formatted(targetContentType));
        }

        String filename = request.filename();
        if (StringUtils.isBlank(filename)) {
            throw new EntityValidationException("filename", "must not be blank");
        }

        String extension = fileExtension(filename);
        if (StringUtils.isBlank(extension)) {
            throw new EntityValidationException("filename", "must have a file extension");
        }

        MediaType derivedContentType = MediaType.forExtension(extension)
                .orElseThrow(() -> new EntityValidationException(
                        "filename", "'.%s' is not a recognised file extension".formatted(extension)));
        if (!isAcceptedContentType(derivedContentType)) {
            throw new EntityValidationException("filename", "'.%s' files are not supported".formatted(extension));
        }

        if (!requestedContentType.equals(derivedContentType)) {
            throw new EntityValidationException(
                    "contentType",
                    "'%s' does not match the '.%s' file extension".formatted(targetContentType, extension));
        }

        String key = UUID.randomUUID() + "." + extension;
        return new StagedDocumentUpload(key, filename, request.sourceSpec(), derivedContentType, contentLength);
    }

    private MediaType parseContentType(String targetContentType) {
        try {
            return MediaType.of(targetContentType.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new EntityValidationException(
                    "contentType", "'%s' is not a valid content type".formatted(targetContentType));
        }
    }

    private String fileExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean isAcceptedContentType(MediaType contentType) {
        return ACCEPTED_CONTENT_TYPES.contains(contentType);
    }
}
