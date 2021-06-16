package com.axibase.tsd.api.method.series.trade;

import com.axibase.tsd.api.method.BaseMethod;
import com.axibase.tsd.api.method.series.SeriesMethod;
import com.axibase.tsd.api.model.Period;
import com.axibase.tsd.api.model.financial.Trade;
import com.axibase.tsd.api.model.series.Sample;
import com.axibase.tsd.api.model.series.Series;
import com.axibase.tsd.api.model.series.query.SeriesQuery;
import com.axibase.tsd.api.model.series.query.transformation.Transformation;
import com.axibase.tsd.api.model.series.query.transformation.aggregate.Aggregate;
import com.axibase.tsd.api.model.series.query.transformation.aggregate.AggregationType;
import com.axibase.tsd.api.model.series.query.transformation.group.Group;
import com.axibase.tsd.api.model.series.query.transformation.group.GroupType;
import com.axibase.tsd.api.util.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import static com.axibase.tsd.api.model.TimeUnit.MINUTE;
import static com.axibase.tsd.api.util.Util.getUnixTime;

public class GroupingTest {
    private final String metric = TradeUtil.tradeQuantityMetric();
    private final String exchange = Mocks.tradeExchange();
    private final String clazz = Mocks.tradeClass();
    private final String symbol = Mocks.tradeSymbol();
    private final String entity = TradeUtil.tradeEntity(symbol, clazz);
    private int insertedTradesCount;
    private Trade firstTrade;
    private Trade lastTrade;

    @BeforeClass
    public void insertTrades() throws Exception {
        List<Trade> trades = new ArrayList<>();
        try (Scanner scanner = new Scanner(GroupingTest.class.getClassLoader().getResourceAsStream("csv/trades.csv"))) {
            while (scanner.hasNextLine()) {
                String[] values = scanner.nextLine().split(",");
                String date = values[0];
                Trade.Side side = Trade.Side.valueOf(values[1]);
                Trade trade = trade(++insertedTradesCount, date, side);
                if (firstTrade == null) firstTrade = trade;
                lastTrade = trade;
                trades.add(trade);
            }
        }
        TradeSender.send(trades).waitUntilTradesInsertedAtMost(1, TimeUnit.MINUTES);
    }

    @DataProvider
    public Object[][] testCases() {
        Period period = new Period(1, MINUTE);
        Group groupCount = new Group(GroupType.COUNT);
        Group groupCountPerMinute = new Group(GroupType.COUNT, period);
        Group groupSumPerMinute = new Group(GroupType.SUM, period);
        Aggregate aggregateSum = new Aggregate(AggregationType.SUM, period);
        Aggregate aggregateCount = new Aggregate(AggregationType.COUNT, period);
        SeriesQuery[] testCases = {
                buildQuery(groupCountPerMinute, null, Arrays.asList(Transformation.GROUP)),
                /** Disable this case because grouping without period behaves differently for 'trade' and 'd' tables. */
                //buildQuery(groupCount, aggregateSum, Arrays.asList(Transformation.GROUP, Transformation.AGGREGATE)),
                buildQuery(groupCountPerMinute, aggregateSum, Arrays.asList(Transformation.GROUP, Transformation.AGGREGATE)),
                buildQuery(groupSumPerMinute, aggregateCount, Arrays.asList(Transformation.AGGREGATE, Transformation.GROUP))
        };
        return TestUtil.convertTo2DimArray(testCases);
    }

    @Test(dataProvider = "testCases")
    public void test(SeriesQuery query) throws JsonProcessingException, JSONException {
        Response response = SeriesMethod.querySeries(query);
        JsonNode responseArray = BaseMethod.responseAsTree(response);
        Assert.assertEquals(responseArray.size(), 1, "One series expected in response.");
        String expectedData =
                "[{\"d\":\"2020-11-25T14:00:00.000Z\",\"v\":41.0},{\"d\":\"2020-11-25T14:01:00.000Z\",\"v\":70.0}]";
        String actualData = responseArray.get(0).get("data").toString();
        JSONAssert.assertEquals(expectedData, actualData, JSONCompareMode.STRICT);
    }

    @Test
    public void testDetailGrouping() {
        SeriesQuery query = buildQuery(new Group(GroupType.DETAIL), null, null);
        List<Series> response = SeriesMethod.querySeriesAsList(query);
        Assert.assertEquals(response.size(), 1);
        List<Sample> actualSeries = response.get(0).getData();
        Assert.assertEquals(actualSeries.size(), insertedTradesCount);
        Sample firstSample = actualSeries.get(0);
        assertTheSame(firstSample, firstTrade, "Unexpected first sample in response.");
        Sample lastSample = actualSeries.get(actualSeries.size() - 1);
        assertTheSame(lastSample, lastTrade, "Unexpected last sample in response.");
    }

    private void assertTheSame(Sample actual, Trade expected, String assertionMessage) {
        long actualTime = TimeUtil.epochNano(actual.getRawDate());
        long expectedTime = expected.epochNano();
        Assert.assertEquals(actualTime, expectedTime, assertionMessage + " Wrong timestamp.");
        Assert.assertEquals(actual.getValue().compareTo(expected.getPrice()), 0, assertionMessage + " Wrong price.");
        Assert.assertEquals(actual.getText(), Long.toString(expected.getNumber()), assertionMessage + " Wrong trade number.");
    }

    private Trade trade(int tradeNumber, String date, Trade.Side side) {
        Trade trade = new Trade(exchange, clazz, symbol, tradeNumber, getUnixTime(date), BigDecimal.ONE, 1);
        trade.setSide(side);
        return trade;
    }

    private SeriesQuery buildQuery(Group group, Aggregate aggregate, List<Transformation> order) {
        return new SeriesQuery()
                .setMetric(metric)
                .setEntity(entity)
                .setStartDate("2020-11-25T14:00:00Z")
                .setEndDate("2020-11-25T14:02:00Z")
                .setGroup(group)
                .setAggregate(aggregate)
                .setTransformationOrder(order)
                .setTimeFormat("ISO");
    }
}
