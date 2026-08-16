package com.practiq.service.document;

import static com.practiq.service.document.DocumentUploadRules.MAX_CONTENT_LENGTH;

import com.practiq.foundation.exception.ContentTooLargeException;
import com.practiq.foundation.exception.EntityValidationException;
import com.practiq.foundation.util.StringUtil;
import com.practiq.service.document.dto.request.DocumentPresignUploadCommand;
import io.micronaut.http.MediaType;
import jakarta.inject.Singleton;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
public class DocumentStager {

    private static final Set<MediaType> ACCEPTED_CONTENT_TYPES = Arrays.stream(DocumentFileType.values())
            .map(DocumentFileType::contentType)
            .collect(Collectors.toSet());

    public DocumentStager() {}

    public StagedDocumentUpload stageUpload(DocumentPresignUploadCommand command) {
        int contentLength = command.contentLength();
        if (contentLength > MAX_CONTENT_LENGTH) {
            throw new ContentTooLargeException(
                    "contentLength", "must not be greater than %s".formatted(MAX_CONTENT_LENGTH));
        }

        String targetContentType = command.contentType();
        MediaType requestedContentType = parseContentType(targetContentType);
        if (!isAcceptedContentType(requestedContentType)) {
            throw new EntityValidationException(
                    "contentType", "'%s' is not a supported content type".formatted(targetContentType));
        }

        String filename = command.filename();
        String extension = fileExtension(filename);
        if (StringUtil.isBlank(extension)) {
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
        return new StagedDocumentUpload(key, filename, command.sourceSpec(), derivedContentType, contentLength);
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
