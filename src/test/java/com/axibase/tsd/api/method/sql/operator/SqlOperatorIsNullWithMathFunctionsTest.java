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


public class SqlOperatorIsNullWithMathFunctionsTest extends SqlTest {
    private static final String TEST_PREFIX = "sql-operator-is-null-with-math-functions-";
    private static final String TEST_ENTITY_NAME = TEST_PREFIX + "entity";
    private static final String TEST_METRIC_NAME = TEST_PREFIX + "metric";


    @BeforeClass
    public static void prepareData() throws Exception {
        Series series = new Series(TEST_ENTITY_NAME, TEST_METRIC_NAME);
        series.addSamples(
                Sample.ofDateDecimal("2016-06-29T08:00:00.000Z", new BigDecimal("2.11")),
                Sample.ofDateDecimal("2016-06-29T08:00:01.000Z", new BigDecimal("7.567")),
                Sample.ofDateDecimal("2016-06-29T08:00:02.000Z", new BigDecimal("-1.23"))
        );

        SeriesMethod.insertSeriesCheck(Collections.singletonList(series));
    }

    @Issue("3049")
    @Test
    public void testIsNotNullWithMathFunction() {
        String sqlQuery = String.format(
                "SELECT SQRT(value) FROM \"%s\" %nWHERE entity = '%s'AND SQRT(value) IS NOT NULL",
                TEST_METRIC_NAME, TEST_ENTITY_NAME
        );

        StringTable resultTable = queryResponse(sqlQuery).readEntity(StringTable.class);

        List<List<String>> expectedRows = Arrays.asList(
                Collections.singletonList(Double.toString(Math.sqrt(2.11))),
                Collections.singletonList(Double.toString(Math.sqrt(7.567)))
        );

        assertTableRowsExist(expectedRows, resultTable);
    }


    @Issue("3049")
    @Test
    public void testIsNullWithMathFunction() {
        String sqlQuery = String.format(
                "SELECT SQRT(value) FROM \"%s\" %nWHERE entity = '%s'AND SQRT(value) IS NULL",
                TEST_METRIC_NAME, TEST_ENTITY_NAME
        );

        StringTable resultTable = queryResponse(sqlQuery).readEntity(StringTable.class);

        List<List<String>> expectedRows = Collections.singletonList(
                Collections.singletonList("NaN")
        );

        assertTableRowsExist(expectedRows, resultTable);
    }


    @Issue("3049")
    @Test
    public void testIsNotNullWithMathFunctionAliasInOrderBy() {
        String sqlQuery = String.format(
                "SELECT SQRT(value) AS \"sqrt\" FROM \"%s\" %nWHERE entity = '%s'AND SQRT(value) IS NOT NULL %nORDER BY \"sqrt\"",
                TEST_METRIC_NAME, TEST_ENTITY_NAME
        );

        StringTable resultTable = queryResponse(sqlQuery).readEntity(StringTable.class);

        List<List<String>> expectedRows = Arrays.asList(
                Collections.singletonList(Double.toString(Math.sqrt(2.11d))),
                Collections.singletonList(Double.toString(Math.sqrt(7.567d)))
        );

        assertTableRowsExist(expectedRows, resultTable);
    }

    @Issue("3049")
    @Test
    public void testIsNullWithMathFunctionAliasInOrderBy() {
        String sqlQuery = String.format(
                "SELECT SQRT(value) AS \"sqrt\" FROM \"%s\" %nWHERE entity = '%s'AND SQRT(value) IS NULL %nORDER BY \"sqrt\"",
                TEST_METRIC_NAME, TEST_ENTITY_NAME
        );

        StringTable resultTable = queryResponse(sqlQuery).readEntity(StringTable.class);

        List<List<String>> expectedRows = Collections.singletonList(
                Collections.singletonList("NaN")
        );

        assertTableRowsExist(expectedRows, resultTable);
    }


    @Issue("3049")
    @Test
    public void testIsNullWithMathFunctionComposeAliasInOrderBy() {
        String sqlQuery = String.format(
                "SELECT SQRT(value) AS \"sqrt\" FROM \"%s\" %nWHERE entity = '%s'AND (SQRT(value) + ABS(value))/value IS NULL %nORDER BY \"sqrt\"",
                TEST_METRIC_NAME, TEST_ENTITY_NAME
        );

        StringTable resultTable = queryResponse(sqlQuery).readEntity(StringTable.class);

        String[][] expectedRows = {
                {"NaN"}
        };

        assertTableRowsExist(expectedRows, resultTable);
    }
}
