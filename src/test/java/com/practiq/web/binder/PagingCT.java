package com.practiq.web.binder;

import static io.micronaut.http.HttpStatus.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import io.micronaut.context.annotation.Property;
import io.micronaut.runtime.server.EmbeddedServer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.ComponentTest;

// How this app pages, proven once against a controller that exists for the purpose: which paging parameters
// are accepted and how the rest are refused, and what the envelope carrying a page looks like on the wire.
// Every paged endpoint then owes a single test that this binder is the one in use, rather than repeating
// these cases.
@ComponentTest
@Property(name = "spec.name", value = "PagingCT")
class PagingCT {

    private static final String PAGING_PATH = "/test/paging";

    // Restated rather than read from configuration: these pin what application.yml says, they do not follow it.
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    @Inject
    private EmbeddedServer embeddedServer;

    @BeforeEach
    void setUp() {
        RestAssured.port = embeddedServer.getPort();
    }

    @Test
    void bindsTheConfiguredDefaultsWhenNoPagingIsRequested() {
        given().when()
                .get(PAGING_PATH)
                .then()
                .statusCode(OK.getCode())
                .body("page", equalTo(0))
                .body("size", equalTo(DEFAULT_PAGE_SIZE));
    }

    @Test
    void bindsTheRequestedPageAndSize() {
        given().when()
                .get(PAGING_PATH + "?page=2&size=20")
                .then()
                .statusCode(OK.getCode())
                .body("page", equalTo(2))
                .body("size", equalTo(20));
    }

    @Test
    void bindsSizeAtTheMaximumButRejectsOneAbove() {
        given().when()
                .get(PAGING_PATH + "?size=" + MAX_PAGE_SIZE)
                .then()
                .statusCode(OK.getCode())
                .body("size", equalTo(MAX_PAGE_SIZE));

        // Refused rather than clamped: the message is the only place a client can learn the ceiling, which a
        // silently shortened page cannot tell it.
        given().when()
                .get(PAGING_PATH + "?size=" + (MAX_PAGE_SIZE + 1))
                .then()
                .statusCode(UNPROCESSABLE_ENTITY.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("size: must not be greater than " + MAX_PAGE_SIZE))
                .body("status", equalTo(422));
    }

    @Test
    void bindsPageZeroButRejectsANegativePage() {
        given().when()
                .get(PAGING_PATH + "?page=0")
                .then()
                .statusCode(OK.getCode())
                .body("page", equalTo(0));

        for (String page : List.of("-1", "-4002")) {
            given().when()
                    .get(PAGING_PATH + "?page=" + page)
                    .then()
                    .statusCode(UNPROCESSABLE_ENTITY.getCode())
                    .contentType(ContentType.JSON)
                    .body("keySet()", containsInAnyOrder("error", "status"))
                    .body("error", equalTo("page: must be greater than or equal to 0"))
                    .body("status", equalTo(422));
        }
    }

    @Test
    void bindsSizeOfOneButRejectsAnythingBelowIt() {
        given().when()
                .get(PAGING_PATH + "?size=1")
                .then()
                .statusCode(OK.getCode())
                .body("size", equalTo(1));

        for (String size : List.of("0", "-5")) {
            given().when()
                    .get(PAGING_PATH + "?size=" + size)
                    .then()
                    .statusCode(UNPROCESSABLE_ENTITY.getCode())
                    .contentType(ContentType.JSON)
                    .body("keySet()", containsInAnyOrder("error", "status"))
                    .body("error", equalTo("size: must be greater than or equal to 1"))
                    .body("status", equalTo(422));
        }
    }

    @Test
    void rejectsPagingValuesThatAreNotNumbers() {
        // The same 400 a non-numeric filter parameter gets, so a client cannot tell binding a page from
        // binding a conceptId by the shape of the failure.
        for (String parameter : List.of("page", "size")) {
            given().when()
                    .get(PAGING_PATH + "?" + parameter + "=abc")
                    .then()
                    .statusCode(BAD_REQUEST.getCode())
                    .contentType(ContentType.JSON)
                    .body("keySet()", containsInAnyOrder("error", "status"))
                    .body("error", equalTo(parameter + ": invalid value"))
                    .body("status", equalTo(400));
        }
    }

    @Test
    void rejectsSortBecauseNoEndpointOffersIt() {
        // Answering 200 in a different order than asked for would be a silent lie; the runners impose their
        // own total order, so no sort a client sends could ever be honoured.
        given().when()
                .get(PAGING_PATH + "?sort=createdAt,desc")
                .then()
                .statusCode(UNPROCESSABLE_ENTITY.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("error", "status"))
                .body("error", equalTo("sort: is not supported"))
                .body("status", equalTo(422));
    }

    @Test
    void bindsAPageBeyondTheDataRatherThanRejectingIt() {
        // Paging past the end is a valid request with an empty answer, not a bad value: a client walking
        // pages must be able to ask for one it turns out not to reach.
        given().when()
                .get(PAGING_PATH + "?page=999")
                .then()
                .statusCode(OK.getCode())
                .body("page", equalTo(999));
    }

    @Test
    void wrapsRowsInAnEnvelopeCarryingThePagingPosition() {
        given().when()
                .get(PAGING_PATH + "?page=1&size=2")
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("content", "page", "size", "totalCount"))
                .body("content", contains("first", "second"))
                .body("page", equalTo(1))
                .body("size", equalTo(2))
                // The total across all pages, not the row count of this one — that is what lets a client
                // render "page 2 of N" without walking to the end.
                .body("totalCount", equalTo(7));
    }

    @Test
    void keepsAnEmptyContentArrayInTheEnvelope() {
        // Serde omits empty collections by default, which would drop the key entirely and force a client to
        // treat absent and empty as the same thing.
        given().when()
                .get(PAGING_PATH + "/empty")
                .then()
                .statusCode(OK.getCode())
                .contentType(ContentType.JSON)
                .body("keySet()", containsInAnyOrder("content", "page", "size", "totalCount"))
                .body("content", empty())
                .body("totalCount", equalTo(0));
    }
}
