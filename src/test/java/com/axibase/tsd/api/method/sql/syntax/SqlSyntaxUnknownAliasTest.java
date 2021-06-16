package com.axibase.tsd.api.method.sql.syntax;

import com.axibase.tsd.api.method.series.SeriesMethod;
import com.axibase.tsd.api.method.sql.SqlTest;
import com.axibase.tsd.api.model.series.Sample;
import com.axibase.tsd.api.model.series.Series;
import com.axibase.tsd.api.util.ErrorTemplate;
import io.qameta.allure.Issue;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.util.Arrays;


public class SqlSyntaxUnknownAliasTest extends SqlTest {
    private static final String TEST_PREFIX = "sql-syntax-unknown-alias-";
    private static final String TEST_METRIC1_NAME = TEST_PREFIX + "metric-1";
    private static final String TEST_METRIC2_NAME = TEST_PREFIX + "metric-2";
    private static final String TEST_ENTITY_NAME = TEST_PREFIX + "entity";


    @BeforeClass
    public static void prepareData() throws Exception {
        Series series1 = new Series(TEST_ENTITY_NAME, TEST_METRIC1_NAME);
        series1.addSamples(Sample.ofDateInteger("2016-06-03T09:24:00.000Z", 0));

        Series series2 = new Series(TEST_ENTITY_NAME, TEST_METRIC2_NAME);
        series2.addSamples(Sample.ofDateInteger("2016-06-03T09:24:00.000Z", 0));

        SeriesMethod.insertSeriesCheck(Arrays.asList(series1, series2));
    }

    @Issue("3234")
    @Test
    public void testUnknownAliasInSelectWithoutJoin() {
        String sqlQuery = String.format(
                "SELECT t1.value FROM \"%s\"",
                TEST_METRIC1_NAME
        );

        Response response = queryResponse(sqlQuery);

        String expectedErrorMessage = ErrorTemplate.Sql.invalidIdentifier("t1");
        assertBadRequest("Alias shouldn't be recognized",
                expectedErrorMessage, response
        );
    }


    @Issue("3234")
    @Test
    public void testUnknownAliasInWhereWithoutJoin() {
        String sqlQuery = String.format(
                "SELECT value FROM \"%s\"WHERE t1.value > 0",
                TEST_METRIC1_NAME
        );

        Response response = queryResponse(sqlQuery);

        String expectedErrorMessage = ErrorTemplate.Sql.invalidIdentifier("t1");
        assertBadRequest("Alias shouldn't be recognized",
                expectedErrorMessage, response
        );
    }


    @Issue("3234")
    @Test
    public void testUnknownAliasInOrderByWithoutJoin() {
        String sqlQuery = String.format(
                "SELECT value FROM \"%s\"WHERE value > 0 ORDER BY t1.value",
                TEST_METRIC1_NAME
        );

        Response response = queryResponse(sqlQuery);

        String expectedErrorMessage = ErrorTemplate.Sql.invalidIdentifier("t1");
        assertBadRequest("Alias shouldn't be recognized",
                expectedErrorMessage, response
        );
    }


    @Issue("3234")
    @Test
    public void testUnknownAliasInGroupByWithoutJoin() {
        String sqlQuery = String.format(
                "SELECT COUNT(value) FROM \"%s\"WHERE value > 0 GROUP BY t1.entity",
                TEST_METRIC1_NAME
        );

        Response response = queryResponse(sqlQuery);

        String expectedErrorMessage = ErrorTemplate.Sql.invalidIdentifier("t1");
        assertBadRequest("Alias shouldn't be recognized",
                expectedErrorMessage, response
        );
    }

    @Issue("3234")
    @Test
    public void testUnknownAliasInSelectWithJoin() {
        String sqlQuery = String.format(
                "SELECT t2.value FROM \"%s\" t1%nJOIN \"%s\" WHERE t1.value > 0%n",
                TEST_METRIC1_NAME, TEST_METRIC2_NAME
        );

        Response response = queryResponse(sqlQuery);

        String expectedErrorMessage = ErrorTemplate.Sql.invalidIdentifier("t2");
        assertBadRequest("Alias shouldn't be recognized",
                expectedErrorMessage, response
        );
    }

    @Issue("3234")
    @Test
    public void testUnknownAliasInWhereWithJoin() {
        String sqlQuery = String.format(
                "SELECT t1.value FROM \"%s\" t1%nJOIN \"%s\" WHERE t2.value > 0",
                TEST_METRIC1_NAME, TEST_METRIC2_NAME
        );

        Response response = queryResponse(sqlQuery);

        String expectedErrorMessage = ErrorTemplate.Sql.invalidIdentifier("t2");
        assertBadRequest("Alias shouldn't be recognized",
                expectedErrorMessage, response
        );
    }

    @Issue("3234")
    @Test
    public void testUnknownAliasInOrderByWithJoin() {
        String sqlQuery = String.format(
                "SELECT t1.value FROM \"%s\" t1%n JOIN \"%s\"WHERE t1.value > 0 ORDER BY t2.value",
                TEST_METRIC1_NAME, TEST_METRIC2_NAME
        );

        Response response = queryResponse(sqlQuery);

        String expectedErrorMessage = ErrorTemplate.Sql.invalidIdentifier("t2");
        assertBadRequest("Alias shouldn't be recognized",
                expectedErrorMessage, response
        );
    }


    @Issue("3234")
    @Test
    public void testUnknownAliasInGroupByWithJoin() {
        String sqlQuery = String.format(
                "SELECT COUNT(t1.value) FROM \"%s\" t1%nJOIN \"%s\"%nWHERE t1.value > 0 GROUP BY t2.entity",
                TEST_METRIC1_NAME, TEST_METRIC2_NAME
        );

        Response response = queryResponse(sqlQuery);

        String expectedErrorMessage = ErrorTemplate.Sql.invalidIdentifier("t2");
        assertBadRequest("Alias shouldn't be recognized",
                expectedErrorMessage, response
        );
    }
}
