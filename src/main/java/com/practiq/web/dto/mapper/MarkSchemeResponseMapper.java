package com.practiq.web.dto.mapper;

import com.practiq.service.markscheme.dto.response.MarkScheme;
import com.practiq.web.dto.response.MarkSchemeResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MarkSchemeResponseMapper {
    public static MarkSchemeResponse toMarkSchemeResponse(MarkScheme markScheme) {
        log.trace("Converting MarkScheme to MarkSchemeResponse: {}", markScheme.id());

        return new MarkSchemeResponse(
                markScheme.id(), markScheme.questionId(), markScheme.body(), markScheme.createdAt());
    }
}
