package com.practiq.web;

// Custom HTTP header names and other wire-level constants owned by the app. Deliberately independent of
// the test-side copy (utils.data.TestData.SESSION_TOKEN_HEADER): duplicated on purpose so a change to one
// side is caught by the other, rather than both moving in lockstep and hiding the regression.
public final class HttpConstants {

    public static final String SESSION_TOKEN_HEADER = "X-Session-Token";
    public static final String ADMIN_KEY_HEADER = "X-Admin-Key";

    private HttpConstants() {}
}
