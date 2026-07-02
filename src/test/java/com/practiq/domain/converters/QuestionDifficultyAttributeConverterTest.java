package com.practiq.domain.converters;

import com.practiq.domain.types.QuestionDifficulty;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuestionDifficultyAttributeConverterTest {

    private static final int[] OUT_OF_RANGE_VALUES = {0, 6, -1, 100};

    private final QuestionDifficultyAttributeConverter converter = new QuestionDifficultyAttributeConverter();

    @Test
    void roundTripsEveryDifficulty() {
        for (QuestionDifficulty difficulty : QuestionDifficulty.values()) {
            Integer column = converter.convertToDatabaseColumn(difficulty);

            assertEquals(difficulty.value(), column);
            assertEquals(difficulty, converter.convertToEntityAttribute(column));
        }
    }

    @Test
    void convertToDatabaseColumnMapsNullToNull() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToEntityAttributeMapsNullToNull() {
        assertNull(converter.convertToEntityAttribute(null));
    }

    // Can't happen while the difficulty CHECK constraint (1..5) stands, but the converter must fail
    // loudly rather than silently return null if a bad value ever reaches it from the database.
    @Test
    void convertToEntityAttributeRejectsOutOfRangeValues() {
        for (int value : OUT_OF_RANGE_VALUES) {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> converter.convertToEntityAttribute(value)
            );
            assertEquals("Unknown difficulty: " + value, exception.getMessage());
        }
    }
}
