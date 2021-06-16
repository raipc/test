package com.axibase.tsd.api.method.sql.operator;

import com.axibase.tsd.api.method.series.SeriesMethod;
import com.axibase.tsd.api.method.sql.SqlTest;
import com.axibase.tsd.api.model.series.Sample;
import com.axibase.tsd.api.model.series.Series;
import io.qameta.allure.Issue;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static com.axibase.tsd.api.util.Mocks.entity;
import static com.axibase.tsd.api.util.Mocks.metric;

public class SqlBetweenStringTest extends SqlTest {
    private static String METRIC_NAME = metric();

    @BeforeClass
    public static void prepareData() throws Exception {
        Series series = new Series(entity(), METRIC_NAME);

        series.addSamples(
                Sample.ofDateIntegerText("2017-01-01T12:00:00.000Z", 1, "b"),
                Sample.ofDateIntegerText("2017-01-02T12:00:00.000Z", 2, "c"),
                Sample.ofDateIntegerText("2017-01-03T12:00:00.000Z", 3, "a"),
                Sample.ofDateIntegerText("2017-01-04T12:00:00.000Z", 4, "d"),
                Sample.ofDateIntegerText("2017-01-05T12:00:00.000Z", 5, "null"),
                Sample.ofDateIntegerText("2017-01-06T12:00:00.000Z", 6, "z"),
                Sample.ofDateIntegerText("2017-01-07T12:00:00.000Z", 7, "x"),
                Sample.ofDateIntegerText("2017-01-08T12:00:00.000Z", 8, "az"),
                Sample.ofDateIntegerText("2017-01-09T12:00:00.000Z", 9, null),
                Sample.ofDateIntegerText("2017-01-10T12:00:00.000Z", 10, "a ")
        );

        SeriesMethod.insertSeriesCheck(series);
    }

    @Issue("3991")
    @Test
    public void testBetweenEqualStringLimits() {
        String sqlQuery = String.format(
                "SELECT value, text FROM \"%s\" WHERE text BETWEEN 'a' AND 'a'",
                METRIC_NAME
        );

        String[][] expectedRows = {{"3", "a"}};

        assertSqlQueryRows("Wrong result with equal string limits in BETWEEN operator", expectedRows, sqlQuery);
    }

    @Issue("3991")
    @Test
    public void testBetweenAllString() {
        String sqlQuery = String.format(
                "SELECT value, text FROM \"%s\" WHERE text BETWEEN 'a' AND 'z'",
                METRIC_NAME
        );

        String[][] expectedRows = {
                {"1", "b"},
                {"2", "c"},
                {"3", "a"},
                {"4", "d"},
                {"5", "null"},
                {"6", "z"},
                {"7", "x"},
                {"8", "az"},
                {"10", "a "},
        };

        assertSqlQueryRows("Wrong result with string limits in BETWEEN operator", expectedRows, sqlQuery);
    }

    @Issue("3991")
    @Test
    public void testBetweenSimpleString() {
        String sqlQuery = String.format(
                "SELECT value, text FROM \"%s\" WHERE text BETWEEN 'a' AND 'b'",
                METRIC_NAME
        );

        String[][] expectedRows = {
                {"1", "b"},
                {"3", "a"},
                {"8", "az"},
                {"10", "a "},
        };

        assertSqlQueryRows("Wrong result with string limits in BETWEEN operator", expectedRows, sqlQuery);
    }

    @Issue("3991")
    @Test
    public void testBetweenNullString() {
        String sqlQuery = String.format(
                "SELECT value, text FROM \"%s\" WHERE text BETWEEN 'null' AND 'z'",
                METRIC_NAME
        );

        String[][] expectedRows = {
                {"5", "null"},
                {"6", "z"},
                {"7", "x"},
        };

        assertSqlQueryRows("Wrong result with \"null\" string limit in BETWEEN operator", expectedRows, sqlQuery);
    }
}
