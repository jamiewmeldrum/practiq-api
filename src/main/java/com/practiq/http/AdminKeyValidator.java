package com.practiq.http;

import static com.practiq.http.HttpConstants.ADMIN_KEY_HEADER;

import com.practiq.exception.UnauthorizedException;
import com.practiq.util.StringUtil;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;

@Singleton
public class AdminKeyValidator {

    public static final String PRACTIQ_ADMIN_KEY_CONFIG_PARAM = "${practiq.admin-key}";

    private final String expectedKey;

    public AdminKeyValidator(@Value(PRACTIQ_ADMIN_KEY_CONFIG_PARAM) String expectedKey) {
        if (StringUtil.isBlank(expectedKey)) {
            throw new IllegalStateException(
                    "Admin key could not bind from parameter " + PRACTIQ_ADMIN_KEY_CONFIG_PARAM);
        }
        this.expectedKey = expectedKey;
    }

    public void validate(String adminKey) {
        if (!expectedKey.equals(adminKey)) {
            throw new UnauthorizedException(ADMIN_KEY_HEADER);
        }
    }
}
