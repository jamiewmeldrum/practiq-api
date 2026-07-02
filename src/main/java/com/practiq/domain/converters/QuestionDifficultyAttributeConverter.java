package com.practiq.domain.converters;

import com.practiq.domain.types.QuestionDifficulty;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Optional;

@Converter
public class QuestionDifficultyAttributeConverter implements AttributeConverter<QuestionDifficulty, Integer> {

    @Override
    public Integer convertToDatabaseColumn(QuestionDifficulty attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public QuestionDifficulty convertToEntityAttribute(Integer db) {
        if (db == null) return null;

        return Optional.ofNullable(QuestionDifficulty.forValue(db))
                .orElseThrow(() -> new IllegalArgumentException("Unknown difficulty: " + db));
    }
}