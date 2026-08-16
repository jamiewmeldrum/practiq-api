package com.practiq.web.binder;

import com.practiq.web.dto.response.PageResponse;
import io.micronaut.context.annotation.Requires;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import java.util.List;

// Test-only controller for PagingCT: builds a paged envelope from whatever Pageable the binder produced, so
// binding and the envelope can both be exercised without a repository, a service or a query runner in the
// way. Scoped to that test via the spec.name idiom. See the README testing section.
@Requires(property = "spec.name", value = "PagingCT")
@Controller("/test/paging")
class PageableTestController {

    private static final List<String> ROWS = List.of("first", "second");
    private static final long TOTAL_COUNT = 7L;

    @Get
    public PageResponse<String> page(Pageable pageable) {
        return PageResponse.of(Page.of(ROWS, pageable, TOTAL_COUNT), ROWS);
    }

    // A separate endpoint rather than a query parameter: emptiness cannot be driven through the paging
    // parameters without inventing a fake-only one, which would muddy the binder cases sharing this
    // controller.
    @Get("/empty")
    public PageResponse<String> emptyPage(Pageable pageable) {
        return PageResponse.of(Page.of(List.of(), pageable, 0L), List.of());
    }
}
