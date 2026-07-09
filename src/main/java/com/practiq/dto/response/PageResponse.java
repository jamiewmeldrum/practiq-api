package com.practiq.dto.response;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.model.Page;
import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

// Envelope for paginated collections. Carries the paging contract alongside the rows so clients can
// render position ("page 2 of N") and know whether a next page exists without probing for an empty
// one. Applied only to endpoints that actually page — unpaged collections stay bare arrays rather
// than carrying degenerate metadata. totalCount comes from the count query the repository's Page
// already runs, so exposing it costs nothing extra.
@Serdeable
public record PageResponse<T>(@NonNull List<T> content, int page, int size, long totalCount) {

    // Pairs the source Page's metadata with rows that have been mapped to a response type — content
    // rarely leaves the service as raw entities, so the mapped list is passed alongside.
    public static <T> PageResponse<T> of(Page<?> page, List<T> content) {
        return new PageResponse<>(content, page.getPageNumber(), page.getSize(), page.getTotalSize());
    }
}
