package com.axibase.tsd.api.method.sql.function.period.extend;

import com.axibase.tsd.api.method.series.SeriesMethod;
import com.axibase.tsd.api.method.sql.SqlTest;
import com.axibase.tsd.api.model.series.Sample;
import com.axibase.tsd.api.model.series.Series;
import com.axibase.tsd.api.model.sql.StringTable;
import io.qameta.allure.Issue;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class SqlPeriodExtendTest extends SqlTest {
    private static final String TEST_PREFIX = "sql-period-extended-";
    private static final String TEST_METRIC_NAME = TEST_PREFIX + "metric";
    private static final String TEST_ENTITY1_NAME = TEST_PREFIX + "entity-1";
    private static final String TEST_ENTITY2_NAME = TEST_PREFIX + "entity-2";

    @BeforeClass
    public static void prepareData() throws Exception {
        List<Series> seriesList = new ArrayList<>();

        seriesList.add(
                new Series(TEST_ENTITY1_NAME, TEST_METRIC_NAME, "a", "b", "b", "c") {{
                    addSamples(
                            Sample.ofDateInteger("2016-07-14T15:00:06.001Z", 1),
                            Sample.ofDateInteger("2016-07-14T15:00:08.001Z", 2)
                    );
                }}
        );

        seriesList.add(
                new Series(TEST_ENTITY2_NAME, TEST_METRIC_NAME, "a", "b", "b", "c") {{
                    addSamples(
                            Sample.ofDateInteger("2016-07-14T15:00:06.001Z", 3)
                    );
                }}
        );

        SeriesMethod.insertSeriesCheck(seriesList);
    }

    @Issue("3066")
    @Test(description = "It tests the EXTEND option in PERIOD function. " +
            "https://github.com/axibase/atsd-docs/blob/master/api/sql/examples/interpolate-extend.md")
    public void testPeriodExtendOptionBegin() {
        String sqlQuery = String.format(
                "SELECT datetime, AVG(value) FROM \"%s\" %nWHERE entity = '%s' %n" +
                        "AND datetime >= '2016-07-14T15:00:05.000Z' AND datetime <'2016-07-14T15:00:07.000Z' %n" +
                        "GROUP BY PERIOD(1 SECOND,EXTEND)",
                TEST_METRIC_NAME, TEST_ENTITY1_NAME
        );

        StringTable resultTable = queryResponse(sqlQuery).readEntity(StringTable.class);

        List<List<String>> expectedRows = Arrays.asList(
                Arrays.asList("2016-07-14T15:00:05.000Z", "1"), //<--interpolated by NEXT value
                Arrays.asList("2016-07-14T15:00:06.000Z", "1")
        );

        assertTableRowsExist(expectedRows, resultTable);
    }

    @Issue("3066")
    @Test
    public void testPeriodExtendOptionTrail() {
        String sqlQuery = String.format(
                "SELECT datetime, AVG(value) FROM \"%s\" %nWHERE entity = '%s' %n" +
                        "AND datetime >= '2016-07-14T15:00:08.000Z' AND datetime <'2016-07-14T15:00:10.000Z'" +
                        "GROUP BY PERIOD(1 SECOND,EXTEND)",
                TEST_METRIC_NAME, TEST_ENTITY1_NAME
        );

        StringTable resultTable = queryResponse(sqlQuery).readEntity(StringTable.class);

        List<List<String>> expectedRows = Arrays.asList(
                Arrays.asList("2016-07-14T15:00:08.000Z", "2"),
                Arrays.asList("2016-07-14T15:00:09.000Z", "2") //<--interpolated by PREVIOUS value
        );

        assertTableRowsExist(expectedRows, resultTable);
    }


    @Issue("3066")
    @Test
    public void testPeriodExtendOptionBeginAndTrail() {
        String sqlQuery = String.format(
                "SELECT datetime, AVG(value) FROM \"%s\" %n" +
                        "WHERE entity = '%s' %nAND datetime >= '2016-07-14T15:00:05.000Z' " +
                        "AND datetime <'2016-07-14T15:00:10.000Z' %nGROUP BY PERIOD(1 SECOND,EXTEND)",
                TEST_METRIC_NAME, TEST_ENTITY1_NAME
        );

        StringTable resultTable = queryResponse(sqlQuery).readEntity(StringTable.class);

        List<List<String>> expectedRows = Arrays.asList(
                Arrays.asList("2016-07-14T15:00:05.000Z", "1"), //<--interpolated by NEXT value
                Arrays.asList("2016-07-14T15:00:06.000Z", "1"),
                // Missing period
                Arrays.asList("2016-07-14T15:00:08.000Z", "2"),
                Arrays.asList("2016-07-14T15:00:09.000Z", "2") //<--interpolated by NEXT value

        );

        assertTableRowsExist(expectedRows, resultTable);
    }


    @Issue("3066")
    @Test
    public void testPeriodExtendOptionBeginAndTrailWithValueInterpolation() {
        String sqlQuery = String.format(
                "SELECT datetime, AVG(value) FROM \"%s\" %nWHERE entity = '%s' %n" +
                        "AND datetime >= '2016-07-14T15:00:05.000Z' AND datetime <'2016-07-14T15:00:10.000Z'" +
                        "GROUP BY PERIOD(1 SECOND,EXTEND, VALUE 0)",
                TEST_METRIC_NAME, TEST_ENTITY1_NAME
        );

        StringTable resultTable = queryResponse(sqlQuery).readEntity(StringTable.class);

        List<List<String>> expectedRows = Arrays.asList(
                Arrays.asList("2016-07-14T15:00:05.000Z", "0.0"), //<--interpolated by VALUE 0
                Arrays.asList("2016-07-14T15:00:06.000Z", "1"),
                Arrays.asList("2016-07-14T15:00:07.000Z", "0.0"),//<--value interpolated
                Arrays.asList("2016-07-14T15:00:08.000Z", "2"),
                Arrays.asList("2016-07-14T15:00:09.000Z", "0.0") //<--interpolated by VALUE 0

        );

        assertTableRowsExist(expectedRows, resultTable);
    }

    @Issue("3066")
    @Test
    public void testPeriodExtendOptionBeginAndTrailWithLinearInterpolation() {
        String sqlQuery = String.format(
                "SELECT datetime, AVG(value) FROM \"%s\" %nWHERE entity = '%s' %n" +
                        "AND datetime >= '2016-07-14T15:00:05.000Z' AND datetime <'2016-07-14T15:00:10.000Z'" +
                        "GROUP BY PERIOD(1 SECOND,EXTEND, LINEAR)",
                TEST_METRIC_NAME, TEST_ENTITY1_NAME
        );

        StringTable resultTable = queryResponse(sqlQuery).readEntity(StringTable.class);

        List<List<String>> expectedRows = Arrays.asList(
                Arrays.asList("2016-07-14T15:00:05.000Z", "1"), //<--interpolated by NEXT
                Arrays.asList("2016-07-14T15:00:06.000Z", "1"),
                Arrays.asList("2016-07-14T15:00:07.000Z", "1.5"),//<--interpolated by LINEAR
                Arrays.asList("2016-07-14T15:00:08.000Z", "2"),
                Arrays.asList("2016-07-14T15:00:09.000Z", "2") //<--interpolated by PREVIOUS

        );

        assertTableRowsExist(expectedRows, resultTable);
    }

    @Issue("3066")
    @Test
    public void testPeriodExtendOptionWithIntervalLinear() {

        String sqlQuery = String.format(
                "SELECT datetime, AVG(value) FROM \"%s\" %nWHERE entity='%s' %n" +
                        "AND datetime >= '2016-07-14T15:00:06.000Z' AND datetime < '2016-07-14T15:00:09.000Z'" +
                        "GROUP BY entity, PERIOD(1 SECOND,EXTEND, LINEAR), tags.a, tags.b",
                TEST_METRIC_NAME, TEST_ENTITY1_NAME
        );

        StringTable resultTable = queryResponse(sqlQuery).readEntity(StringTable.class);

        List<List<String>> expectedRows = Arrays.asList(
                Arrays.asList("2016-07-14T15:00:06.000Z", "1"),
                Arrays.asList("2016-07-14T15:00:07.000Z", "1.5"),//<--interpolated by LINEAR
                Arrays.asList("2016-07-14T15:00:08.000Z", "2")

        );

        assertTableRowsExist(expectedRows, resultTable);
    }


    @Issue("3066")
    @Test
    public void testPeriodExtendOptionWithMultipleEntityWithoutInterval() {

        String sqlQuery = String.format(
                "SELECT entity, datetime, AVG(value) FROM \"%s\" %nWHERE tags.a LIKE 'b%%' %n" +
                        "GROUP BY entity, PERIOD(1 SECOND,EXTEND, LINEAR) %nORDER BY datetime",
                TEST_METRIC_NAME
        );


        StringTable resultTable = queryResponse(sqlQuery).readEntity(StringTable.class);

        List<List<String>> expectedRows = Arrays.asList(
                Arrays.asList(TEST_ENTITY1_NAME, "2016-07-14T15:00:06.000Z", "1"),
                Arrays.asList(TEST_ENTITY2_NAME, "2016-07-14T15:00:06.000Z", "3"),
                Arrays.asList(TEST_ENTITY1_NAME, "2016-07-14T15:00:07.000Z", "1.5"),//<--interpolated by LINEAR
                Arrays.asList(TEST_ENTITY1_NAME, "2016-07-14T15:00:08.000Z", "2")

        );

        assertTableRowsExist(expectedRows, resultTable);
    }


    @Issue("3066")
    @Test
    public void testPeriodExtendOptionWithMultipleEntityWithstartDate() {

        String sqlQuery = String.format(
                "SELECT entity, datetime, AVG(value) FROM \"%s\" %n" +
                        "WHERE tags.a LIKE 'b%%' AND datetime >= '2016-07-14T15:00:05.000Z' %n" +
                        "GROUP BY entity, PERIOD(1 SECOND,EXTEND, LINEAR) %nORDER BY datetime",
                TEST_METRIC_NAME
        );

        StringTable resultTable = queryResponse(sqlQuery).readEntity(StringTable.class);

        List<List<String>> expectedRows = Arrays.asList(
                Arrays.asList(TEST_ENTITY1_NAME, "2016-07-14T15:00:05.000Z", "1"),//<--interpolated by NEXT
                Arrays.asList(TEST_ENTITY2_NAME, "2016-07-14T15:00:05.000Z", "3"),//<--interpolated by NEXT
                Arrays.asList(TEST_ENTITY1_NAME, "2016-07-14T15:00:06.000Z", "1"),
                Arrays.asList(TEST_ENTITY2_NAME, "2016-07-14T15:00:06.000Z", "3"),
                Arrays.asList(TEST_ENTITY1_NAME, "2016-07-14T15:00:07.000Z", "1.5"),//<--interpolated by LINEAR
                Arrays.asList(TEST_ENTITY1_NAME, "2016-07-14T15:00:08.000Z", "2")

        );

        assertTableRowsExist(expectedRows, resultTable);
    }


    @Issue("3066")

    @Test
    public void testPeriodExtendOptionWithMultipleEntityWithEndDate() {

        String sqlQuery = String.format(
                "SELECT entity, datetime, AVG(value) FROM \"%s\" %n" +
                        "WHERE tags.a LIKE 'b%%' AND datetime < '2016-07-14T15:00:09.000Z' %n" +
                        "GROUP BY entity, PERIOD(1 SECOND,EXTEND, LINEAR) %nORDER BY datetime",
                TEST_METRIC_NAME
        );

        StringTable resultTable = queryResponse(sqlQuery).readEntity(StringTable.class);

        List<List<String>> expectedRows = Arrays.asList(
                Arrays.asList(TEST_ENTITY1_NAME, "2016-07-14T15:00:06.000Z", "1"),
                Arrays.asList(TEST_ENTITY2_NAME, "2016-07-14T15:00:06.000Z", "3"),
                Arrays.asList(TEST_ENTITY1_NAME, "2016-07-14T15:00:07.000Z", "1.5"),//<--interpolated by LINEAR
                Arrays.asList(TEST_ENTITY2_NAME, "2016-07-14T15:00:07.000Z", "3"),//<--interpolated by PREVIOUS
                Arrays.asList(TEST_ENTITY1_NAME, "2016-07-14T15:00:08.000Z", "2"),
                Arrays.asList(TEST_ENTITY2_NAME, "2016-07-14T15:00:08.000Z", "3")//<--interpolated by PREVIOUS

        );

        assertTableRowsExist(expectedRows, resultTable);
    }


    @Issue("3066")
    @Test
    public void testPeriodExtendOptionWithMultipleEntityWithStartAndEndDate() {

        String sqlQuery = String.format(
                "SELECT entity, datetime, AVG(value) FROM \"%s\" %n" +
                        "WHERE datetime >= '2016-07-14T15:00:05.000Z' AND datetime < '2016-07-14T15:00:09.000Z' %n" +
                        "GROUP BY entity, PERIOD(1 SECOND,EXTEND, LINEAR) %nORDER BY datetime",
                TEST_METRIC_NAME
        );

        StringTable resultTable = queryResponse(sqlQuery).readEntity(StringTable.class);

        List<List<String>> expectedRows = Arrays.asList(
                Arrays.asList(TEST_ENTITY1_NAME, "2016-07-14T15:00:05.000Z", "1"),//<--interpolated by NEXT
                Arrays.asList(TEST_ENTITY2_NAME, "2016-07-14T15:00:05.000Z", "3"),//<--interpolated by NEXT
                Arrays.asList(TEST_ENTITY1_NAME, "2016-07-14T15:00:06.000Z", "1"),
                Arrays.asList(TEST_ENTITY2_NAME, "2016-07-14T15:00:06.000Z", "3"),
                Arrays.asList(TEST_ENTITY1_NAME, "2016-07-14T15:00:07.000Z", "1.5"),//<--interpolated by LINEAR
                Arrays.asList(TEST_ENTITY2_NAME, "2016-07-14T15:00:07.000Z", "3"),//<--interpolated by PREVIOUS
                Arrays.asList(TEST_ENTITY1_NAME, "2016-07-14T15:00:08.000Z", "2"),
                Arrays.asList(TEST_ENTITY2_NAME, "2016-07-14T15:00:08.000Z", "3")//<--interpolated by PREVIOUS

        );

        assertTableRowsExist(expectedRows, resultTable);
    }
}
