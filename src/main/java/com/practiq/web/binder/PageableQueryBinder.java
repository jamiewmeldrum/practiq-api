package com.practiq.web.binder;

import com.practiq.foundation.exception.EntityValidationException;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.exceptions.ConversionErrorException;
import io.micronaut.core.type.Argument;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.runtime.config.DataConfiguration;
import io.micronaut.data.runtime.http.PageableRequestArgumentBinder;
import io.micronaut.http.HttpParameters;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.bind.binders.TypedRequestArgumentBinder;
import jakarta.inject.Singleton;
import java.util.Optional;

// Micronaut's own Pageable binder is deliberately lenient: it coerces every unusable paging value to a
// default and answers 200, so page=-1 and size=abc are indistinguishable from sending nothing. That is the
// opposite of how the rest of the API treats a value it cannot use, and it leaves a client unable to learn
// what the size ceiling is. This binder refuses instead, naming the parameter and the rule.
@Singleton
@Replaces(PageableRequestArgumentBinder.class)
public class PageableQueryBinder implements TypedRequestArgumentBinder<Pageable> {

    private static final Argument<Pageable> TYPE = Argument.of(Pageable.class);

    private static final int MIN_PAGE = 0;
    private static final int MIN_SIZE = 1;

    private final DataConfiguration.PageableConfiguration configuration;

    public PageableQueryBinder(DataConfiguration.PageableConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Argument<Pageable> argumentType() {
        return TYPE;
    }

    @Override
    public BindingResult<Pageable> bind(ArgumentConversionContext<Pageable> context, HttpRequest<?> source) {
        HttpParameters parameters = source.getParameters();

        rejectSort(parameters);
        Pageable pageable = Pageable.from(readPage(parameters), readSize(parameters));

        return () -> Optional.of(pageable);
    }

    // No endpoint offers sorting: the query runners impose their own total order so rows cannot straddle a
    // page. To allow it later, put an allow-list annotation on the Pageable parameter and read it from
    // context.getArgument().getAnnotationMetadata() — the list wants to be mandatory, since an open sort
    // lets a client order by an unindexed column. The runner must then append its id tiebreak to whatever
    // the client asked for or paging stops being stable, so the binder alone will not be enough.
    private void rejectSort(HttpParameters parameters) {
        String sortParameter = configuration.getSortParameterName();
        if (parameters.contains(sortParameter)) {
            throw new EntityValidationException(sortParameter, "is not supported");
        }
    }

    private int readPage(HttpParameters parameters) {
        String pageParameter = configuration.getPageParameterName();
        Integer page = readInt(parameters, pageParameter);

        if (page == null) {
            return MIN_PAGE;
        }
        if (page < MIN_PAGE) {
            throw new EntityValidationException(pageParameter, "must be greater than or equal to " + MIN_PAGE);
        }
        return page;
    }

    private int readSize(HttpParameters parameters) {
        String sizeParameter = configuration.getSizeParameterName();
        Integer size = readInt(parameters, sizeParameter);

        if (size == null) {
            return configuration.getDefaultPageSize();
        }
        if (size < MIN_SIZE) {
            throw new EntityValidationException(sizeParameter, "must be greater than or equal to " + MIN_SIZE);
        }
        // Stated back to the caller rather than clamped: a client handed 50 rows after asking for 500 cannot
        // tell the ceiling from the end of the data, and the message is the only place the maximum is public.
        int maxSize = configuration.getMaxPageSize();
        if (size > maxSize) {
            throw new EntityValidationException(sizeParameter, "must not be greater than " + maxSize);
        }
        return size;
    }

    // Absent reads as null; present but unparseable raises the same conversion error a bad filter parameter
    // raises, so a non-numeric page is the 400 that conceptId=abc already is.
    private Integer readInt(HttpParameters parameters, String name) {
        String raw = parameters.getFirst(name, String.class).orElse(null);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(raw);
        } catch (NumberFormatException e) {
            throw new ConversionErrorException(Argument.of(Integer.class, name), e);
        }
    }
}
