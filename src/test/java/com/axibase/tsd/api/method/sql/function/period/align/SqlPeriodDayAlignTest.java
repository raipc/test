package com.axibase.tsd.api.method.sql.function.period.align;

import com.axibase.tsd.api.method.series.SeriesMethod;
import com.axibase.tsd.api.method.sql.SqlTest;
import com.axibase.tsd.api.model.series.Sample;
import com.axibase.tsd.api.model.series.Series;
import com.axibase.tsd.api.util.TestUtil;
import com.axibase.tsd.api.util.TestUtil.TimeTranslation;
import com.axibase.tsd.api.util.Util;
import io.qameta.allure.Issue;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.time.ZoneOffset;
import java.util.*;

import static com.axibase.tsd.api.util.Mocks.entity;
import static com.axibase.tsd.api.util.Mocks.metric;

public class SqlPeriodDayAlignTest extends SqlTest {
    private static final String TEST_METRIC_NAME = metric();
    private static final String DAY_FORMAT_PATTERN = "yyyy-MM-dd";
    private static final String START_TIME = "2016-06-19T00:00:00.000Z";
    private static final String END_TIME = "2016-06-23T00:00:00.000Z";
    private static final long DELTA = 900000L;

    @BeforeClass
    public static void prepareData() throws Exception {
        Series series = new Series(entity(), TEST_METRIC_NAME);

        long firstTime = Util.parseDate(START_TIME).getTime();
        long lastTime = Util.parseDate(END_TIME).getTime();
        for (long time = firstTime; time < lastTime; time += DELTA) {
            series.addSamples(Sample.ofDateInteger(Util.ISOFormat(time), 0));
        }

        SeriesMethod.insertSeriesCheck(Collections.singletonList(series));
    }

    @Issue("3241")
    @Test
    public void testDayAlign() {
        String sqlQuery = String.format(
                "SELECT DATE_FORMAT(time,'%s'), COUNT(*) " +
                        "FROM \"%s\" " +
                        "GROUP BY PERIOD(1 DAY)",
                DAY_FORMAT_PATTERN, TEST_METRIC_NAME
        );

        assertSqlQueryRows("Wrong result when grouping by 1 day (default timezone)",
                generateExpectedRows(null), sqlQuery);
    }

    @Issue("4100")
    @Test
    public void testDayAlignWithTimezone() {
        TimeZone timeZone = TimeZone.getTimeZone("Asia/Kathmandu");
        String sqlQuery = String.format(
                "SELECT DATE_FORMAT(time,'%1$s', '%3$s'), COUNT(*) " +
                        "FROM \"%2$s\" " +
                        "GROUP BY PERIOD(1 DAY, '%3$s')",
                DAY_FORMAT_PATTERN,
                TEST_METRIC_NAME,
                timeZone.getID()
        );

        assertSqlQueryRows("Wrong result when grouping by 1 day (custom timezone)",
                generateExpectedRows(timeZone), sqlQuery);
    }


    private List<List<String>> generateExpectedRows(TimeZone timeZone) {
        List<List<String>> resultRows = new ArrayList<>();
        final String localStartDate, localEndDate;

        if (timeZone == null) {
            localStartDate = TestUtil.timeTranslateDefault(START_TIME, TimeTranslation.UNIVERSAL_TO_LOCAL);
            localEndDate = TestUtil.timeTranslateDefault(END_TIME, TimeTranslation.UNIVERSAL_TO_LOCAL);
        } else {
            localStartDate = TestUtil.timeTranslate(START_TIME, timeZone, TimeTranslation.UNIVERSAL_TO_LOCAL);
            localEndDate = TestUtil.timeTranslate(END_TIME, timeZone, TimeTranslation.UNIVERSAL_TO_LOCAL);
        }

        long startTime = Util.parseDate(localStartDate).getTime();
        long endTime = Util.parseDate(localEndDate).getTime();
        long time;

        int daySeriesCount = 0;
        for (time = startTime; time < endTime; time += DELTA) {
            if (isDayStart(time) && daySeriesCount > 0) {
                resultRows.add(formatRow(time - TestUtil.MILLIS_IN_DAY, daySeriesCount));
                daySeriesCount = 0;
            }
            daySeriesCount++;
        }

        if (daySeriesCount > 0) {
            resultRows.add(formatRow(time - DELTA, daySeriesCount));
        }

        return resultRows;
    }

    private List<String> formatRow(long time, int count) {
        return Arrays.asList(TestUtil.formatDate(time, DAY_FORMAT_PATTERN, ZoneOffset.UTC), Integer.toString(count));
    }

    private boolean isDayStart(long time) {
        return time % TestUtil.MILLIS_IN_DAY == 0;
    }

}
