package com.axibase.tsd.api.method.sql.syntax;

import com.axibase.tsd.api.method.metric.MetricMethod;
import com.axibase.tsd.api.method.series.SeriesMethod;
import com.axibase.tsd.api.method.sql.SqlTest;
import com.axibase.tsd.api.model.entity.Entity;
import com.axibase.tsd.api.model.metric.Metric;
import com.axibase.tsd.api.model.series.Sample;
import com.axibase.tsd.api.model.series.Series;
import com.axibase.tsd.api.model.sql.StringTable;
import io.qameta.allure.Issue;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.*;

public class SqlSyntaxQuotesEscapingTest extends SqlTest {
    private static final String TEST_PREFIX = "sql-syntax-quote-escape-";
    private static final String TEST_ENTITY_NAME = TEST_PREFIX + "entity";
    private static final String TEST_METRIC_NAME = TEST_PREFIX + "metric";

    @BeforeClass
    public void prepareData() throws Exception {
        Map<String, String> tags = new HashMap<>();
        tags.put("double\"quote", "tv1");
        tags.put("single'quote", "tv2");
        tags.put("both'quo\"tes", "tv3");
        Series series = new Series(TEST_ENTITY_NAME, TEST_METRIC_NAME, tags);
        series.addSamples(Sample.ofDateDecimal("2016-07-27T22:41:50.407Z", new BigDecimal("12.4")));
        SeriesMethod.insertSeriesCheck(Collections.singletonList(series));

        Metric updatedMetricQuery = new Metric();
        updatedMetricQuery.setTags(tags);
        MetricMethod.updateMetric(TEST_METRIC_NAME, updatedMetricQuery);

        Entity updatedEntityQuery = new Entity();
        updatedEntityQuery.setTags(tags);
        MetricMethod.updateMetric(TEST_METRIC_NAME, updatedEntityQuery);
    }

    @Issue("3125")
    @Test
    public void testSeriesTagsAll() {
        String sqlQuery = String.format(
                "SELECT tags.*  %nFROM \"%s\"  %nWHERE datetime > '2016-07-27T22:40:00.000Z'",
                TEST_METRIC_NAME
        );

        StringTable resultTable = queryResponse(sqlQuery)
                .readEntity(StringTable.class);

        List<String> expectedColumnNames = Arrays.asList(
                "tags.double\"quote", "tags.single'quote", "tags.both'quo\"tes"
        );

        List<List<String>> expectedRows = Collections.singletonList(
                Arrays.asList("tv3", "tv1", "tv2")
        );
        assertTableColumnsNames(expectedColumnNames, resultTable);
        assertTableRowsExist(expectedRows, resultTable);

    }


    @Issue("3125")
    @Test
    public void testMetricTagsAll() {
        String sqlQuery = String.format(
                "SELECT metric.tags.*  %nFROM \"%s\"  %nWHERE datetime > '2016-07-27T22:40:00.000Z'",
                TEST_METRIC_NAME
        );

        StringTable resultTable = queryResponse(sqlQuery)
                .readEntity(StringTable.class);

        List<String> expectedColumnNames = Arrays.asList(
                "metric.tags.double\"quote", "metric.tags.single'quote", "metric.tags.both'quo\"tes"
        );

        List<List<String>> expectedRows = Collections.singletonList(
                Arrays.asList("tv3", "tv1", "tv2")
        );
        assertTableColumnsNames(expectedColumnNames, resultTable);
        assertTableRowsExist(expectedRows, resultTable);

    }

    @Issue("3125")
    @Test
    public void testSpecifiedSeriesTags() {
        String sqlQuery = String.format(
                "SELECT tags.\"double\"\"quote\", %n" +
                        "tags.\"single'quote\", %n" +
                        "tags.\"both'quo\"\"tes\" %n" +
                        "FROM \"%s\" %nWHERE datetime > '2016-07-27T22:40:00.000Z'",
                TEST_METRIC_NAME

        );

        StringTable resultTable = queryResponse(sqlQuery)
                .readEntity(StringTable.class);

        List<String> expectedColumnNames = Arrays.asList(
                "tags.double\"quote", "tags.single'quote", "tags.both'quo\"tes"
        );

        List<List<String>> expectedRows = Collections.singletonList(
                Arrays.asList("tv1", "tv2", "tv3")
        );
        assertTableColumnsNames(expectedColumnNames, resultTable);
        assertTableRowsExist(expectedRows, resultTable);
    }


    @Issue("3125")
    @Test
    public void testSpecifiedMetricSeriesTags() {
        String sqlQuery = String.format(
                "SELECT metric.tags.\"double\"\"quote\", %n" +
                        "metric.tags.\"single'quote\", %n" +
                        "metric.tags.\"both'quo\"\"tes\"%n" +
                        "FROM \"%s\" %nWHERE datetime > '2016-07-27T22:40:00.000Z'",
                TEST_METRIC_NAME

        );

        StringTable resultTable = queryResponse(sqlQuery)
                .readEntity(StringTable.class);

        List<String> expectedColumnNames = Arrays.asList(
                "metric.tags.double\"quote", "metric.tags.single'quote", "metric.tags.both'quo\"tes"
        );

        List<List<String>> expectedRows = Collections.singletonList(
                Arrays.asList("tv1", "tv2", "tv3")
        );
        assertTableColumnsNames(expectedColumnNames, resultTable);
        assertTableRowsExist(expectedRows, resultTable);
    }


    @Issue("3125")
    @Test
    public void testLikeOperatorSeriesTags() {
        String sqlQuery = String.format(
                "SELECT tags.\"double\"\"quote\", %n" +
                        "tags.\"single'quote\", %n" +
                        "tags.\"both'quo\"\"tes\" %n" +
                        "FROM \"%s\" %nWHERE tags.\"double\"\"quote\" LIKE 'tv%%' %n" +
                        "AND tags.\"both'quo\"\"tes\" LIKE 'tv3' %nAND tags.\"single'quote\" LIKE '%%2' %n" +
                        "ORDER BY tags.\"single'quote\"",
                TEST_METRIC_NAME

        );

        StringTable resultTable = queryResponse(sqlQuery)
                .readEntity(StringTable.class);

        List<String> expectedColumnNames = Arrays.asList(
                "tags.double\"quote", "tags.single'quote", "tags.both'quo\"tes"
        );

        List<List<String>> expectedRows = Collections.singletonList(
                Arrays.asList("tv1", "tv2", "tv3")
        );
        assertTableColumnsNames(expectedColumnNames, resultTable);
        assertTableRowsExist(expectedRows, resultTable);
    }


    @Issue("3125")
    @Test
    public void testIsNullOperatorSeriesTags() {
        String sqlQuery = String.format(
                "SELECT tags.\"double\"\"quote\", %n" +
                        "tags.\"single'quote\", %n" +
                        "tags.\"both'quo\"\"tes\" %n" +
                        "FROM \"%s\" %nWHERE tags.\"double\"\"quote\" IS NOT NULL %n" +
                        "AND tags.\"both'quo\"\"tes\" IS NOT NULL %nAND tags.\"single'quote\" IS NOT NULL %n" +
                        "ORDER BY tags.\"single'quote\"",
                TEST_METRIC_NAME

        );

        StringTable resultTable = queryResponse(sqlQuery)
                .readEntity(StringTable.class);

        List<String> expectedColumnNames = Arrays.asList(
                "tags.double\"quote", "tags.single'quote", "tags.both'quo\"tes"
        );

        List<List<String>> expectedRows = Collections.singletonList(
                Arrays.asList("tv1", "tv2", "tv3")
        );
        assertTableColumnsNames(expectedColumnNames, resultTable);
        assertTableRowsExist(expectedRows, resultTable);
    }


    @Issue("3125")
    @Test
    public void testEqualsOperatorSeriesTags() {
        String sqlQuery = String.format(
                "SELECT tags.\"double\"\"quote\", %n" +
                        "tags.\"single'quote\", %n" +
                        "tags.\"both'quo\"\"tes\" %n" +
                        "FROM \"%s\" %nWHERE tags.\"double\"\"quote\" = 'tv1' %n" +
                        "AND tags.\"both'quo\"\"tes\" = 'tv3' %nAND tags.\"single'quote\" = 'tv2' %n" +
                        "ORDER BY tags.\"single'quote\"",
                TEST_METRIC_NAME

        );

        StringTable resultTable = queryResponse(sqlQuery)
                .readEntity(StringTable.class);

        List<String> expectedColumnNames = Arrays.asList(
                "tags.double\"quote", "tags.single'quote", "tags.both'quo\"tes"
        );

        List<List<String>> expectedRows = Collections.singletonList(
                Arrays.asList("tv1", "tv2", "tv3")
        );
        assertTableColumnsNames(expectedColumnNames, resultTable);
        assertTableRowsExist(expectedRows, resultTable);
    }


}
