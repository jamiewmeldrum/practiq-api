package com.practiq.service.document;

import com.practiq.dto.request.PostDocumentRequest;
import com.practiq.exception.EntityValidationException;
import io.micronaut.http.MediaType;
import io.micronaut.http.exceptions.ContentLengthExceededException;
import jakarta.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;
import software.amazon.awssdk.utils.StringUtils;

@Singleton
public class DocumentStager {

    public static final int MAX_CONTENT_LENGTH = 1024 * 1024 * 5; // 5Mb

    private static final Set<MediaType> ACCEPTED_MEDIA_TYPES = Arrays.stream(DocumentFileType.values())
            .map(DocumentFileType::mediaType)
            .collect(Collectors.toSet());

    public DocumentStager() {}

    public StagedDocumentUpload stageUpload(PostDocumentRequest request) {
        int contentLength = request.contentLength();
        if (contentLength > MAX_CONTENT_LENGTH) {
            throw new ContentLengthExceededException(
                    "Length of content (%s) is greater than %s".formatted(contentLength, MAX_CONTENT_LENGTH));
        }

        String targetMediaType = request.contentType();
        MediaType requestedMediaType = MediaType.of(targetMediaType.toLowerCase(Locale.ROOT));
        if (!isAcceptedMediaType(requestedMediaType)) {
            throw new EntityValidationException("Unsupported media type specified: " + targetMediaType);
        }

        String filename = request.filename();
        String extension = fileExtension(filename);
        if (StringUtils.isBlank(extension)) {
            throw new EntityValidationException("File extension is could not be determined: " + filename);
        }

        MediaType derivedMediaType = mediaTypeOf(extension)
                .orElseThrow(() ->
                        new EntityValidationException("Unrecognised file extension for file with name: " + filename));
        if (!isAcceptedMediaType(derivedMediaType)) {
            throw new EntityValidationException("Unsupported file type for file with name: " + filename);
        }

        if (!requestedMediaType.equals(derivedMediaType)) {
            throw new EntityValidationException(
                    "Media type for file %s does not match specified type %s: ".formatted(filename, targetMediaType));
        }

        String key = UUID.randomUUID() + "." + extension;
        return new StagedDocumentUpload(key, filename, request.sourceSpec(), derivedMediaType, contentLength);
    }

    private Optional<MediaType> mediaTypeOf(String fileExtension) {
        if (StringUtils.isBlank(fileExtension)) {
            return Optional.empty();
        }
        return MediaType.forExtension(fileExtension);
    }

    private String fileExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean isAcceptedMediaType(MediaType mediaType) {
        return ACCEPTED_MEDIA_TYPES.contains(mediaType);
    }
}
