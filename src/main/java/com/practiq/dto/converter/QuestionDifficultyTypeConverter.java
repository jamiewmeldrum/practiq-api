package com.practiq.dto.converter;

import com.practiq.domain.types.QuestionDifficulty;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.TypeConverter;
import jakarta.inject.Singleton;

import java.util.Optional;

@Singleton
public class QuestionDifficultyTypeConverter implements TypeConverter<String, QuestionDifficulty> {

    @Override
    public Optional<QuestionDifficulty> convert(String value,
                                                Class<QuestionDifficulty> targetType,
                                                ConversionContext context) {
        try {
            QuestionDifficulty difficulty = QuestionDifficulty.forValue(Integer.parseInt(value.trim()));
            if (difficulty == null) {
                throw new IllegalArgumentException("Unknown difficulty code: " + value);
            }
            return Optional.of(difficulty);
        } catch (IllegalArgumentException e) {
            context.reject(value, e);
            return Optional.empty();
        }
    }
}
