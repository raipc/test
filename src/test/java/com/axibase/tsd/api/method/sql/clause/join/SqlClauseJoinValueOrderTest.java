package com.axibase.tsd.api.method.sql.clause.join;

import com.axibase.tsd.api.method.series.SeriesMethod;
import com.axibase.tsd.api.method.sql.SqlTest;
import com.axibase.tsd.api.model.series.Sample;
import com.axibase.tsd.api.model.series.Series;
import com.axibase.tsd.api.model.sql.StringTable;
import io.qameta.allure.Issue;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;

public class SqlClauseJoinValueOrderTest extends SqlTest {
    private static final String TEST_PREFIX = "sql-clause-join-value-order-";
    private static final String TEST_METRIC1_NAME = TEST_PREFIX + "metric-1";
    private static final String TEST_METRIC2_NAME = TEST_PREFIX + "metric-2";
    private static final String TEST_ENTITY_NAME = TEST_PREFIX + "entity";

    @BeforeClass
    public void prepareData() throws Exception {
        Series series1 = new Series(TEST_ENTITY_NAME, TEST_METRIC1_NAME,"a", "b");

        series1.addSamples(
                Sample.ofDateInteger("2016-06-03T09:20:00.000Z", 1),
                Sample.ofDateInteger("2016-06-03T09:21:00.000Z", 2),
                Sample.ofDateInteger("2016-06-03T09:22:00.000Z", 3),
                Sample.ofDateInteger("2016-06-03T09:23:00.000Z", 4)
        );

        Series series2 = new Series(TEST_ENTITY_NAME, TEST_METRIC2_NAME);
        series2.addSamples(
                Sample.ofDateInteger("2016-06-03T09:24:00.000Z", 3),
                Sample.ofDateInteger("2016-06-03T09:25:00.000Z", 4),
                Sample.ofDateInteger("2016-06-03T09:26:00.000Z", 5)
        );

        SeriesMethod.insertSeriesCheck(Arrays.asList(series1, series2));
    }

    @Issue("3322")
    @Test
    public void testOrderAsc() {
        String sqlQuery = String.format(
                "SELECT t1.value FROM \"%s\" t1%nOUTER JOIN \"%s\" t2%nORDER BY t1.value",
                TEST_METRIC1_NAME, TEST_METRIC2_NAME);

        StringTable resultTable = queryResponse(sqlQuery).readEntity(StringTable.class);

        String[][] expectedRows = {
                {"null"},
                {"null"},
                {"null"},
                {"1"},
                {"2"},
                {"3"},
                {"4"}
        };


        assertTableRowsExist(expectedRows, resultTable);
    }

    @Issue("3322")
    @Test
    public void testOrderDesc() {
        String sqlQuery = String.format(
                "SELECT t1.value FROM \"%s\" t1%nOUTER JOIN \"%s\" t2%nORDER BY t1.value DESC",
                TEST_METRIC1_NAME, TEST_METRIC2_NAME);

        StringTable resultTable = queryResponse(sqlQuery).readEntity(StringTable.class);

        String[][] expectedRows = {
                {"4"},
                {"3"},
                {"2"},
                {"1"},
                {"null"},
                {"null"},
                {"null"}
        };
        assertTableRowsExist(expectedRows, resultTable);
    }

}
