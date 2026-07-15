package utils;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

/**
 * Counts the JDBC prepared statements executed while an action runs, via Hibernate's {@link Statistics}.
 * The <em>prepared-statement</em> count (not the query-execution count) is deliberate: an eager
 * association fires secondary SELECTs that never appear as distinct HQL/criteria queries but do show up
 * as prepared statements — so this is the metric that catches an N+1 regression.
 *
 * <p>Not a bean: instantiated from an injected {@link EntityManagerFactory} inside a *PT test, so it never
 * loads in the DB-less component-test context.
 */
public class StatementCounter {

    private final Statistics statistics;

    public StatementCounter(EntityManagerFactory entityManagerFactory) {
        this.statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    public long countDuring(Runnable action) {
        statistics.clear();
        action.run();
        return statistics.getPrepareStatementCount();
    }
}
