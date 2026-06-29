package com.practiq.test;

import io.micronaut.jdbc.DataSourceResolver;
import jakarta.inject.Singleton;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
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

    private final DataSource dataSource;

    public TestDatabase(DataSource dataSource, DataSourceResolver dataSourceResolver) {
        // The injected DataSource is Micronaut's connection-managed wrapper: its getConnection()
        // routes through the data-layer connection manager and demands an ambient @Transactional/
        // @Connectable scope. Resolve to the raw vendor pool so we get plain, independent JDBC
        // connections — the whole point of arranging data outside the application's persistence.
        this.dataSource = dataSourceResolver.resolve(dataSource);
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

        String sql = "INSERT INTO " + table + " (" + String.join(", ", names) + ") VALUES ("
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
