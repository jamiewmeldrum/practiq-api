package com.practiq.domain.converters;

import com.practiq.domain.types.QuestionDifficulty;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Optional;

@Converter
public class QuestionDifficultyAttributeConverter implements AttributeConverter<QuestionDifficulty, String> {

    @Override
    public String convertToDatabaseColumn(QuestionDifficulty attribute) {
        return String.valueOf(attribute.value());
    }

    @Override
    public QuestionDifficulty convertToEntityAttribute(String db) {
        if (db == null) return null;

        return Optional.ofNullable(QuestionDifficulty.forValue(Integer.parseInt(db)))
                .orElseThrow(() -> new IllegalArgumentException("Unknown difficulty: " + db));
    }
}