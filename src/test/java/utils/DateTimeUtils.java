package utils;

import static java.time.Clock.systemUTC;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class DateTimeUtils {

    // Exposed alongside the clock so a test can state the times it expects outright, rather than
    // deriving them from the same instant the code under test is working from.
    public static final Instant FIXED_NOW = Instant.parse("2026-01-01T12:00:00Z");

    public static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    public static OffsetDateTime now() {
        return OffsetDateTime.now(systemUTC());
    }
}
