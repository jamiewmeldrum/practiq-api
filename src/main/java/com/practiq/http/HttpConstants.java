package com.practiq.http;

// Custom HTTP header names and other wire-level constants owned by the app. Deliberately independent of
// the test-side copy (utils.data.TestData.SESSION_TOKEN_HEADER): duplicated on purpose so a change to one
// side is caught by the other, rather than both moving in lockstep and hiding the regression.
public final class HttpConstants {

    public static final String SESSION_TOKEN_HEADER = "X-Session-Token";

    private HttpConstants() {
    }
}
