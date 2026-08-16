package com.practiq.web.binder;

import io.micronaut.context.annotation.Context;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.data.runtime.config.DataConfiguration.PageableConfiguration;

// Replacing Micronaut's Pageable binder means inheriting its whole configuration surface while honouring
// only part of it: sort-ignore-case and sort-delimiter exist solely to parse a sort expression, and
// PageableQueryBinder refuses the sort parameter before any parsing happens. Setting either would do
// nothing whatsoever, so the app refuses to start rather than ignoring them. A startup failure is met in CI
// or a deploy; a silently inert knob is met by whoever spends an afternoon wondering why it had no effect.
//
// Bound values are compared rather than property keys because relaxed binding means the same setting can
// arrive as sort-ignore-case, sortIgnoreCase or MICRONAUT_DATA_PAGEABLE_SORT_IGNORE_CASE, and a key-based
// check has to enumerate the spellings.
@Context
class PageableConfigurationGuard {

    // Micronaut publishes constants for its other pageable defaults but not for this one.
    private static final String DEFAULT_SORT_DELIMITER = ",";

    PageableConfigurationGuard(PageableConfiguration configuration) {
        if (configuration.isSortIgnoreCase() != PageableConfiguration.DEFAULT_SORT_IGNORE_CASE) {
            throw new ConfigurationException(hasNoEffect("sort-ignore-case"));
        }

        if (!DEFAULT_SORT_DELIMITER.equals(
                configuration.getSortDelimiterPattern().pattern())) {
            throw new ConfigurationException(hasNoEffect("sort-delimiter"));
        }
    }

    private static String hasNoEffect(String property) {
        return ("micronaut.data.pageable.%s is set but has no effect: PageableQueryBinder rejects the sort "
                        + "parameter, so no sort expression is ever parsed. Remove the property, or restore "
                        + "sorting in the binder.")
                .formatted(property);
    }
}
