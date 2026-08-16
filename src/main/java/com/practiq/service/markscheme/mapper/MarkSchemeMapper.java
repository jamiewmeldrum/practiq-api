package com.practiq.service.markscheme.mapper;

import com.practiq.persistence.MarkSchemeEntity;
import com.practiq.service.markscheme.dto.response.MarkScheme;

public class MarkSchemeMapper {
    public static MarkScheme toMarkScheme(MarkSchemeEntity markSchemeEntity) {
        return new MarkScheme(
                markSchemeEntity.getId(),
                markSchemeEntity.getVersion(),
                markSchemeEntity.getQuestionId(),
                markSchemeEntity.getBody(),
                markSchemeEntity.getCreatedAt());
    }
}
