package com.axibase.tsd.api.method.sql.operator;

import com.axibase.tsd.api.method.series.SeriesMethod;
import com.axibase.tsd.api.method.sql.SqlTest;
import com.axibase.tsd.api.model.series.Sample;
import com.axibase.tsd.api.model.series.Series;
import com.axibase.tsd.api.util.Mocks;
import io.qameta.allure.Issue;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;

public class CaseOperatorTest extends SqlTest {
    private final String TEST_METRIC = Mocks.metric();

    private final String[] SERIES_VALUES = { "10", "0", "-5" };

    @BeforeClass
    public void insertDefaultSeriesData() throws Exception {
        final String ISO_MINUTES_FORMAT = "2016-06-03T09:%02d:00.000Z";
        String entity = Mocks.entity();
        Series series = new Series(entity, TEST_METRIC);

        int minutes = 0;
        for (int i = 0; i < SERIES_VALUES.length; i++) {
            String time = String.format(ISO_MINUTES_FORMAT, minutes);
            Sample sample = Sample.ofDateDecimal(time, new BigDecimal(SERIES_VALUES[i]));
            series.addSamples(sample);
            minutes +=5;
        }

        SeriesMethod.insertSeriesCheck(series);
    }

    @Issue("3981")
    @Test
    public void testSimpleCaseOperator() {
        final String sql = String.format(
                "SELECT CASE WHEN value > 0 THEN 'high' ELSE 'low' END%n" +
                "FROM \"%s\"%n" +
                "ORDER BY datetime ASC",
                TEST_METRIC
        );

        final String[][] expected = {
                { "high" }, // 10
                { "low"  },  // 0
                { "low"  }   // -5
        };

        assertSqlQueryRows("Bad CASE... result", expected, sql);
    }

    @Issue("3981")
    @Test
    public void testCaseOperator() {
        final String sql = String.format(
                "SELECT CASE WHEN value > 0 THEN 'high' ELSE 'low' END%n" +
                "FROM \"%s\"%n" +
                "ORDER BY datetime ASC",
                TEST_METRIC
        );

        final String[][] expected = {
                { "high" }, // 10
                { "low"  },  // 0
                { "low"  }   // -5
        };

        assertSqlQueryRows("Bad CASE... result", expected, sql);
    }

    @Issue("3981")
    @Test
    public void testCaseOperatorInNumericFunction() {
        final String sql = String.format(
                "SELECT SUM(CASE WHEN value > 0 THEN 100 ELSE 10 END)%n" +
                "FROM \"%s\"",
                TEST_METRIC
        );

        final String[][] expected = {
                { "120" }   // 100 * 1 + 10 * 2
        };

        assertSqlQueryRows("Bad apply SUM to CASE...", expected, sql);
    }

    @Issue("3981")
    @Test
    public void testCaseOperatorInStringFunction() {
        final String sql = String.format(
                "SELECT UPPER(CASE WHEN value > 0 THEN 'high' ELSE 'low' END)%n" +
                "FROM \"%s\"%n" +
                "ORDER by datetime ASC",
                TEST_METRIC
        );

        final String[][] expected = {
                { "HIGH" }, // 10
                { "LOW"  },  // 0
                { "LOW"  }   // -5
        };

        assertSqlQueryRows("Bad apply UPPER to CASE...", expected, sql);
    }

    @Issue("3852")
    @Test
    public void testOrderByCaseOperator() {
        final String sql = String.format(
                "SELECT value %n" +
                "FROM \"%s\" %n" +
                "ORDER BY CASE WHEN value >= 0 THEN 1 ELSE 0 END ASC",
                TEST_METRIC
        );

        final String[][] expected = {
                { "-5" },
                { "10" },
                {  "0" }
        };

        assertSqlQueryRows("Bad ascending order by CASE...", expected, sql);
    }

    @Issue("3852")
    @Test
    public void testOrderByCaseOperatorAsColumn() {
        final String sql = String.format(
                "SELECT value, CASE WHEN value >= 0 THEN 1 ELSE 0 END as \"val_group\"%n" +
                "FROM \"%s\" %n" +
                "ORDER BY \"val_group\" ASC",
                TEST_METRIC
        );

        final String[][] expected = {
                { "-5", "0" },
                { "10", "1" },
                { "0",  "1" }
        };

        assertSqlQueryRows("Bad ascending order by CASE...", expected, sql);
    }

    @Issue("3852")
    @Test
    public void testOrderByCaseOperatorOfDifferentTypes() {
        final String sql = String.format(
                "SELECT value %n" +
                "FROM \"%s\" %n" +
                "ORDER BY CASE WHEN value >= 0 THEN 'top' ELSE 10 END ASC",
                TEST_METRIC
        );

        final String[][] expected = {
                { "-5" },
                { "10" },
                {  "0" }
        };

        assertSqlQueryRows("Bad ascending order by CASE...", expected, sql);
    }

    @Issue("3852")
    @Test
    public void testCaseOperatorInWhereSwitchingCondition() {
        final String sql = String.format(
                "SELECT value %n" +
                "FROM \"%s\" %n" +
                "WHERE CASE WHEN value >= 0 THEN value > 5 ELSE value < -3 END",
                TEST_METRIC
        );

        final String[][] expected = {
                { "10" },
                { "-5" }
        };

        assertSqlQueryRows("Bad WHERE selection using boolean-type CASE...", expected, sql);
    }


    @Issue("3852")
    @Test
    public void testCaseOperatorInWhere() {
        final String sql = String.format(
                "SELECT value %n" +
                "FROM \"%s\" %n" +
                "WHERE value > CASE WHEN value > -1 THEN 5 ELSE  -8 END",
                TEST_METRIC
        );

        final String[][] expected = {
                { "10" },
                { "-5" }
        };

        assertSqlQueryRows("Bad WHERE selection using boolean-type CASE...", expected, sql);
    }
}
