package com.axibase.tsd.api.method.sql.operator;

import com.axibase.tsd.api.method.series.SeriesMethod;
import com.axibase.tsd.api.method.sql.SqlTest;
import com.axibase.tsd.api.model.series.Sample;
import com.axibase.tsd.api.model.series.Series;
import com.axibase.tsd.api.model.sql.StringTable;
import io.qameta.allure.Issue;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;


public class SqlNotEqualsOperatorTest extends SqlTest {
    private static final String TEST_PREFIX = "sql-not-equals-syntax-";
    private static final String TEST_ENTITY_NAME = TEST_PREFIX + "entity";
    private static final String TEST_METRIC_NAME = TEST_PREFIX + "metric";

    @BeforeClass
    public static void prepareData() throws Exception {
        Series series = new Series(TEST_ENTITY_NAME, TEST_METRIC_NAME, "a", "b") {{
            addSamples(Sample.ofDateDecimal("2016-06-03T09:23:00.000Z", new BigDecimal("1.01")));
        }};
        SeriesMethod.insertSeriesCheck(Collections.singletonList(series));
    }

    @Issue("2933")
    @Test
    public void testNotEqualsWithDatetimeIsFalse() {
        final String sqlQuery = String.format(
                "SELECT entity, value, datetime FROM \"%s\"" +
                        "WHERE datetime <> '2016-06-03T09:23:00.000Z' AND entity = '%s'",
                TEST_METRIC_NAME, TEST_ENTITY_NAME
        );

        String[][] expectedRows = {};

        assertSqlQueryRows(expectedRows, sqlQuery);
    }

    @Issue("2933")
    @Test
    public void testNotEqualsWithDatetimeIsTrue() {
        final String sqlQuery = String.format(
                "SELECT entity, value, datetime FROM \"%s\"" +
                        "WHERE datetime <> '2016-06-03T09:25:00.000Z' AND entity = '%s'",
                TEST_METRIC_NAME, TEST_ENTITY_NAME
        );

        String[][] expectedRows = {
                {TEST_ENTITY_NAME, "1.01", "2016-06-03T09:23:00.000Z" }
        };

        assertSqlQueryRows(expectedRows, sqlQuery);
    }

    @Issue("4554")
    @Test
    public void testNotEqualsDatetimeAllowedTrueCondition() {
        final String sqlQuery = String.format(
                "SELECT entity, value, datetime FROM \"%s\"" +
                        "WHERE datetime != '2016-06-03T09:25:00.000Z'",
                TEST_METRIC_NAME
        );

        String[][] expectedRows = {
                {TEST_ENTITY_NAME, "1.01", "2016-06-03T09:23:00.000Z" }
        };

        assertSqlQueryRows(expectedRows, sqlQuery);
    }

    @Issue("4554")
    @Test
    public void testNotEqualsDatetimeAllowedFalseCondition() {
        final String sqlQuery = String.format(
                "SELECT entity, value, datetime FROM \"%s\"" +
                        "WHERE datetime != '2016-06-03T09:23:00.000Z'",
                TEST_METRIC_NAME
        );

        String[][] expectedRows = { };

        assertSqlQueryRows(expectedRows, sqlQuery);
    }

    @Issue("4554")
    @Test
    public void testNotEqualsDatetimeAllowedComplexANDTrueCondition() {
        final String sqlQuery = String.format(
                "SELECT entity, value, datetime FROM \"%s\"" +
                        "WHERE datetime != '2016-06-03T09:25:00.000Z' AND " +
                        "datetime BETWEEN '2016-06-03T09:22:00.000Z' AND '2016-06-03T09:26:00.000Z'",
                TEST_METRIC_NAME
        );

        String[][] expectedRows = {
                {TEST_ENTITY_NAME, "1.01", "2016-06-03T09:23:00.000Z" }
        };

        assertSqlQueryRows(expectedRows, sqlQuery);
    }

    @Issue("4554")
    @Test
    public void testNotEqualsDatetimeAllowedComplexANDFalseCondition() {
        final String sqlQuery = String.format(
                "SELECT entity, value, datetime FROM \"%s\" " +
                        "WHERE datetime != '2016-06-03T09:23:00.000Z' AND " +
                        "datetime BETWEEN '2016-06-03T09:22:00.000Z' AND '2016-06-03T09:26:00.000Z'",
                TEST_METRIC_NAME
        );

        String[][] expectedRows = { };

        assertSqlQueryRows(expectedRows, sqlQuery);
    }

    @Issue("4554")
    @Test
    public void testNotEqualsDatetimeAllowedNotOperatorTrue() {
        final String sqlQuery = String.format(
                "SELECT entity, value, datetime FROM \"%s\" " +
                        "WHERE NOT datetime = '2016-06-03T09:25:00.000Z'",
                TEST_METRIC_NAME
        );

        String[][] expectedRows = {
                {TEST_ENTITY_NAME, "1.01", "2016-06-03T09:23:00.000Z" }
        };

        assertSqlQueryRows(expectedRows, sqlQuery);
    }

    @Issue("4554")
    @Test
    public void testNotEqualsDatetimeAllowedNotOperatorFalse() {
        final String sqlQuery = String.format(
                "SELECT entity, value, datetime FROM \"%s\" " +
                        "WHERE NOT datetime = '2016-06-03T09:23:00.000Z'",
                TEST_METRIC_NAME
        );

        String[][] expectedRows = { };

        assertSqlQueryRows(expectedRows, sqlQuery);
    }

    @Issue("2933")
    @Test
    public void testNotEqualsWithNumericIsFalse() {
        final String sqlQuery = String.format(
                "SELECT entity, value FROM \"%s\"WHERE value <> 1.2 AND entity = '%s'",
                TEST_METRIC_NAME, TEST_ENTITY_NAME
        );

        List<List<String>> resultRows = queryResponse(sqlQuery)
                .readEntity(StringTable.class)
                .filterRows("value");

        List<List<String>> expectedRows = Collections.singletonList(
                Collections.singletonList("1.01")
        );

        assertEquals("Table value rows must be identical", expectedRows, resultRows);
    }

    @Issue("2933")
    @Test
    public void testNotEqualsWithNumericIsTrue() {
        final String sqlQuery = String.format(
                "SELECT entity, value FROM \"%s\" %nWHERE value <> 1.01 AND entity = '%s'",
                TEST_METRIC_NAME, TEST_ENTITY_NAME
        );

        List<List<String>> resultRows = queryResponse(sqlQuery)
                .readEntity(StringTable.class)
                .getRows();
        assertTrue("Result rows must be empty", resultRows.isEmpty());
    }

    @Issue("2933")
    @Test
    public void testNotEqualsWitStringIsFalse() {
        final String sqlQuery = String.format(
                "SELECT entity, value, datetime FROM \"%s\" %nWHERE tags.a <> 'b'",
                TEST_METRIC_NAME
        );

        List<List<String>> resultRows = queryResponse(sqlQuery)
                .readEntity(StringTable.class)
                .getRows();
        assertTrue("Result rows must be empty", resultRows.isEmpty());
    }

    @Issue("2933")
    @Test
    public void testNotEqualsWithStringIsTrue() {
        final String sqlQuery = String.format(
                "SELECT entity, tags.a FROM \"%s\" %n WHERE tags.a <> 'a' AND entity = '%s'",
                TEST_METRIC_NAME, TEST_ENTITY_NAME
        );

        List<List<String>> resultRows = queryResponse(sqlQuery)
                .readEntity(StringTable.class)
                .filterRows("tags.a");

        List<List<String>> expectedRows = Arrays.asList(
                Arrays.asList("b")
        );

        assertEquals("Table value rows must be identical", expectedRows, resultRows);
    }
}
