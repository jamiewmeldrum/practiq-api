package com.practiq.domain.types;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class QuestionDifficultyTest {

    private static final int[] OUT_OF_RANGE_VALUES = {0, 6, -1, 100};

    // Locks the stored integer values: these are persisted, referenced by the DB CHECK constraint,
    // and mapped by the converter, so a silent renumbering would corrupt existing rows.
    @Test
    void difficultiesMapToExpectedIntegers() {
        assertEquals(1, QuestionDifficulty.TRIVIAL.value());
        assertEquals(2, QuestionDifficulty.EASY.value());
        assertEquals(3, QuestionDifficulty.MEDIUM.value());
        assertEquals(4, QuestionDifficulty.HARD.value());
        assertEquals(5, QuestionDifficulty.VERY_HARD.value());
    }

    @Test
    void forValueReturnsConstantForItsValue() {
        for (QuestionDifficulty difficulty : QuestionDifficulty.values()) {
            assertEquals(difficulty, QuestionDifficulty.forValue(difficulty.value()));
        }
    }

    @Test
    void forValueReturnsNullForOutOfRangeValues() {
        for (int value : OUT_OF_RANGE_VALUES) {
            assertNull(QuestionDifficulty.forValue(value));
        }
    }

    // Guard: if you add a sixth difficulty, this fails on purpose. Before changing the number,
    // update the DB CHECK constraint (difficulty between 1 and 5), the V2 migration comment,
    // QuestionDifficulty.forValue, and the converter tests — the value is not a single-file change.
    @Test
    void onlyFiveDifficultyLevelsExist() {
        assertEquals(5, QuestionDifficulty.values().length);
    }
}
