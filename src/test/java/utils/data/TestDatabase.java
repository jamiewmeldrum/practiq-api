package utils.data;

import io.micronaut.jdbc.DataSourceResolver;
import jakarta.inject.Singleton;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Raw-JDBC test data helper for integration tests. Inserts and clears rows with hand-written
 * SQL, deliberately independent of the application's repositories: a failed IT then points at
 * the code under test, not at the persistence code used to arrange it. Schema coupling is
 * intentional — a column change should break these inserts loudly.
 */
@Singleton
public class TestDatabase {

    // Postgres SQLState codes for the constraint violations the ITs assert on. Codes are the stable,
    // portable contract (unlike the human-readable, version-specific error text), so a constraint IT
    // checks these rather than message strings. See the Postgres "Error Codes" appendix.
    public static final String NOT_NULL_VIOLATION = "23502";
    public static final String FOREIGN_KEY_VIOLATION = "23503";
    public static final String UNIQUE_VIOLATION = "23505";
    public static final String CHECK_VIOLATION = "23514";

    private final DataSource dataSource;

    public TestDatabase(DataSource dataSource, DataSourceResolver dataSourceResolver) {
        // The injected DataSource is Micronaut's connection-managed wrapper: its getConnection()
        // routes through the data-layer connection manager and demands an ambient @Transactional/
        // @Connectable scope. Resolve to the raw vendor pool so we get plain, independent JDBC
        // connections — the whole point of arranging data outside the application's persistence.
        this.dataSource = dataSourceResolver.resolve(dataSource);
    }

    public List<DBRow> selectAll(String table) {
        String sql = "SELECT * FROM " + table;

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            List<DBRow> DBRows = new ArrayList<>();

            while (resultSet.next()) {
                DBRows.add(DBRow.from(resultSet));
            }

            return DBRows;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to select from " + table,
                    e
            );
        }
    }

    /**
     * Inserts a single row. Each entry is a column name and the value to write; values are
     * bound as JDBC parameters, column and table names are interpolated (identifiers can't be
     * parameterised). Omitted columns are filled by the table definition: identity/default
     * columns are generated (e.g. id, created_at), nullable columns become NULL, and a NOT NULL
     * column with no default makes the insert fail — omission is not a free pass.
     */
    public void insert(String table, Map<String, Object> columns) {
        List<String> names = new ArrayList<>(columns.size());
        List<Object> values = new ArrayList<>(columns.size());
        for (Map.Entry<String, Object> column : columns.entrySet()) {
            names.add(column.getKey());
            values.add(column.getValue());
        }

        String sql = "INSERT INTO " + table + " (" + String.join(", ", names) + ") OVERRIDING SYSTEM VALUE VALUES ("
                + String.join(", ", Collections.nCopies(names.size(), "?")) + ")";

        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            connection.setAutoCommit(true);
            for (int i = 0; i < values.size(); i++) {
                statement.setObject(i + 1, values.get(i));
            }
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to insert into " + table + ": " + sql, e);
        }
    }

    /**
     * Updates a single row. Must specify table name and id for unique identification
     */
    public void update(String table, long id, String column, Object value) {
        String sql = """
        UPDATE %s
        SET %s = ?
        WHERE id = ?;
        """.formatted(table, column);

        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            connection.setAutoCommit(true);
            statement.setObject(1, value);
            statement.setLong(2, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update " + table + ": " + sql, e);
        }
    }

    public void delete(String table, long id) {
        String sql = """
        DELETE
        FROM %s
        WHERE id = ?;
        """.formatted(table);

        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            connection.setAutoCommit(true);
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete from " + table + ": " + sql, e);
        }
    }

    /**
     * Walks the cause chain of a failure thrown by insert/update/clear to the wrapped JDBC SQLException
     * and returns its SQLState — the stable, portable code identifying a constraint violation (e.g.
     * {@link #NOT_NULL_VIOLATION}). Lets a constraint IT assert on the code, not the Postgres-specific
     * message text. Fails if no SQLException is present, since that means the failure wasn't a DB one.
     */
    public static String sqlStateOf(Throwable thrown) {
        for (Throwable cause = thrown; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException) {
                return sqlException.getSQLState();
            }
        }
        throw new AssertionError("No SQLException in the cause chain of " + thrown, thrown);
    }

    /** Removes every row from the table and resets its identity sequence. */
    public void clear(String table) {
        String sql = "TRUNCATE TABLE " + table + " RESTART IDENTITY CASCADE";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            connection.setAutoCommit(true);
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to clear " + table + ": " + sql, e);
        }
    }
}
