package com.practiq.foundation.util;

import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import java.time.Clock;

// Nothing reads the system clock directly: a service takes a Clock so the time it makes decisions
// against is an input a test can set, rather than whatever now() happened to return.
@Factory
public class ClockFactory {

    @Singleton
    public Clock systemClock() {
        return Clock.systemUTC();
    }
}
