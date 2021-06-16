package com.axibase.tsd.api.method.series;

import com.axibase.tsd.api.model.Period;
import com.axibase.tsd.api.model.TimeUnit;
import com.axibase.tsd.api.model.series.Sample;
import com.axibase.tsd.api.model.series.Series;
import com.axibase.tsd.api.model.series.query.SeriesQuery;
import com.axibase.tsd.api.model.series.query.transformation.aggregate.Aggregate;
import com.axibase.tsd.api.model.series.query.transformation.aggregate.AggregationType;
import com.axibase.tsd.api.model.series.query.transformation.group.Group;
import com.axibase.tsd.api.model.series.query.transformation.group.GroupType;
import com.axibase.tsd.api.model.series.query.transformation.rate.Rate;
import com.axibase.tsd.api.util.Mocks;
import com.axibase.tsd.api.util.TestUtil;
import io.qameta.allure.Issue;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.json.JSONException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.*;

import static org.testng.Assert.assertEquals;

public class SeriesQueryAggregateGroupOrderRateTest extends SeriesMethod {
    private String TEST_ENTITY1;
    private String TEST_ENTITY2;
    private String TEST_METRIC;

    @Data
    @RequiredArgsConstructor
    private class CheckedFields {
        private final String entity;
        private final List<Sample> data;
        private final String metric = TEST_METRIC;

        @Override
        public String toString() {
            return ReflectionToStringBuilder.toString(this, ToStringStyle.NO_CLASS_NAME_STYLE);
        }
    }

    @BeforeClass
    public void prepareData() throws Exception {
        TEST_ENTITY1 = Mocks.entity();
        TEST_ENTITY2 = Mocks.entity();

        TEST_METRIC = Mocks.metric();

        Series series1 = new Series(TEST_ENTITY1, TEST_METRIC);
        Series series2 = new Series(TEST_ENTITY2, TEST_METRIC);
        for (int i = 0; i < 20; i++) {
            Series series = i % 2 != 0 ? series1 : series2;
            int value = i % 2 != 0 ? 100 + i : 200 + i;
            series.addSamples(Sample.ofDateInteger(
                    TestUtil.addTimeUnitsInTimezone(
                            "2017-01-01T00:00:00Z",
                            ZoneId.of("Etc/UTC"),
                            TimeUnit.SECOND,
                            i),
                    value)
            );
        }

        insertSeriesCheck(series1, series2);
    }

    @DataProvider
    public Object[][] provideOrders() {
        return new Object[][] {
                {null},
                {0},
                {5},
                {-5}
        };
    }

    @Issue("4729")
    @Test(
            dataProvider = "provideOrders",
            description = "test query result with aggregate")
    public void testAggregateOrder(Integer order) {
        SeriesQuery query = new SeriesQuery(
                "*",
                TEST_METRIC,
                "2017-01-01T00:00:00Z",
                "2017-01-01T00:00:20Z");
        Aggregate aggregate = new Aggregate(
                AggregationType.MIN,
                new Period(10, TimeUnit.SECOND));
        if (order != null) {
            aggregate.setOrder(order);
        }
        query.setAggregate(aggregate);

        Set<CheckedFields> actualFields = createCheckFields(querySeriesAsList(query));

        CheckedFields series1 = new CheckedFields(TEST_ENTITY1, Arrays.asList(
                Sample.ofDateDecimal("2017-01-01T00:00:00.000Z", new BigDecimal("101.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:10.000Z", new BigDecimal("111.0"))));

        CheckedFields series2 = new CheckedFields(TEST_ENTITY2, Arrays.asList(
                Sample.ofDateDecimal("2017-01-01T00:00:00.000Z", new BigDecimal("200.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:10.000Z", new BigDecimal("210.0"))));
        Set<CheckedFields> expectedFields = collectCheckFields(series1, series2);


        assertEquals(
                actualFields,
                expectedFields,
                "Incorrect query result with aggregate");
    }

    @Issue("4729")
    @Test(
            dataProvider = "provideOrders",
            description = "test query result with group")
    public void testGroupOrder(Integer order) {
        SeriesQuery query = new SeriesQuery(
                "*",
                TEST_METRIC,
                "2017-01-01T00:00:00Z",
                "2017-01-01T00:00:20Z");
        Group group = new Group(
                GroupType.MAX,
                new Period(5, TimeUnit.SECOND));
        if (order != null) {
            group.setOrder(order);
        }
        query.setGroup(group);

        Set<CheckedFields> actualFields = createCheckFields(querySeriesAsList(query));

        CheckedFields series = new CheckedFields("*", Arrays.asList(
                Sample.ofDateDecimal("2017-01-01T00:00:00.000Z", new BigDecimal("204.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:05.000Z", new BigDecimal("208.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:10.000Z", new BigDecimal("214.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:15.000Z", new BigDecimal("218.0"))));
        Set<CheckedFields> expectedFields = collectCheckFields(series);

        assertEquals(
                actualFields,
                expectedFields,
                "Incorrect query result with group");
    }

    @Issue("4729")
    @Test(
            dataProvider = "provideOrders",
            description = "test query result with rate")
    public void testRateOrder(Integer order) {
        SeriesQuery query = new SeriesQuery(
                "*",
                TEST_METRIC,
                "2017-01-01T00:00:00Z",
                "2017-01-01T00:00:20Z");
        Rate rate = new Rate(new Period(5, TimeUnit.SECOND));
        if (order != null) {
            rate.setOrder(order);
        }
        query.setRate(rate);

        Set<CheckedFields> actualFields = createCheckFields(querySeriesAsList(query));

        CheckedFields series1 = new CheckedFields(TEST_ENTITY1, Arrays.asList(
                Sample.ofDateDecimal("2017-01-01T00:00:03.000Z", new BigDecimal("5.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:05.000Z", new BigDecimal("5.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:07.000Z", new BigDecimal("5.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:09.000Z", new BigDecimal("5.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:11.000Z", new BigDecimal("5.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:13.000Z", new BigDecimal("5.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:15.000Z", new BigDecimal("5.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:17.000Z", new BigDecimal("5.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:19.000Z", new BigDecimal("5.0"))));

        CheckedFields series2 = new CheckedFields(TEST_ENTITY2, Arrays.asList(
                Sample.ofDateDecimal("2017-01-01T00:00:02.000Z", new BigDecimal("5.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:04.000Z", new BigDecimal("5.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:06.000Z", new BigDecimal("5.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:08.000Z", new BigDecimal("5.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:10.000Z", new BigDecimal("5.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:12.000Z", new BigDecimal("5.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:14.000Z", new BigDecimal("5.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:16.000Z", new BigDecimal("5.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:18.000Z", new BigDecimal("5.0"))));
        Set<CheckedFields> expectedFields = collectCheckFields(series1, series2);

        assertEquals(
                actualFields,
                expectedFields,
                "Incorrect query result with rate");
    }

    @Issue("4729")
    @Test(description = "test query result with default Group/Aggregate order")
    public void testDefaultOrderGroupAggregate() {
        SeriesQuery query = new SeriesQuery(
                "*",
                TEST_METRIC,
                "2017-01-01T00:00:00Z",
                "2017-01-01T00:00:20Z");
        query.setAggregate(new Aggregate(
                AggregationType.COUNT,
                new Period(10, TimeUnit.SECOND)
        ));
        query.setGroup(new Group(
                GroupType.MAX,
                new Period(5, TimeUnit.SECOND)
        ));

        Set<CheckedFields> actualFields = createCheckFields(querySeriesAsList(query));

        CheckedFields series = new CheckedFields("*", Arrays.asList(
                Sample.ofDateDecimal("2017-01-01T00:00:00.000Z", new BigDecimal("2.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:10.000Z", new BigDecimal("2.0"))));
        Set<CheckedFields> expectedFields = collectCheckFields(series);

        assertEquals(
                actualFields,
                expectedFields,
                "Incorrect query result with default Group/Aggregate order");
    }

    @Issue("4729")
    @Test(description = "test query result with default Group/Rate order")
    public void testDefaultOrderGroupRate() {
        SeriesQuery query = new SeriesQuery(
                "*",
                TEST_METRIC,
                "2017-01-01T00:00:00Z",
                "2017-01-01T00:00:20Z");
        query.setGroup(new Group(
                GroupType.MAX,
                new Period(5, TimeUnit.SECOND)
        ));
        query.setRate(new Rate(
                new Period(10, TimeUnit.SECOND)
        ));

        Set<CheckedFields> actualFields = createCheckFields(querySeriesAsList(query));

        CheckedFields series = new CheckedFields("*", Arrays.asList(
                Sample.ofDateDecimal("2017-01-01T00:00:05.000Z", new BigDecimal("8.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:10.000Z", new BigDecimal("12.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:15.000Z", new BigDecimal("8.0"))));
        Set<CheckedFields> expectedFields = collectCheckFields(series);

        assertEquals(
                actualFields,
                expectedFields,
                "Incorrect query result with default Group/Rate order");
    }

    @Issue("4729")
    @Test(description = "test query result with default Group/Aggregate order")
    public void testDefaultOrderRateAggregate() throws JSONException {
        SeriesQuery query = new SeriesQuery(
                "*",
                TEST_METRIC,
                "2017-01-01T00:00:00Z",
                "2017-01-01T00:00:20Z");
        query.setRate(new Rate(
                new Period(5, TimeUnit.SECOND)
        ));
        query.setAggregate(new Aggregate(
                AggregationType.COUNT,
                new Period(10, TimeUnit.SECOND)
        ));

        Set<CheckedFields> actualFields = createCheckFields(querySeriesAsList(query));

        CheckedFields series1 = new CheckedFields(TEST_ENTITY1, Arrays.asList(
                Sample.ofDateDecimal("2017-01-01T00:00:00.000Z", new BigDecimal("4.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:10.000Z", new BigDecimal("5.0"))));

        CheckedFields series2 = new CheckedFields(TEST_ENTITY2, Arrays.asList(
                Sample.ofDateDecimal("2017-01-01T00:00:00.000Z", new BigDecimal("4.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:10.000Z", new BigDecimal("5.0"))));
        Set<CheckedFields> expectedFields = collectCheckFields(series1, series2);


        assertEquals(
                actualFields,
                expectedFields,
                "Incorrect query result with default Rate/Aggregate order");
    }

    @Issue("4729")
    @Test(description = "test query result with explicit equals Group/Rate/Aggregate order")
    public void testExplicitEqualsOrderGroupRateAggregate() {
        SeriesQuery query = new SeriesQuery(
                "*",
                TEST_METRIC,
                "2017-01-01T00:00:00Z",
                "2017-01-01T00:00:20Z");
        query.setGroup(new Group(
                GroupType.MAX,
                new Period(5, TimeUnit.SECOND),
                0
        ));
        query.setRate(new Rate(
                new Period(2, TimeUnit.SECOND),
                0
        ));
        query.setAggregate(new Aggregate(
                AggregationType.SUM,
                new Period(10, TimeUnit.SECOND),
                0
        ));

        Set<CheckedFields> actualFields = createCheckFields(querySeriesAsList(query));

        CheckedFields series = new CheckedFields("*", Arrays.asList(
                Sample.ofDateDecimal("2017-01-01T00:00:00.000Z", new BigDecimal("1.6")),
                Sample.ofDateDecimal("2017-01-01T00:00:10.000Z", new BigDecimal("4.0"))));
        Set<CheckedFields> expectedFields = collectCheckFields(series);

        assertEquals(
                actualFields,
                expectedFields,
                "Incorrect query result with explicit equals Group/Aggregate order");
    }

    @Issue("4729")
    @Test(description = "test query result with explicit non-equals Group/Aggregate order")
    public void testExplicitOrderGroupRateAggregate() {
        SeriesQuery query = new SeriesQuery(
                "*",
                TEST_METRIC,
                "2017-01-01T00:00:00Z",
                "2017-01-01T00:00:20Z");
        query.setAggregate(new Aggregate(
                AggregationType.SUM,
                new Period(4, TimeUnit.SECOND),
                3
        ));
        query.setGroup(new Group(
                GroupType.MIN,
                new Period(5, TimeUnit.SECOND),
                4
        ));
        query.setRate(new Rate(
                new Period(10, TimeUnit.SECOND),
                5
        ));

        Set<CheckedFields> actualFields = createCheckFields(querySeriesAsList(query));

        CheckedFields series = new CheckedFields("*", Arrays.asList(
                Sample.ofDateDecimal("2017-01-01T00:00:05.000Z", new BigDecimal("32.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:10.000Z", new BigDecimal("16.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:15.000Z", new BigDecimal("16.0"))));
        Set<CheckedFields> expectedFields = collectCheckFields(series);

        assertEquals(
                actualFields,
                expectedFields,
                "Incorrect query result with explicit non-equals Group/Rate/Aggregate order");
    }

    @Issue("4729")
    @Test(description = "test query result with explicit negative Group/Aggregate order")
    public void testExplicitNegativeOrderGroupRateAggregate() {
        SeriesQuery query = new SeriesQuery(
                "*",
                TEST_METRIC,
                "2017-01-01T00:00:00Z",
                "2017-01-01T00:00:20Z");
        query.setAggregate(new Aggregate(
                AggregationType.SUM,
                new Period(4, TimeUnit.SECOND),
                -5
        ));
        query.setGroup(new Group(
                GroupType.MIN,
                new Period(5, TimeUnit.SECOND),
                -4
        ));
        query.setRate(new Rate(
                new Period(10, TimeUnit.SECOND),
                -3
        ));

        Set<CheckedFields> actualFields = createCheckFields(querySeriesAsList(query));

        CheckedFields series = new CheckedFields("*", Arrays.asList(
                Sample.ofDateDecimal("2017-01-01T00:00:05.000Z", new BigDecimal("32.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:10.000Z", new BigDecimal("16.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:15.000Z", new BigDecimal("16.0"))));
        Set<CheckedFields> expectedFields = collectCheckFields(series);

        assertEquals(
                actualFields,
                expectedFields,
                "Incorrect query result with explicit non-equals Group/Rate/Aggregate order");
    }

    @Issue("4729")
    @Test(description = "test query result with aggregate limit")
    public void testAggregateLimit() {
        SeriesQuery query = new SeriesQuery(
                "*",
                TEST_METRIC,
                "2017-01-01T00:00:00Z",
                "2017-01-01T00:00:20Z");
        query.setAggregate(new Aggregate(
                AggregationType.MAX,
                new Period(5, TimeUnit.SECOND)
        ));
        query.setLimit(1);
        query.setDirection("ASC");

        Set<CheckedFields> actualFields = createCheckFields(querySeriesAsList(query));

        CheckedFields series1 = new CheckedFields(TEST_ENTITY1, Arrays.asList(
                Sample.ofDateDecimal("2017-01-01T00:00:00.000Z", new BigDecimal("103.0"))));

        CheckedFields series2 = new CheckedFields(TEST_ENTITY2, Arrays.asList(
                Sample.ofDateDecimal("2017-01-01T00:00:00.000Z", new BigDecimal("204.0"))));
        Set<CheckedFields> expectedFields = collectCheckFields(series1, series2);

        assertEquals(
                actualFields,
                expectedFields,
                "Incorrect query result with aggregate limit");
    }

    @Issue("4729")
    @Test(description = "test query result with group limit")
    public void testGroupLimit() {
        SeriesQuery query = new SeriesQuery(
                "*",
                TEST_METRIC,
                "2017-01-01T00:00:00Z",
                "2017-01-01T00:00:20Z");
        query.setGroup(new Group(
                GroupType.COUNT,
                new Period(10, TimeUnit.SECOND)
        ));
        query.setLimit(1);
        query.setDirection("ASC");

        Set<CheckedFields> actualFields = createCheckFields(querySeriesAsList(query));

        CheckedFields series = new CheckedFields("*", Arrays. asList(
                Sample.ofDateDecimal("2017-01-01T00:00:00.000Z", BigDecimal.valueOf(10.0d))));
        Set<CheckedFields> expectedFields = collectCheckFields(series);

        assertEquals(
                actualFields,
                expectedFields,
                "Incorrect query result with group limit");
    }

    @Issue("4729")
    @Test(description = "test query result with rate limit")
    public void testRateLimit() {
        SeriesQuery query = new SeriesQuery(
                "*",
                TEST_METRIC,
                "2017-01-01T00:00:00Z",
                "2017-01-01T00:00:20Z");
        query.setRate(new Rate(
                new Period(10, TimeUnit.SECOND)
        ));
        query.setLimit(1);
        query.setDirection("ASC");

        Set<CheckedFields> actualFields = createCheckFields(querySeriesAsList(query));

        CheckedFields series1 = new CheckedFields(TEST_ENTITY1, Arrays.asList(
                Sample.ofDateDecimal("2017-01-01T00:00:03.000Z", new BigDecimal("10.0"))));
        CheckedFields series2 = new CheckedFields(TEST_ENTITY2, Arrays.asList(
                Sample.ofDateDecimal("2017-01-01T00:00:02.000Z", new BigDecimal("10.0"))));
        Set<CheckedFields> expectedFields = collectCheckFields(series1, series2);

        assertEquals(
                actualFields,
                expectedFields,
                "Incorrect query result with group limit");
    }

    @Issue("4729")
    @Test(description = "test query result with aggregate seriesLimit")
    public void testAggregateSeriesLimit() {
        SeriesQuery query = new SeriesQuery(
                "*",
                TEST_METRIC,
                "2017-01-01T00:00:00Z",
                "2017-01-01T00:00:20Z");
        query.setAggregate(new Aggregate(
                AggregationType.MIN,
                new Period(10, TimeUnit.SECOND)
        ));
        query.setSeriesLimit(1);

        Set<CheckedFields> actualFields = createCheckFields(querySeriesAsList(query));

        CheckedFields series = new CheckedFields(TEST_ENTITY1, Arrays.asList(
                Sample.ofDateDecimal("2017-01-01T00:00:00.000Z", new BigDecimal("101.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:10.000Z", new BigDecimal("111.0"))));
        Set<CheckedFields> expectedFields = collectCheckFields(series);

        assertEquals(
                actualFields,
                expectedFields,
                "Incorrect query result with aggregate seriesLimit");
    }

    @Issue("4729")
    @Test(description = "test query result with group seriesLimit")
    public void testGroupSeriesLimit() {
        SeriesQuery query = new SeriesQuery(
                "*",
                TEST_METRIC,
                "2017-01-01T00:00:00Z",
                "2017-01-01T00:00:20Z");
        query.setGroup(new Group(
                GroupType.COUNT,
                new Period(5, TimeUnit.SECOND)
        ));
        query.setSeriesLimit(1);

        Set<CheckedFields> actualFields = createCheckFields(querySeriesAsList(query));

        CheckedFields series = new CheckedFields("*", Arrays.asList(
                Sample.ofDateDecimal("2017-01-01T00:00:00.000Z", BigDecimal.valueOf(5.0d)),
                Sample.ofDateDecimal("2017-01-01T00:00:05.000Z", BigDecimal.valueOf(5.0d)),
                Sample.ofDateDecimal("2017-01-01T00:00:10.000Z", BigDecimal.valueOf(5.0d)),
                Sample.ofDateDecimal("2017-01-01T00:00:15.000Z", BigDecimal.valueOf(5.0d))));
        Set<CheckedFields> expectedFields = collectCheckFields(series);

        assertEquals(
                actualFields,
                expectedFields,
                "Incorrect query result with group seriesLimit");
    }

    @Issue("4729")
    @Test(description = "test query result with rate seriesLimit")
    public void testRateSeriesLimit() {
        SeriesQuery query = new SeriesQuery(
                "*",
                TEST_METRIC,
                "2017-01-01T00:00:00Z",
                "2017-01-01T00:00:20Z");
        query.setRate(new Rate(
                new Period(5, TimeUnit.SECOND)
        ));
        query.setSeriesLimit(1);

        Set<CheckedFields> actualFields = createCheckFields(querySeriesAsList(query));

        CheckedFields series = new CheckedFields(TEST_ENTITY1, Arrays.asList(
                Sample.ofDateDecimal("2017-01-01T00:00:03.000Z", new BigDecimal("5.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:05.000Z", new BigDecimal("5.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:07.000Z", new BigDecimal("5.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:09.000Z", new BigDecimal("5.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:11.000Z", new BigDecimal("5.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:13.000Z", new BigDecimal("5.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:15.000Z", new BigDecimal("5.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:17.000Z", new BigDecimal("5.0")),
                Sample.ofDateDecimal("2017-01-01T00:00:19.000Z", new BigDecimal("5.0"))));
        Set<CheckedFields> expectedFields = collectCheckFields(series);

        assertEquals(
                actualFields,
                expectedFields,
                "Incorrect query result with rate");
    }

    private Set<CheckedFields> createCheckFields(List<Series> seriesList) {
        Set<CheckedFields> result = new HashSet<>();
        for (Series series: seriesList) {
            CheckedFields checkedFields = new CheckedFields(series.getEntity(), series.getData());
            result.add(checkedFields);
        }

        return result;
    }

    private Set<CheckedFields> collectCheckFields(CheckedFields... checkedFields) {
        return new HashSet<>(Arrays.asList(checkedFields));
    }
}
