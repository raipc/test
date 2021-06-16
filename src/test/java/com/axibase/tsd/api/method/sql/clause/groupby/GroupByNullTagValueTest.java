package com.axibase.tsd.api.method.sql.clause.groupby;

import com.axibase.tsd.api.method.series.SeriesMethod;
import com.axibase.tsd.api.method.sql.SqlTest;
import com.axibase.tsd.api.model.series.Sample;
import com.axibase.tsd.api.model.series.Series;
import io.qameta.allure.Issue;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static com.axibase.tsd.api.util.Mocks.*;

public class GroupByNullTagValueTest extends SqlTest {
    private static final String TEST_ENTITY_NAME = entity();
    private static final String TEST_METRIC_NAME = metric();

    @BeforeClass
    public void prepareData() throws Exception {
        Series series = new Series(TEST_ENTITY_NAME, TEST_METRIC_NAME, "tag1", "tagname");
        series.addSamples(
                Sample.ofDateDecimal("2017-02-09T12:00:00.000Z", DECIMAL_VALUE),
                Sample.ofDateDecimal("2017-02-10T12:00:00.000Z", DECIMAL_VALUE)
        );

        Series seriesWithoutTag = new Series(TEST_ENTITY_NAME, TEST_METRIC_NAME);
        seriesWithoutTag.addSamples(
                Sample.ofDateDecimal("2017-02-11T12:00:00.000Z", DECIMAL_VALUE),
                Sample.ofDateDecimal("2017-02-12T12:00:00.000Z", DECIMAL_VALUE)
        );

        SeriesMethod.insertSeriesCheck(series, seriesWithoutTag);
    }

    @Issue("4028")
    @Test
    public void testGroupingByTagnameThatHasNullValues() {
        String sqlQuery = String.format(
                        "SELECT tags.tag1, avg(value) " +
                        "FROM \"%s\" " +
                        "GROUP BY tags.tag1",
                TEST_METRIC_NAME
        );

        String[][] expectedRows = {
                {"null", DECIMAL_VALUE.toString()},
                {"tagname", DECIMAL_VALUE.toString()}
        };

        assertSqlQueryRows("GROUP BY tag name that has null values gives wrong result", expectedRows, sqlQuery);
    }
}
