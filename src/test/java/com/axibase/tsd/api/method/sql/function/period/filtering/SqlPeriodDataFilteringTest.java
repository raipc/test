package com.axibase.tsd.api.method.sql.function.period.filtering;

import com.axibase.tsd.api.method.series.SeriesMethod;
import com.axibase.tsd.api.method.sql.SqlTest;
import com.axibase.tsd.api.model.series.Sample;
import com.axibase.tsd.api.model.series.Series;
import io.qameta.allure.Issue;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static com.axibase.tsd.api.util.Mocks.entity;
import static com.axibase.tsd.api.util.Mocks.metric;


public class SqlPeriodDataFilteringTest extends SqlTest {
    private static final String TEST_METRIC_MILLISECONDS = metric();
    private static final String TEST_METRIC_SECONDS = metric();
    private static final String TEST_METRIC_MINUTES = metric();
    private static final String TEST_METRIC_HOURS = metric();
    private static final String TEST_METRIC_DAYS = metric();
    private static final String TEST_METRIC_WEEKS = metric();
    private static final String TEST_METRIC_MONTHS = metric();
    private static final String TEST_METRIC_YEARS = metric();


    @BeforeClass
    public static void prepareDataSet() throws Exception {
        Series seriesMillis = new Series(entity(), TEST_METRIC_MILLISECONDS);
        seriesMillis.addSamples(
                Sample.ofDateInteger("2017-01-01T00:00:00.000Z", 1),
                Sample.ofDateInteger("2017-01-01T00:00:00.001Z", 2),
                Sample.ofDateInteger("2017-01-01T00:00:00.002Z", 3),
                Sample.ofDateInteger("2017-01-01T00:00:00.003Z", 4),
                Sample.ofDateInteger("2017-01-01T00:00:00.004Z", 5)
        );


        Series seriesSeconds = new Series(entity(), TEST_METRIC_SECONDS);
        seriesSeconds.addSamples(
                Sample.ofDateInteger("2017-01-01T00:00:00.000Z", 1),
                Sample.ofDateInteger("2017-01-01T00:00:00.500Z", 2),
                Sample.ofDateInteger("2017-01-01T00:00:01.000Z", 3),
                Sample.ofDateInteger("2017-01-01T00:00:02.000Z", 4),
                Sample.ofDateInteger("2017-01-01T00:00:03.000Z", 5),
                Sample.ofDateInteger("2017-01-01T00:00:04.000Z", 6)
        );

        Series seriesMinutes = new Series(entity(), TEST_METRIC_MINUTES);
        seriesMinutes.addSamples(
                Sample.ofDateInteger("2017-01-01T00:00:00.000Z", 1),
                Sample.ofDateInteger("2017-01-01T00:00:30.000Z", 2),
                Sample.ofDateInteger("2017-01-01T00:01:00.000Z", 3),
                Sample.ofDateInteger("2017-01-01T00:02:00.000Z", 4),
                Sample.ofDateInteger("2017-01-01T00:03:00.000Z", 5),
                Sample.ofDateInteger("2017-01-01T00:04:00.000Z", 6)
        );

        Series seriesHours = new Series(entity(), TEST_METRIC_HOURS);
        seriesHours.addSamples(
                Sample.ofDateInteger("2017-01-01T00:00:00.000Z", 1),
                Sample.ofDateInteger("2017-01-01T00:01:00.000Z", 2),
                Sample.ofDateInteger("2017-01-01T01:00:00.000Z", 3),
                Sample.ofDateInteger("2017-01-01T02:00:00.000Z", 4),
                Sample.ofDateInteger("2017-01-01T02:01:00.000Z", 5)
        );

        Series seriesDays = new Series(entity(), TEST_METRIC_DAYS);
        seriesDays.addSamples(
                Sample.ofDateInteger("2017-01-01T00:00:00.000Z", 1),
                Sample.ofDateInteger("2017-01-01T12:00:00.000Z", 2),
                Sample.ofDateInteger("2017-01-02T00:00:00.000Z", 3),
                Sample.ofDateInteger("2017-01-03T00:00:00.000Z", 4),
                Sample.ofDateInteger("2017-01-04T00:00:00.000Z", 5)
        );

        Series seriesWeeks = new Series(entity(), TEST_METRIC_WEEKS);
        seriesWeeks.addSamples(
                Sample.ofDateInteger("2016-12-31T00:00:00.000Z", 0),
                Sample.ofDateInteger("2017-01-01T00:00:00.000Z", 1),
                Sample.ofDateInteger("2017-01-02T00:00:00.000Z", 2),
                Sample.ofDateInteger("2017-01-08T00:00:00.000Z", 3),
                Sample.ofDateInteger("2017-01-09T00:00:00.000Z", 4)
        );

        Series seriesMonths = new Series(entity(), TEST_METRIC_MONTHS);
        seriesMonths.addSamples(
                Sample.ofDateInteger("2017-01-01T00:00:00.000Z", 1),
                Sample.ofDateInteger("2017-01-15T00:00:00.000Z", 2),
                Sample.ofDateInteger("2017-02-01T00:00:00.000Z", 3),
                Sample.ofDateInteger("2017-03-01T00:00:00.000Z", 4),
                Sample.ofDateInteger("2017-04-01T00:00:00.000Z", 5)
        );

        Series seriesYears = new Series(entity(), TEST_METRIC_YEARS);
        seriesYears.addSamples(
                Sample.ofDateInteger("1970-01-01T00:00:00.000Z", 1),
                Sample.ofDateInteger("1970-05-01T00:00:00.000Z", 2),
                Sample.ofDateInteger("1970-09-01T00:00:00.000Z", 3),
                Sample.ofDateInteger("1971-01-01T00:00:00.000Z", 4),
                Sample.ofDateInteger("1972-01-01T00:00:00.000Z", 5)
        );

        SeriesMethod.insertSeriesCheck(seriesMillis, seriesSeconds, seriesMinutes,
                seriesHours, seriesDays, seriesWeeks, seriesMonths, seriesYears);
    }

    @Issue("2967")
    @Issue("4146")
    @Test
    public void testPeriodFilterMilliseconds() {
        final String sqlQuery = String.format(
                "SELECT datetime, sum(value), count(value) FROM \"%s\" " +
                        "WHERE datetime > '2017-01-01T00:00:00.000Z'" +
                        "GROUP BY PERIOD(2 MILLISECOND, 'UTC')",
                TEST_METRIC_MILLISECONDS
        );

        String[][] expectedRows = {
                {"2017-01-01T00:00:00.000Z", "2", "1"},
                {"2017-01-01T00:00:00.002Z", "7", "2"},
                {"2017-01-01T00:00:00.004Z", "5", "1"},
        };

        assertSqlQueryRows("Wrong result for millisecond period filter", expectedRows, sqlQuery);
    }

    @Issue("2967")
    @Issue("4146")
    @Test
    public void testPeriodFilterSeconds() {
        final String sqlQuery = String.format(
                "SELECT datetime, sum(value), count(value) FROM \"%s\" " +
                        "WHERE datetime > '2017-01-01T00:00:00.000Z'" +
                        "GROUP BY PERIOD(1 SECOND, 'UTC')",
                TEST_METRIC_SECONDS
        );

        String[][] expectedRows = {
                {"2017-01-01T00:00:00.000Z", "2", "1"},
                {"2017-01-01T00:00:01.000Z", "3", "1"},
                {"2017-01-01T00:00:02.000Z", "4", "1"},
                {"2017-01-01T00:00:03.000Z", "5", "1"},
                {"2017-01-01T00:00:04.000Z", "6", "1"},
        };

        assertSqlQueryRows("Wrong result for second period filter", expectedRows, sqlQuery);
    }

    @Issue("2967")
    @Issue("4146")
    @Test
    public void testPeriodFilterFewSeconds() {
        final String sqlQuery = String.format(
                "SELECT datetime, sum(value), count(value) FROM \"%s\" " +
                        "WHERE datetime > '2017-01-01T00:00:00.000Z'" +
                        "GROUP BY PERIOD(2 SECOND, 'UTC')",
                TEST_METRIC_SECONDS
        );

        String[][] expectedRows = {
                {"2017-01-01T00:00:00.000Z", "5", "2"},
                {"2017-01-01T00:00:02.000Z", "9", "2"},
                {"2017-01-01T00:00:04.000Z", "6", "1"},
        };

        assertSqlQueryRows("Wrong result for second period filter", expectedRows, sqlQuery);
    }

    @Issue("2967")
    @Issue("4146")
    @Test
    public void testPeriodFilterMinutes() {
        final String sqlQuery = String.format(
                "SELECT datetime, sum(value), count(value) FROM \"%s\" " +
                        "WHERE datetime > '2017-01-01T00:00:00.000Z'" +
                        "GROUP BY PERIOD(1 MINUTE, 'UTC')",
                TEST_METRIC_MINUTES
        );

        String[][] expectedRows = {
                {"2017-01-01T00:00:00.000Z", "2", "1"},
                {"2017-01-01T00:01:00.000Z", "3", "1"},
                {"2017-01-01T00:02:00.000Z", "4", "1"},
                {"2017-01-01T00:03:00.000Z", "5", "1"},
                {"2017-01-01T00:04:00.000Z", "6", "1"},
        };

        assertSqlQueryRows("Wrong result for second period filter", expectedRows, sqlQuery);
    }

    @Issue("2967")
    @Issue("4146")
    @Test
    public void testPeriodFilterFewMinutes() {
        final String sqlQuery = String.format(
                "SELECT datetime, sum(value), count(value) FROM \"%s\" " +
                        "WHERE datetime >= '2017-01-01T00:03:00.000Z'" +
                        "GROUP BY PERIOD(5 MINUTE, 'UTC')",
                TEST_METRIC_MINUTES
        );

        String[][] expectedRows = {
                {"2017-01-01T00:00:00.000Z", "11", "2"}
        };

        assertSqlQueryRows("Wrong result for second period filter", expectedRows, sqlQuery);
    }

    @Issue("2967")
    @Issue("4146")
    @Test
    public void testPeriodFilterDays() {
        final String sqlQuery = String.format(
                "SELECT datetime, sum(value), count(value) FROM \"%s\" " +
                        "WHERE datetime > '2017-01-01T00:00:00.000Z'" +
                        "GROUP BY PERIOD(1 DAY, 'UTC')",
                TEST_METRIC_DAYS
        );

        String[][] expectedRows = {
                {"2017-01-01T00:00:00.000Z", "2", "1"},
                {"2017-01-02T00:00:00.000Z", "3", "1"},
                {"2017-01-03T00:00:00.000Z", "4", "1"},
                {"2017-01-04T00:00:00.000Z", "5", "1"},
        };

        assertSqlQueryRows("Wrong result for second period filter", expectedRows, sqlQuery);
    }

    @Issue("2967")
    @Issue("4146")
    @Test
    public void testPeriodFilterFewDays() {
        final String sqlQuery = String.format(
                "SELECT datetime, sum(value), count(value) FROM \"%s\" " +
                        "WHERE datetime >= '2017-01-03T00:00:00.000Z'" +
                        "GROUP BY PERIOD(3 DAY, 'UTC')",
                TEST_METRIC_DAYS
        );

        String[][] expectedRows = {
                {"2017-01-01T00:00:00.000Z", "4", "1"},
                {"2017-01-04T00:00:00.000Z", "5", "1"},
        };

        assertSqlQueryRows("Wrong result for second period filter", expectedRows, sqlQuery);
    }


    @Issue("2967")
    @Issue("4146")
    @Test
    public void testPeriodFilterWeeks() {
        final String sqlQuery = String.format(
                "SELECT datetime, sum(value), count(value) FROM \"%s\" " +
                        "WHERE datetime >= '2017-01-01T00:00:00.000Z'" +
                        "GROUP BY PERIOD(1 WEEK, 'UTC')",
                TEST_METRIC_WEEKS
        );

        String[][] expectedRows = {
                {"2016-12-26T00:00:00.000Z", "1", "1"},
                {"2017-01-02T00:00:00.000Z", "5", "2"},
                {"2017-01-09T00:00:00.000Z", "4", "1"},
        };

        assertSqlQueryRows("Wrong result for second period filter", expectedRows, sqlQuery);
    }

    @Issue("2967")
    @Issue("4146")
    @Test
    public void testPeriodFilterFewWeeks() {
        final String sqlQuery = String.format(
                "SELECT datetime, sum(value), count(value) FROM \"%s\" " +
                        "WHERE datetime >= '2017-01-01T00:00:00.000Z'" +
                        "GROUP BY PERIOD(2 WEEK, 'UTC')",
                TEST_METRIC_WEEKS
        );

        String[][] expectedRows = {
                {"2016-12-26T00:00:00.000Z", "6", "3"},
                {"2017-01-09T00:00:00.000Z", "4", "1"},
        };

        assertSqlQueryRows("Wrong result for second period filter", expectedRows, sqlQuery);
    }

    @Issue("2967")
    @Issue("4146")
    @Test
    public void testPeriodFilterMonths() {
        final String sqlQuery = String.format(
                "SELECT datetime, sum(value), count(value) FROM \"%s\" " +
                        "WHERE datetime > '2017-01-01T00:00:00.000Z'" +
                        "GROUP BY PERIOD(1 MONTH, 'UTC')",
                TEST_METRIC_MONTHS
        );

        String[][] expectedRows = {
                {"2017-01-01T00:00:00.000Z", "2", "1"},
                {"2017-02-01T00:00:00.000Z", "3", "1"},
                {"2017-03-01T00:00:00.000Z", "4", "1"},
                {"2017-04-01T00:00:00.000Z", "5", "1"},
        };

        assertSqlQueryRows("Wrong result for second period filter", expectedRows, sqlQuery);
    }

    @Issue("2967")
    @Issue("4146")
    @Test
    public void testPeriodFilterFewMonths() {
        final String sqlQuery = String.format(
                "SELECT datetime, sum(value), count(value) FROM \"%s\" " +
                        "WHERE datetime > '2017-02-02T00:00:00.000Z'" +
                        "GROUP BY PERIOD(3 MONTH, 'UTC')",
                TEST_METRIC_MONTHS
        );

        String[][] expectedRows = {
                {"2017-01-01T00:00:00.000Z", "4", "1"},
                {"2017-04-01T00:00:00.000Z", "5", "1"}
        };

        assertSqlQueryRows("Wrong result for second period filter", expectedRows, sqlQuery);
    }

    @Issue("2967")
    @Issue("4146")
    @Test
    public void testPeriodFilterQuarters() {
        final String sqlQuery = String.format(
                "SELECT datetime, sum(value), count(value) FROM \"%s\" " +
                        "WHERE datetime > '2017-01-01T00:00:00.000Z'" +
                        "GROUP BY PERIOD(1 QUARTER, 'UTC')",
                TEST_METRIC_MONTHS
        );

        String[][] expectedRows = {
                {"2017-01-01T00:00:00.000Z", "9", "3"},
                {"2017-04-01T00:00:00.000Z", "5", "1"}
        };

        assertSqlQueryRows("Wrong result for second period filter", expectedRows, sqlQuery);
    }

    @Issue("2967")
    @Issue("4146")
    @Test
    public void testPeriodFilterYears() {
        final String sqlQuery = String.format(
                "SELECT datetime, sum(value), count(value) FROM \"%s\" " +
                        "WHERE datetime > '1970-01-01T00:00:00.000Z'" +
                        "GROUP BY PERIOD(1 YEAR, 'UTC')",
                TEST_METRIC_YEARS
        );

        String[][] expectedRows = {
                {"1970-01-01T00:00:00.000Z", "5", "2"},
                {"1971-01-01T00:00:00.000Z", "4", "1"},
                {"1972-01-01T00:00:00.000Z", "5", "1"}
        };

        assertSqlQueryRows("Wrong result for second period filter", expectedRows, sqlQuery);
    }

    @Issue("2967")
    @Issue("4146")
    @Test
    public void testPeriodFilterLeftBound() {
        final String sqlQuery = String.format(
                "SELECT datetime, sum(value), count(value) FROM \"%s\" " +
                        "WHERE datetime > '2017-01-01T00:00:00.000Z'" +
                        "GROUP BY PERIOD(1 HOUR, 'UTC')",
                TEST_METRIC_HOURS
        );

        String[][] expectedRows = {
                {"2017-01-01T00:00:00.000Z", "2", "1"},
                {"2017-01-01T01:00:00.000Z", "3", "1"},
                {"2017-01-01T02:00:00.000Z", "9", "2"},
        };

        assertSqlQueryRows("Wrong result if period is not in range (left bound)", expectedRows, sqlQuery);
    }

    @Issue("2967")
    @Issue("4146")
    @Test
    public void testPeriodFilterRightBound() {
        final String sqlQuery = String.format(
                "SELECT datetime, sum(value), count(value) FROM \"%s\" " +
                        "WHERE datetime <= '2017-01-01T02:00:00.000Z'" +
                        "GROUP BY PERIOD(1 HOUR, 'UTC')",
                TEST_METRIC_HOURS
        );

        String[][] expectedRows = {
                {"2017-01-01T00:00:00.000Z", "3", "2"},
                {"2017-01-01T01:00:00.000Z", "3", "1"},
                {"2017-01-01T02:00:00.000Z", "4", "1"},
        };

        assertSqlQueryRows("Wrong result if period is not in range (right bound)", expectedRows, sqlQuery);
    }

    @Issue("2967")
    @Issue("4146")
    @Test
    public void testPeriodFilterBothBounds() {
        final String sqlQuery = String.format(
                "SELECT datetime, sum(value), count(value) FROM \"%s\" " +
                        "WHERE datetime > '2017-01-01T00:00:00.000Z' AND datetime <= '2017-01-01T02:00:00.000Z' " +
                        "GROUP BY PERIOD(1 HOUR, 'UTC')",
                TEST_METRIC_HOURS
        );

        String[][] expectedRows = {
                {"2017-01-01T00:00:00.000Z", "2", "1"},
                {"2017-01-01T01:00:00.000Z", "3", "1"},
                {"2017-01-01T02:00:00.000Z", "4", "1"},
        };
        assertSqlQueryRows("Wrong result if period is not in range (both bounds)", expectedRows, sqlQuery);
    }

    @Issue("2967")
    @Issue("4146")
    @Test
    public void testPeriodNonFilterBothBounds() {
        final String sqlQuery = String.format(
                "SELECT datetime, sum(value), count(value) FROM \"%s\" " +
                        "WHERE datetime >= '2017-01-01T00:00:00.000Z' AND datetime <= '2017-01-01T02:01:00.000Z' " +
                        "GROUP BY PERIOD(1 HOUR, 'UTC')",
                TEST_METRIC_HOURS
        );

        String[][] expectedRows = {
                {"2017-01-01T00:00:00.000Z", "3", "2"},
                {"2017-01-01T01:00:00.000Z", "3", "1"},
                {"2017-01-01T02:00:00.000Z", "9", "2"},
        };
        assertSqlQueryRows("Wrong result if all periods are in range", expectedRows, sqlQuery);
    }
}
