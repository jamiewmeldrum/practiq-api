package utils;

import static java.time.Clock.systemUTC;

import java.time.OffsetDateTime;

public class DateTimeUtils {

    public static OffsetDateTime now() {
        return OffsetDateTime.now(systemUTC());
    }
}
