package com.axibase.tsd.api.method.sql.operator;

import com.axibase.tsd.api.method.series.SeriesMethod;
import com.axibase.tsd.api.method.sql.SqlTest;
import com.axibase.tsd.api.model.series.Sample;
import com.axibase.tsd.api.model.series.Series;
import com.axibase.tsd.api.model.sql.StringTable;
import io.qameta.allure.Issue;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.ProcessingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;


public class SqlModuloTest extends SqlTest {
    private static final String TEST_PREFIX = "sql-modulo-";
    private static final String TEST_ENTITY_NAME = TEST_PREFIX + "entity";
    private static final String TEST_METRIC1_NAME = TEST_PREFIX + "metric-1";
    private static final String TEST_METRIC2_NAME = TEST_PREFIX + "metric-2";
    private static double EPS = 10e-4;


    @BeforeClass
    public static void prepareData() throws Exception {
        final List<Series> seriesList = new ArrayList<>();
        seriesList.add(new Series(TEST_ENTITY_NAME, TEST_METRIC1_NAME, "a", "b", "b", "c") {{
            addSamples(
                    Sample.ofDateInteger("2016-06-03T09:23:00.000Z", 7),
                    Sample.ofDateInteger("2016-06-03T09:24:00.000Z", 0),
                    Sample.ofDateInteger("2016-06-03T09:25:00.000Z", 12),
                    Sample.ofDateDecimal("2016-06-03T09:26:00.000Z", new BigDecimal("10.3")),
                    Sample.ofDateInteger("2016-06-03T09:27:00.000Z", 10)
            );
        }});


        seriesList.add(new Series(TEST_ENTITY_NAME, TEST_METRIC2_NAME, "a", "b", "b", "c") {{
            addSamples(
                    Sample.ofDateInteger("2016-06-03T09:23:00.000Z", 5),
                    Sample.ofDateInteger("2016-06-03T09:24:00.000Z", 7),
                    Sample.ofDateInteger("2016-06-03T09:25:00.000Z", -2),
                    Sample.ofDateDecimal("2016-06-03T09:26:00.000Z", new BigDecimal("-2.1"))
            );
        }});

        SeriesMethod.insertSeriesCheck(seriesList);
    }

    @Issue("2922")
    @Test
    public void testDividingPositiveByPositiveInteger() {
        String sqlQuery = String.format(
                "SELECT m1.value AS \"num\", m2.value AS \"den\", m1.value %s m2.value AS \"modulo\" FROM \"%s\" m1 %n " +
                        "OUTER JOIN \"%s\" m2 %nWHERE m1.datetime = '2016-06-03T09:23:00.000Z' AND m1.entity = '%s'",
                "%", TEST_METRIC1_NAME, TEST_METRIC2_NAME, TEST_ENTITY_NAME
        );

        StringTable resultTable = queryResponse(sqlQuery).readEntity(StringTable.class);

        String[][] expectedRows = {
                {"7.0", "5.0", "2.0"}
        };

        assertTableRowsExist(expectedRows, resultTable);
    }

    @Issue("2922")
    @Test
    public void testDividingZeroByPositiveInteger() {
        String sqlQuery = String.format(
                "SELECT m1.value AS \"num\", m2.value AS \"den\", m1.value %s m2.value AS \"modulo\" FROM \"%s\" m1 %n " +
                        "OUTER JOIN \"%s\" m2 %nWHERE m1.datetime = '2016-06-03T09:24:00.000Z' AND m1.entity = '%s'",
                "%", TEST_METRIC1_NAME, TEST_METRIC2_NAME, TEST_ENTITY_NAME
        );


        StringTable resultTable = queryResponse(sqlQuery).readEntity(StringTable.class);


        String[][] expectedRows = {
                {"0.0", "7.0", "0.0"}
        };

        assertTableRowsExist(expectedRows, resultTable);
    }

    @Issue("2922")
    @Test
    public void testDividingPositiveByZeroInteger() {
        String sqlQuery = String.format(
                "SELECT m2.value AS \"num\", m1.value AS \"den\", m2.value %s m1.value AS \"modulo\" FROM \"%s\" m1 %n " +
                        "OUTER JOIN \"%s\" m2 %nWHERE m1.datetime = '2016-06-03T09:24:00.000Z' AND m1.entity = '%s'",
                "%", TEST_METRIC1_NAME, TEST_METRIC2_NAME, TEST_ENTITY_NAME
        );

        StringTable resultTable = queryResponse(sqlQuery).readEntity(StringTable.class);

        String[][] expectedRows = {
                {"7.0", "0.0", "NaN"}
        };

        assertTableRowsExist(expectedRows, resultTable);
    }

    @Issue("2922")
    @Test
    public void testDividingPositiveByNegativeInteger() {
        String sqlQuery = String.format(
                "SELECT m1.value AS \"num\", m2.value AS \"den\", m1.value %s m2.value AS \"modulo\" FROM \"%s\" m1 %n " +
                        "OUTER JOIN \"%s\" m2 %nWHERE m1.datetime = '2016-06-03T09:25:00.000Z' AND m1.entity = '%s'",
                "%", TEST_METRIC1_NAME, TEST_METRIC2_NAME, TEST_ENTITY_NAME
        );

        StringTable resultTable = queryResponse(sqlQuery).readEntity(StringTable.class);

        String[][] expectedRows = {
                {"12.0", "-2.0", "0.0"}
        };

        assertTableRowsExist(expectedRows, resultTable);
    }

    @Issue("2922")
    @Test
    public void testDividingNegativeByPositiveInteger() {
        String sqlQuery = String.format(
                "SELECT m2.value AS \"num\", m1.value AS \"den\", m2.value %s m1.value AS \"modulo\" FROM \"%s\" m1 %n " +
                        "OUTER JOIN \"%s\" m2 %nWHERE m1.datetime = '2016-06-03T09:25:00.000Z' AND m1.entity = '%s'",
                "%", TEST_METRIC1_NAME, TEST_METRIC2_NAME, TEST_ENTITY_NAME
        );

        StringTable resultTable = queryResponse(sqlQuery).readEntity(StringTable.class);

        String[][] expectedRows = {
                {"-2.0", "12.0", "-2.0"}
        };

        assertTableRowsExist(expectedRows, resultTable);
    }

    @Issue("2922")
    @Test
    public void testDividingPositiveByNegativeDecimal() {
        String sqlQuery = String.format(

                "SELECT m1.value AS \"num\", m2.value AS \"den\", m1.value %s m2.value AS \"modulo\" FROM \"%s\" m1 %n " +
                        "OUTER JOIN \"%s\" m2 %nWHERE m1.datetime = '2016-06-03T09:26:00.000Z' AND m1.entity = '%s'",
                "%", TEST_METRIC1_NAME, TEST_METRIC2_NAME, TEST_ENTITY_NAME
        );

        Double expectedModulo = 1.9;

        Double resultModulo = Double.parseDouble(queryResponse(sqlQuery)
                .readEntity(StringTable.class)
                .getValueAt(2, 0)
        );

        assertEquals(expectedModulo, resultModulo, EPS);
    }

    @Issue("2922")
    @Test
    public void testDividingNegativeByPositiveDecimal() {
        String sqlQuery = String.format(
                "SELECT m2.value AS \"num\", m1.value AS \"den\", m2.value %s m1.value AS \"modulo\" FROM \"%s\" m1 %n " +
                        "OUTER JOIN \"%s\" m2 %nWHERE m1.datetime = '2016-06-03T09:26:00.000Z' AND m1.entity = '%s'",
                "%", TEST_METRIC1_NAME, TEST_METRIC2_NAME, TEST_ENTITY_NAME
        );
        Double expectedModulo = -2.1;

        Double resultModulo = Double.parseDouble(queryResponse(sqlQuery)
                .readEntity(StringTable.class)
                .getValueAt(2, 0)
        );

        assertEquals(expectedModulo, resultModulo, EPS);
    }

    @Issue("2922")
    @Test
    public void testDividingNullByNumber() {
        String sqlQuery = String.format(
                "SELECT m1.value AS \"num\", m2.value AS \"den\", m1.value %s m2.value AS \"modulo\" FROM \"%s\" m1 %n " +
                        "OUTER JOIN \"%s\" m2 %nWHERE m1.datetime = '2016-06-03T09:27:00.000Z' AND m1.entity = '%s'",
                "%", TEST_METRIC1_NAME, TEST_METRIC2_NAME, TEST_ENTITY_NAME
        );

        StringTable resultTable = queryResponse(sqlQuery).readEntity(StringTable.class);

        String[][] expectedRows = {
                {"10.0", "null", "null"}
        };

        assertTableRowsExist(expectedRows, resultTable);
    }

    @Issue("2922")
    @Test
    public void testDividingNumberByNull() {
        String sqlQuery = String.format(
                "SELECT m2.value AS \"num\", m1.value AS \"den\", m2.value %s m1.value AS \"modulo\" FROM \"%s\" m1 %n " +
                        "OUTER JOIN \"%s\" m2 %nWHERE m1.datetime = '2016-06-03T09:27:00.000Z' AND m1.entity = '%s'",
                "%", TEST_METRIC1_NAME, TEST_METRIC2_NAME, TEST_ENTITY_NAME
        );

        StringTable resultTable = queryResponse(sqlQuery).readEntity(StringTable.class);

        String[][] expectedRows = {
                {"null", "10.0", "null"}
        };

        assertTableRowsExist(expectedRows, resultTable);
    }

    @Issue("2922")
    @Test(expectedExceptions = ProcessingException.class)
    public void testDividingStringByString() {
        String sqlQuery = String.format(
                "SELECT m2.value AS \"num\", m1.value AS \"den\", tags.a %s tags.b AS \"modulo\" FROM \"%s\" m1 %n " +
                        "OUTER JOIN \"%s\" m2 %nWHERE m1.datetime = '2016-06-03T09:27:00.000Z' AND m1.entity = '%s'",
                "%", TEST_METRIC1_NAME, TEST_METRIC2_NAME, TEST_ENTITY_NAME
        );

        queryResponse(sqlQuery)
                .readEntity(StringTable.class);
    }

    @Issue("2922")
    @Test(expectedExceptions = ProcessingException.class)
    public void testDividingNaNByNumber() {
        String sqlQuery = String.format(
                "SELECT value, 0/0 %s m1.value AS \"modulo\" FROM \"%s\" %n " +
                        "WHERE m1.datetime = '2016-06-03T09:23:00.000Z' AND m1.entity = '%s' ",
                "%", TEST_METRIC1_NAME, TEST_ENTITY_NAME
        );

        queryResponse(sqlQuery)
                .readEntity(StringTable.class);
    }

}
