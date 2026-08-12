package com.practiq.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StringUtilTest {

    @Test
    void isBlankIsTrueForNullEmptyAndWhitespace() {
        assertTrue(StringUtil.isBlank(null));
        assertTrue(StringUtil.isBlank(""));
        assertTrue(StringUtil.isBlank("   "));
        assertTrue(StringUtil.isBlank("\t\n"));
    }

    @Test
    void isBlankIsFalseWhenThereIsAnyNonWhitespaceCharacter() {
        assertFalse(StringUtil.isBlank("a"));
        assertFalse(StringUtil.isBlank("  a  "));
    }
}
