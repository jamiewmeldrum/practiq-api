package utils.data;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

public class DBRow {

    private final Map<String, Object> values;
    private final Set<String> asserted = new HashSet<>();

    private DBRow(Map<String, Object> values) {
        this.values = values;
    }

    public <T> void assertThat(String column, Matcher<? super T> matcher) {
        asserted.add(column);
        org.hamcrest.MatcherAssert.assertThat(this.<T>get(column), matcher);
    }

    public <T> T get(String column) {
        return (T) values.get(column);
    }

    public void assertAllColumnsChecked() {
        Set<String> unchecked = new HashSet<>(values.keySet());
        unchecked.removeAll(asserted);

        if (!unchecked.isEmpty()) {
            throw new AssertionError(
                    "Unchecked columns: " + unchecked
            );
        }
    }

    public static DBRow from(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metadata = resultSet.getMetaData();
        Map<String, Object> values = new LinkedHashMap<>();

        for (int i = 1; i <= metadata.getColumnCount(); i++) {
            String columnName = metadata.getColumnName(i);
            Object value = resultSet.getObject(i);

            //A little bit of on the fly util conversion to ensure we use standard types. Low cost and helpful to avoid
            //strange test failures.
            if (value instanceof Timestamp timestamp) {
                value = timestamp.toInstant();
            }

            values.put(columnName, value);
        }

        return new DBRow(values);
    }

    public static List<Object> collectColumn(List<DBRow> rows, String column) {
        return rows.stream().map(q -> q.get(column)).collect(Collectors.toList());
    }

    public static <T> Matcher<DBRow> hasColumn(String column, Matcher<? super T> matcher) {
        return new FeatureMatcher<DBRow, T>(matcher, column, column) {
            @Override
            protected T featureValueOf(DBRow row) {
                return row.get(column);
            }
        };
    }
}

