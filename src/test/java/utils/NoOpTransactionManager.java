package utils;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.data.connection.ConnectionStatus;
import io.micronaut.transaction.SynchronousTransactionManager;
import io.micronaut.transaction.TransactionCallback;
import io.micronaut.transaction.TransactionDefinition;
import io.micronaut.transaction.TransactionStatus;
import io.micronaut.transaction.hibernate.HibernateTransactionManager;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.hibernate.Session;

import java.util.Optional;

// Inert transaction manager for the component-test slice. A @Transactional service method opens a
// transaction *above* the mocked repositories, and readOnly=true eagerly acquires a JDBC connection to
// flag it read-only — which would dial the slice's dead datasource. This replaces the real manager under
// the ctslice environment only, running the transactional callback directly and never touching a
// connection. Transactional semantics themselves belong to *IT (real Postgres); the CT just needs the
// boundary to exist and be inert. @Named("default") matches the real @EachBean(DataSource) qualifier.
@Singleton
@Named("default")
@Requires(env = "ctslice")
@Replaces(HibernateTransactionManager.class)
public class NoOpTransactionManager implements SynchronousTransactionManager<Session> {

    private static final TransactionStatus<Session> INERT_STATUS = new TransactionStatus<>() {
        @Override
        public Object getTransaction() {
            return null;
        }

        @Override
        public ConnectionStatus<Session> getConnectionStatus() {
            // Never reached in a component test: every query is mocked, so nothing asks for a connection.
            // If this throws, a CT is exercising a path that needs a real DB and belongs in an *IT instead.
            throw new UnsupportedOperationException("No database connection in the component-test slice");
        }

        @Override
        public boolean isNewTransaction() {
            return true;
        }

        @Override
        public void setRollbackOnly() {
        }

        @Override
        public boolean isRollbackOnly() {
            return false;
        }

        @Override
        public boolean isCompleted() {
            return false;
        }

        @Override
        public TransactionDefinition getTransactionDefinition() {
            return TransactionDefinition.READ_ONLY;
        }
    };

    @Override
    public <R> R execute(TransactionDefinition definition, TransactionCallback<Session, R> callback) {
        return callback.apply(INERT_STATUS);
    }

    @Override
    public TransactionStatus<Session> getTransaction(TransactionDefinition definition) {
        return INERT_STATUS;
    }

    @Override
    public void commit(TransactionStatus<Session> status) {
    }

    @Override
    public void rollback(TransactionStatus<Session> status) {
    }

    @Override
    public Session getConnection() {
        throw new UnsupportedOperationException("No database connection in the component-test slice");
    }

    @Override
    public boolean hasConnection() {
        return false;
    }

    @Override
    public Optional<? extends TransactionStatus<?>> findTransactionStatus() {
        return Optional.empty();
    }
}
