package com.practiq.dto.mapper;

import com.practiq.domain.MarkScheme;
import com.practiq.dto.response.MarkSchemeResponse;
import lombok.extern.slf4j.Slf4j;

//TODO - explicit tests
@Slf4j
public class MarkSchemeResponseMapper {
    public static MarkSchemeResponse toMarkSchemeResponse(MarkScheme markScheme) {
        log.trace("Converting question to MarkSchemeResponse: {}", markScheme.getId());

        return new MarkSchemeResponse(
                markScheme.getId(),
                markScheme.getQuestion().getId(),
                markScheme.getBody(),
                markScheme.getCreatedAt()
        );
    }
}
