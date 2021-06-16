package com.axibase.tsd.api.method.sql.trade;

import com.axibase.tsd.api.model.financial.Trade;
import com.axibase.tsd.api.util.TestUtil;
import com.axibase.tsd.api.util.TradeSender;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class TradeDailyTimeRangeTest extends SqlTradeTest {
    private static final String QUERY_TEMPLATE =
            "SELECT datetime, symbol FROM atsd_trade " +
                    "WHERE {instrument} AND {timeRange} " +
                    "WITH TIMEZONE = '{timeZone}' " +
                    "ORDER BY {orderBy}";

        private static final String AGGREGATION_QUERY_TEMPLATE = "SELECT datetime, symbol FROM atsd_trade " +
                "WHERE {instrument} AND {timeRange} " +
                "WITH TIMEZONE = '{timeZone}' " +
                "GROUP BY exchange, class, symbol, period(1 minute) " +
                "ORDER BY {orderBy}";

    @BeforeClass
    public void prepareData() throws Exception {
        List<Trade> trades = new ArrayList<>();
        String[] timestamps = {
                "2020-05-19T09:55:45.000000Z",
                "2020-05-19T10:00:00.000000Z",
                "2020-05-19T10:21:49.123000Z",
                "2020-05-19T10:59:59.999999Z",
                "2020-05-19T11:00:00.000000Z",
                "2020-05-19T11:05:00.000000Z",
                "2020-05-20T09:55:45.000000Z",
                "2020-05-20T10:00:00.000000Z",
                "2020-05-20T10:35:15.123000Z",
                "2020-05-20T10:43:03.456000Z",
                "2020-05-20T11:00:00.000000Z",
                "2020-05-20T11:05:00.000000Z",
        };

        // first instrument
        trades.addAll(fromISO(timestamps));
        // second instrument
        for (Trade trade : fromISO(timestamps)) {
            trades.add(trade.setSymbol(symbolTwo()));
        }
        TradeSender.send(trades).waitUntilTradesInsertedAtMost(1, TimeUnit.MINUTES);
    }

    @Test(dataProvider = "testData")
    public void test(TestConfig testConfig) throws Exception {
        String sql = testConfig.applyTemplate(QUERY_TEMPLATE);
        assertSqlQueryRows(testConfig.description, testConfig.expected, sql);
    }

        @Test(dataProvider = "testDataAggregation")
        public void testAggregation(TestConfig testConfig) throws Exception {
                String sql = testConfig.applyTemplate(AGGREGATION_QUERY_TEMPLATE);
                assertSqlQueryRows(testConfig.description, testConfig.expected, sql);
        }

        @DataProvider
        public Object[][] testDataAggregation() throws Exception {
                TestConfig[] data = {
                        test("Test HH:mm same day")
                                .setInstrument(instrumentCondition())
                                .setTimeRange("date_format(time, 'HH:mm') BETWEEN '10:20' AND '10:44' EXCL")
                                .addExpected("2020-05-19T10:21:00.000000Z", symbol())
                                .addExpected("2020-05-20T10:35:00.000000Z", symbol())
                                .addExpected("2020-05-20T10:43:00.000000Z", symbol()),
                        test("Test HH:mm next day")
                                .setInstrument(instrumentCondition())
                                .setTimeRange("date_format(time, 'HH:mm') BETWEEN '10:36' AND '10:20' EXCL")
                                .addExpected("2020-05-19T09:55:00.000000Z", symbol())
                                .addExpected("2020-05-19T10:00:00.000000Z", symbol())
                                .addExpected("2020-05-19T10:59:00.000000Z", symbol())
                                .addExpected("2020-05-19T11:00:00.000000Z", symbol())
                                .addExpected("2020-05-19T11:05:00.000000Z", symbol())
                                .addExpected("2020-05-20T09:55:00.000000Z", symbol())
                                .addExpected("2020-05-20T10:00:00.000000Z", symbol())
                                .addExpected("2020-05-20T10:43:00.000000Z", symbol())
                                .addExpected("2020-05-20T11:00:00.000000Z", symbol())
                                .addExpected("2020-05-20T11:05:00.000000Z", symbol())
                };
                return TestUtil.convertTo2DimArray(data);
        }

    @DataProvider
    public Object[][] testData() {
        TestConfig[] data = {
                // 'HH' pattern tests
                test("Test 'HH' pattern inclusive forward scan")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH') BETWEEN '10' AND '11'")
                        .addExpected("2020-05-19T10:00:00.000000Z", symbol())
                        .addExpected("2020-05-19T10:21:49.123000Z", symbol())
                        .addExpected("2020-05-19T10:59:59.999999Z", symbol())
                        .addExpected("2020-05-19T11:00:00.000000Z", symbol())
                        .addExpected("2020-05-19T11:05:00.000000Z", symbol())
                        .addExpected("2020-05-20T10:00:00.000000Z", symbol())
                        .addExpected("2020-05-20T10:35:15.123000Z", symbol())
                        .addExpected("2020-05-20T10:43:03.456000Z", symbol())
                        .addExpected("2020-05-20T11:00:00.000000Z", symbol())
                        .addExpected("2020-05-20T11:05:00.000000Z", symbol())
                        .addExpected("2020-05-19T10:00:00.000000Z", symbolTwo())
                        .addExpected("2020-05-19T10:21:49.123000Z", symbolTwo())
                        .addExpected("2020-05-19T10:59:59.999999Z", symbolTwo())
                        .addExpected("2020-05-19T11:00:00.000000Z", symbolTwo())
                        .addExpected("2020-05-19T11:05:00.000000Z", symbolTwo())
                        .addExpected("2020-05-20T10:00:00.000000Z", symbolTwo())
                        .addExpected("2020-05-20T10:35:15.123000Z", symbolTwo())
                        .addExpected("2020-05-20T10:43:03.456000Z", symbolTwo())
                        .addExpected("2020-05-20T11:00:00.000000Z", symbolTwo())
                        .addExpected("2020-05-20T11:05:00.000000Z", symbolTwo())
                ,
                test("Test 'HH' pattern inclusive backward scan")
                        .setInstrument(instrumentCondition())
                        .setOrderBy("datetime DESC")
                        .setTimeRange("date_format(time, 'HH') BETWEEN '10' AND '11'")
                        .addExpected("2020-05-20T11:05:00.000000Z", symbol())
                        .addExpected("2020-05-20T11:00:00.000000Z", symbol())
                        .addExpected("2020-05-20T10:43:03.456000Z", symbol())
                        .addExpected("2020-05-20T10:35:15.123000Z", symbol())
                        .addExpected("2020-05-20T10:00:00.000000Z", symbol())
                        .addExpected("2020-05-19T11:05:00.000000Z", symbol())
                        .addExpected("2020-05-19T11:00:00.000000Z", symbol())
                        .addExpected("2020-05-19T10:59:59.999999Z", symbol())
                        .addExpected("2020-05-19T10:21:49.123000Z", symbol())
                        .addExpected("2020-05-19T10:00:00.000000Z", symbol())

                ,
                test("Test 'HH' pattern upper boundary exclusive forward scan")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH') BETWEEN '10' AND '11' EXCL")
                        .addExpected("2020-05-19T10:00:00.000000Z", symbol())
                        .addExpected("2020-05-19T10:21:49.123000Z", symbol())
                        .addExpected("2020-05-19T10:59:59.999999Z", symbol())
                        .addExpected("2020-05-20T10:00:00.000000Z", symbol())
                        .addExpected("2020-05-20T10:35:15.123000Z", symbol())
                        .addExpected("2020-05-20T10:43:03.456000Z", symbol())
                        .addExpected("2020-05-19T10:00:00.000000Z", symbolTwo())
                        .addExpected("2020-05-19T10:21:49.123000Z", symbolTwo())
                        .addExpected("2020-05-19T10:59:59.999999Z", symbolTwo())
                        .addExpected("2020-05-20T10:00:00.000000Z", symbolTwo())
                        .addExpected("2020-05-20T10:35:15.123000Z", symbolTwo())
                        .addExpected("2020-05-20T10:43:03.456000Z", symbolTwo())
                ,
                test("Test 'HH' pattern upper boundary exclusive backward scan")
                        .setInstrument(instrumentCondition())
                        .setOrderBy("datetime DESC")
                        .setTimeRange("date_format(time, 'HH') BETWEEN '10' AND '11' EXCL")
                        .addExpected("2020-05-20T10:43:03.456000Z", symbol())
                        .addExpected("2020-05-20T10:35:15.123000Z", symbol())
                        .addExpected("2020-05-20T10:00:00.000000Z", symbol())
                        .addExpected("2020-05-19T10:59:59.999999Z", symbol())
                        .addExpected("2020-05-19T10:21:49.123000Z", symbol())
                        .addExpected("2020-05-19T10:00:00.000000Z", symbol())
                ,
                test("Test 'HH' pattern lower boundary exclusive forward scan")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH') BETWEEN '10' EXCL AND '11'")
                        .addExpected("2020-05-19T11:00:00.000000Z", symbol())
                        .addExpected("2020-05-19T11:05:00.000000Z", symbol())
                        .addExpected("2020-05-20T11:00:00.000000Z", symbol())
                        .addExpected("2020-05-20T11:05:00.000000Z", symbol())
                        .addExpected("2020-05-19T11:00:00.000000Z", symbolTwo())
                        .addExpected("2020-05-19T11:05:00.000000Z", symbolTwo())
                        .addExpected("2020-05-20T11:00:00.000000Z", symbolTwo())
                        .addExpected("2020-05-20T11:05:00.000000Z", symbolTwo())
                ,
                test("Test 'HH' pattern lower boundary exclusive backward scan")
                        .setInstrument(instrumentCondition())
                        .setOrderBy("datetime DESC")
                        .setTimeRange("date_format(time, 'HH') BETWEEN '10' EXCL AND '11'")
                        .addExpected("2020-05-20T11:05:00.000000Z", symbol())
                        .addExpected("2020-05-20T11:00:00.000000Z", symbol())
                        .addExpected("2020-05-19T11:05:00.000000Z", symbol())
                        .addExpected("2020-05-19T11:00:00.000000Z", symbol())
                ,
                test("Test 'HH' pattern multi-day interval forward scan")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH') BETWEEN '11' AND '10' EXCL")
                        .addExpected("2020-05-19T09:55:45.000000Z", symbol())
                        .addExpected("2020-05-19T11:00:00.000000Z", symbol())
                        .addExpected("2020-05-19T11:05:00.000000Z", symbol())
                        .addExpected("2020-05-20T09:55:45.000000Z", symbol())
                        .addExpected("2020-05-20T11:00:00.000000Z", symbol())
                        .addExpected("2020-05-20T11:05:00.000000Z", symbol())
                        .addExpected("2020-05-19T09:55:45.000000Z", symbolTwo())
                        .addExpected("2020-05-19T11:00:00.000000Z", symbolTwo())
                        .addExpected("2020-05-19T11:05:00.000000Z", symbolTwo())
                        .addExpected("2020-05-20T09:55:45.000000Z", symbolTwo())
                        .addExpected("2020-05-20T11:00:00.000000Z", symbolTwo())
                        .addExpected("2020-05-20T11:05:00.000000Z", symbolTwo())
                ,
                test("Test 'HH' pattern multi-day interval backward scan")
                        .setInstrument(instrumentCondition())
                        .setOrderBy("datetime DESC")
                        .setTimeRange("date_format(time, 'HH') BETWEEN '11' AND '10' EXCL")
                        .addExpected("2020-05-20T11:05:00.000000Z", symbol())
                        .addExpected("2020-05-20T11:00:00.000000Z", symbol())
                        .addExpected("2020-05-20T09:55:45.000000Z", symbol())
                        .addExpected("2020-05-19T11:05:00.000000Z", symbol())
                        .addExpected("2020-05-19T11:00:00.000000Z", symbol())
                        .addExpected("2020-05-19T09:55:45.000000Z", symbol())
                ,
                test("Test 'HH' pattern greater than condition")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH') > '10'")
                        .addExpected("2020-05-19T11:00:00.000000Z", symbol())
                        .addExpected("2020-05-19T11:05:00.000000Z", symbol())
                        .addExpected("2020-05-20T11:00:00.000000Z", symbol())
                        .addExpected("2020-05-20T11:05:00.000000Z", symbol())
                        .addExpected("2020-05-19T11:00:00.000000Z", symbolTwo())
                        .addExpected("2020-05-19T11:05:00.000000Z", symbolTwo())
                        .addExpected("2020-05-20T11:00:00.000000Z", symbolTwo())
                        .addExpected("2020-05-20T11:05:00.000000Z", symbolTwo())
                ,
                test("Test 'HH' pattern greater or equal condition")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH') >= '11'")
                        .addExpected("2020-05-19T11:00:00.000000Z", symbol())
                        .addExpected("2020-05-19T11:05:00.000000Z", symbol())
                        .addExpected("2020-05-20T11:00:00.000000Z", symbol())
                        .addExpected("2020-05-20T11:05:00.000000Z", symbol())
                        .addExpected("2020-05-19T11:00:00.000000Z", symbolTwo())
                        .addExpected("2020-05-19T11:05:00.000000Z", symbolTwo())
                        .addExpected("2020-05-20T11:00:00.000000Z", symbolTwo())
                        .addExpected("2020-05-20T11:05:00.000000Z", symbolTwo())
                ,
                test("Test 'HH' pattern equal condition")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH') = '11'")
                        .addExpected("2020-05-19T11:00:00.000000Z", symbol())
                        .addExpected("2020-05-19T11:05:00.000000Z", symbol())
                        .addExpected("2020-05-20T11:00:00.000000Z", symbol())
                        .addExpected("2020-05-20T11:05:00.000000Z", symbol())
                        .addExpected("2020-05-19T11:00:00.000000Z", symbolTwo())
                        .addExpected("2020-05-19T11:05:00.000000Z", symbolTwo())
                        .addExpected("2020-05-20T11:00:00.000000Z", symbolTwo())
                        .addExpected("2020-05-20T11:05:00.000000Z", symbolTwo())
                ,
                test("Test 'HH' pattern less than condition")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH') < '10'")
                        .addExpected("2020-05-19T09:55:45.000000Z", symbol())
                        .addExpected("2020-05-20T09:55:45.000000Z", symbol())
                        .addExpected("2020-05-19T09:55:45.000000Z", symbolTwo())
                        .addExpected("2020-05-20T09:55:45.000000Z", symbolTwo())

                ,
                test("Test 'HH' pattern less or equal condition")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH') <= '09'")
                        .addExpected("2020-05-19T09:55:45.000000Z", symbol())
                        .addExpected("2020-05-20T09:55:45.000000Z", symbol())
                        .addExpected("2020-05-19T09:55:45.000000Z", symbolTwo())
                        .addExpected("2020-05-20T09:55:45.000000Z", symbolTwo())
                ,
                // 'HH:mm' pattern tests
                test("Test 'HH:mm' pattern hour interval forward scan")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH:mm') BETWEEN '10:00' AND '11:00'")
                        .addExpected("2020-05-19T10:00:00.000000Z", symbol())
                        .addExpected("2020-05-19T10:21:49.123000Z", symbol())
                        .addExpected("2020-05-19T10:59:59.999999Z", symbol())
                        .addExpected("2020-05-19T11:00:00.000000Z", symbol())
                        .addExpected("2020-05-20T10:00:00.000000Z", symbol())
                        .addExpected("2020-05-20T10:35:15.123000Z", symbol())
                        .addExpected("2020-05-20T10:43:03.456000Z", symbol())
                        .addExpected("2020-05-20T11:00:00.000000Z", symbol())
                        .addExpected("2020-05-19T10:00:00.000000Z", symbolTwo())
                        .addExpected("2020-05-19T10:21:49.123000Z", symbolTwo())
                        .addExpected("2020-05-19T10:59:59.999999Z", symbolTwo())
                        .addExpected("2020-05-19T11:00:00.000000Z", symbolTwo())
                        .addExpected("2020-05-20T10:00:00.000000Z", symbolTwo())
                        .addExpected("2020-05-20T10:35:15.123000Z", symbolTwo())
                        .addExpected("2020-05-20T10:43:03.456000Z", symbolTwo())
                        .addExpected("2020-05-20T11:00:00.000000Z", symbolTwo())
                ,
                test("Test 'HH:mm' pattern hour interval backward scan")
                        .setInstrument(instrumentCondition())
                        .setOrderBy("datetime DESC")
                        .setTimeRange("date_format(time, 'HH:mm') BETWEEN '10:00' AND '11:00'")
                        .addExpected("2020-05-20T11:00:00.000000Z", symbol())
                        .addExpected("2020-05-20T10:43:03.456000Z", symbol())
                        .addExpected("2020-05-20T10:35:15.123000Z", symbol())
                        .addExpected("2020-05-20T10:00:00.000000Z", symbol())
                        .addExpected("2020-05-19T11:00:00.000000Z", symbol())
                        .addExpected("2020-05-19T10:59:59.999999Z", symbol())
                        .addExpected("2020-05-19T10:21:49.123000Z", symbol())
                        .addExpected("2020-05-19T10:00:00.000000Z", symbol())
                ,
                test("Test 'HH:mm' pattern upper boundary exclusive forward scan")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH:mm') BETWEEN '10:21' AND '11:00' EXCL")
                        .addExpected("2020-05-19T10:21:49.123000Z", symbol())
                        .addExpected("2020-05-19T10:59:59.999999Z", symbol())
                        .addExpected("2020-05-20T10:35:15.123000Z", symbol())
                        .addExpected("2020-05-20T10:43:03.456000Z", symbol())
                        .addExpected("2020-05-19T10:21:49.123000Z", symbolTwo())
                        .addExpected("2020-05-19T10:59:59.999999Z", symbolTwo())
                        .addExpected("2020-05-20T10:35:15.123000Z", symbolTwo())
                        .addExpected("2020-05-20T10:43:03.456000Z", symbolTwo())
                ,
                test("Test 'HH:mm' pattern upper boundary exclusive backward scan")
                        .setInstrument(instrumentCondition())
                        .setOrderBy("datetime DESC")
                        .setTimeRange("date_format(time, 'HH:mm') BETWEEN '10:21' AND '11:00' EXCL")
                        .addExpected("2020-05-20T10:43:03.456000Z", symbol())
                        .addExpected("2020-05-20T10:35:15.123000Z", symbol())
                        .addExpected("2020-05-19T10:59:59.999999Z", symbol())
                        .addExpected("2020-05-19T10:21:49.123000Z", symbol())
                ,
                test("Test 'HH:mm' pattern end of hour inclusive forward scan")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH:mm') BETWEEN '10:59' AND '10:59' ")
                        .addExpected("2020-05-19T10:59:59.999999Z", symbol())
                        .addExpected("2020-05-19T10:59:59.999999Z", symbolTwo())
                ,
                test("Test 'HH:mm' pattern end of hour inclusive backward scan")
                        .setInstrument(instrumentCondition())
                        .setOrderBy("datetime DESC")
                        .setTimeRange("date_format(time, 'HH:mm') BETWEEN '10:59' AND '10:59' ")
                        .addExpected("2020-05-19T10:59:59.999999Z", symbol())
                ,
                test("Test 'HH:mm' pattern end of hour exclusive forward scan")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH:mm') BETWEEN '10:59' AND '10:59' EXCL ")
                // an empty result expected
                ,
                test("Test 'HH:mm' pattern end of hour exclusive backward scan")
                        .setInstrument(instrumentCondition())
                        .setOrderBy("datetime DESC")
                        .setTimeRange("date_format(time, 'HH:mm') BETWEEN '10:59' AND '10:59' EXCL ")
                // an empty result expected
                ,
                test("Test 'HH:mm' pattern start of hour inclusive forward scan")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH:mm') BETWEEN '10:00' AND '10:01' ")
                        .addExpected("2020-05-19T10:00:00.000000Z", symbol())
                        .addExpected("2020-05-20T10:00:00.000000Z", symbol())
                        .addExpected("2020-05-19T10:00:00.000000Z", symbolTwo())
                        .addExpected("2020-05-20T10:00:00.000000Z", symbolTwo())

                ,
                test("Test 'HH:mm' pattern start of hour inclusive backward scan")
                        .setInstrument(instrumentCondition())
                        .setOrderBy("datetime DESC")
                        .setTimeRange("date_format(time, 'HH:mm') BETWEEN '10:00' AND '10:01' ")
                        .addExpected("2020-05-20T10:00:00.000000Z", symbol())
                        .addExpected("2020-05-19T10:00:00.000000Z", symbol())

                ,
                test("Test 'HH:mm' pattern start of hour exclusive forward scan")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH:mm') BETWEEN '10:00' EXCL AND '10:01' ")
                // an empty result expected
                ,
                test("Test 'HH:mm' pattern start of hour exclusive backward scan")
                        .setInstrument(instrumentCondition())
                        .setOrderBy("datetime DESC")
                        .setTimeRange("date_format(time, 'HH:mm') BETWEEN '10:01' EXCL AND '10:01' ")
                // an empty result expected
                ,
                test("Test 'HH:mm' pattern cross-hour interval forward scan")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH:mm') BETWEEN '10:30' AND '11:05' ")
                        .addExpected("2020-05-19T10:59:59.999999Z", symbol())
                        .addExpected("2020-05-19T11:00:00.000000Z", symbol())
                        .addExpected("2020-05-19T11:05:00.000000Z", symbol())
                        .addExpected("2020-05-20T10:35:15.123000Z", symbol())
                        .addExpected("2020-05-20T10:43:03.456000Z", symbol())
                        .addExpected("2020-05-20T11:00:00.000000Z", symbol())
                        .addExpected("2020-05-20T11:05:00.000000Z", symbol())
                        .addExpected("2020-05-19T10:59:59.999999Z", symbolTwo())
                        .addExpected("2020-05-19T11:00:00.000000Z", symbolTwo())
                        .addExpected("2020-05-19T11:05:00.000000Z", symbolTwo())
                        .addExpected("2020-05-20T10:35:15.123000Z", symbolTwo())
                        .addExpected("2020-05-20T10:43:03.456000Z", symbolTwo())
                        .addExpected("2020-05-20T11:00:00.000000Z", symbolTwo())
                        .addExpected("2020-05-20T11:05:00.000000Z", symbolTwo())
                ,
                test("Test 'HH:mm' pattern cross-hour interval backward scan")
                        .setInstrument(instrumentCondition())
                        .setOrderBy("datetime DESC")
                        .setTimeRange("date_format(time, 'HH:mm') BETWEEN '10:30' AND '11:05' ")
                        .addExpected("2020-05-20T11:05:00.000000Z", symbol())
                        .addExpected("2020-05-20T11:00:00.000000Z", symbol())
                        .addExpected("2020-05-20T10:43:03.456000Z", symbol())
                        .addExpected("2020-05-20T10:35:15.123000Z", symbol())
                        .addExpected("2020-05-19T11:05:00.000000Z", symbol())
                        .addExpected("2020-05-19T11:00:00.000000Z", symbol())
                        .addExpected("2020-05-19T10:59:59.999999Z", symbol())
                ,
                test("Test 'HH:mm' pattern greater than condition")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH:mm') > '11:04'")
                        .addExpected("2020-05-19T11:05:00.000000Z", symbol())
                        .addExpected("2020-05-20T11:05:00.000000Z", symbol())
                        .addExpected("2020-05-19T11:05:00.000000Z", symbolTwo())
                        .addExpected("2020-05-20T11:05:00.000000Z", symbolTwo())
                ,
                test("Test 'HH:mm' pattern greater or equal condition")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH:mm') >= '11:05'")
                        .addExpected("2020-05-19T11:05:00.000000Z", symbol())
                        .addExpected("2020-05-20T11:05:00.000000Z", symbol())
                        .addExpected("2020-05-19T11:05:00.000000Z", symbolTwo())
                        .addExpected("2020-05-20T11:05:00.000000Z", symbolTwo())
                ,
                test("Test 'HH:mm' pattern equal condition")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH:mm') = '11:05'")
                        .addExpected("2020-05-19T11:05:00.000000Z", symbol())
                        .addExpected("2020-05-20T11:05:00.000000Z", symbol())
                        .addExpected("2020-05-19T11:05:00.000000Z", symbolTwo())
                        .addExpected("2020-05-20T11:05:00.000000Z", symbolTwo())
                ,
                test("Test 'HH:mm' pattern less than condition")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH:mm') < '09:56'")
                        .addExpected("2020-05-19T09:55:45.000000Z", symbol())
                        .addExpected("2020-05-20T09:55:45.000000Z", symbol())
                        .addExpected("2020-05-19T09:55:45.000000Z", symbolTwo())
                        .addExpected("2020-05-20T09:55:45.000000Z", symbolTwo())

                ,
                test("Test 'HH:mm' pattern less or equal condition")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH:mm') <= '09:55'")
                        .addExpected("2020-05-19T09:55:45.000000Z", symbol())
                        .addExpected("2020-05-20T09:55:45.000000Z", symbol())
                        .addExpected("2020-05-19T09:55:45.000000Z", symbolTwo())
                        .addExpected("2020-05-20T09:55:45.000000Z", symbolTwo())

                // 'HH:mm:ss' pattern tests

                ,
                test("Test 'HH:mm:ss' pattern hour interval forward scan")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH:mm:ss') BETWEEN '10:00:00' AND '11:00:00'")
                        .addExpected("2020-05-19T10:00:00.000000Z", symbol())
                        .addExpected("2020-05-19T10:21:49.123000Z", symbol())
                        .addExpected("2020-05-19T10:59:59.999999Z", symbol())
                        .addExpected("2020-05-19T11:00:00.000000Z", symbol())
                        .addExpected("2020-05-20T10:00:00.000000Z", symbol())
                        .addExpected("2020-05-20T10:35:15.123000Z", symbol())
                        .addExpected("2020-05-20T10:43:03.456000Z", symbol())
                        .addExpected("2020-05-20T11:00:00.000000Z", symbol())
                        .addExpected("2020-05-19T10:00:00.000000Z", symbolTwo())
                        .addExpected("2020-05-19T10:21:49.123000Z", symbolTwo())
                        .addExpected("2020-05-19T10:59:59.999999Z", symbolTwo())
                        .addExpected("2020-05-19T11:00:00.000000Z", symbolTwo())
                        .addExpected("2020-05-20T10:00:00.000000Z", symbolTwo())
                        .addExpected("2020-05-20T10:35:15.123000Z", symbolTwo())
                        .addExpected("2020-05-20T10:43:03.456000Z", symbolTwo())
                        .addExpected("2020-05-20T11:00:00.000000Z", symbolTwo())
                ,
                test("Test 'HH:mm:ss' pattern hour interval backward scan")
                        .setInstrument(instrumentCondition())
                        .setOrderBy("datetime DESC")
                        .setTimeRange("date_format(time, 'HH:mm:ss') BETWEEN '10:00:00' AND '11:00:00'")
                        .addExpected("2020-05-20T11:00:00.000000Z", symbol())
                        .addExpected("2020-05-20T10:43:03.456000Z", symbol())
                        .addExpected("2020-05-20T10:35:15.123000Z", symbol())
                        .addExpected("2020-05-20T10:00:00.000000Z", symbol())
                        .addExpected("2020-05-19T11:00:00.000000Z", symbol())
                        .addExpected("2020-05-19T10:59:59.999999Z", symbol())
                        .addExpected("2020-05-19T10:21:49.123000Z", symbol())
                        .addExpected("2020-05-19T10:00:00.000000Z", symbol())
                ,
                test("Test 'HH:mm:ss' pattern upper boundary exclusive forward scan")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH:mm:ss') BETWEEN '10:21:49' AND '11:00:00' EXCL")
                        .addExpected("2020-05-19T10:21:49.123000Z", symbol())
                        .addExpected("2020-05-19T10:59:59.999999Z", symbol())
                        .addExpected("2020-05-20T10:35:15.123000Z", symbol())
                        .addExpected("2020-05-20T10:43:03.456000Z", symbol())
                        .addExpected("2020-05-19T10:21:49.123000Z", symbolTwo())
                        .addExpected("2020-05-19T10:59:59.999999Z", symbolTwo())
                        .addExpected("2020-05-20T10:35:15.123000Z", symbolTwo())
                        .addExpected("2020-05-20T10:43:03.456000Z", symbolTwo())
                ,
                test("Test 'HH:mm:ss' pattern upper boundary exclusive backward scan")
                        .setInstrument(instrumentCondition())
                        .setOrderBy("datetime DESC")
                        .setTimeRange("date_format(time, 'HH:mm:ss') BETWEEN '10:21:49' AND '11:00:00' EXCL")
                        .addExpected("2020-05-20T10:43:03.456000Z", symbol())
                        .addExpected("2020-05-20T10:35:15.123000Z", symbol())
                        .addExpected("2020-05-19T10:59:59.999999Z", symbol())
                        .addExpected("2020-05-19T10:21:49.123000Z", symbol())
                ,
                test("Test 'HH:mm:ss' pattern end of hour inclusive forward scan")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH:mm:ss') BETWEEN '10:59:59' AND '10:59:59' ")
                        .addExpected("2020-05-19T10:59:59.999999Z", symbol())
                        .addExpected("2020-05-19T10:59:59.999999Z", symbolTwo())
                ,
                test("Test 'HH:mm:ss' pattern end of hour inclusive backward scan")
                        .setInstrument(instrumentCondition())
                        .setOrderBy("datetime DESC")
                        .setTimeRange("date_format(time, 'HH:mm:ss') BETWEEN '10:59:59' AND '10:59:59' ")
                        .addExpected("2020-05-19T10:59:59.999999Z", symbol())
                ,
                test("Test 'HH:mm:ss' pattern end of hour exclusive forward scan")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH:mm:ss') BETWEEN '10:59:59' AND '10:59:59' EXCL ")
                // an empty result expected
                ,
                test("Test 'HH:mm:ss' pattern end of hour exclusive backward scan")
                        .setInstrument(instrumentCondition())
                        .setOrderBy("datetime DESC")
                        .setTimeRange("date_format(time, 'HH:mm:ss') BETWEEN '10:59:59' AND '10:59:59' EXCL ")
                // an empty result expected
                ,
                test("Test 'HH:mm:ss' pattern start of hour inclusive forward scan")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH:mm:ss') BETWEEN '10:00:00' AND '10:00:01' ")
                        .addExpected("2020-05-19T10:00:00.000000Z", symbol())
                        .addExpected("2020-05-20T10:00:00.000000Z", symbol())
                        .addExpected("2020-05-19T10:00:00.000000Z", symbolTwo())
                        .addExpected("2020-05-20T10:00:00.000000Z", symbolTwo())

                ,
                test("Test 'HH:mm:ss' pattern start of hour inclusive backward scan")
                        .setInstrument(instrumentCondition())
                        .setOrderBy("datetime DESC")
                        .setTimeRange("date_format(time, 'HH:mm:ss') BETWEEN '10:00:00' AND '10:00:01' ")
                        .addExpected("2020-05-20T10:00:00.000000Z", symbol())
                        .addExpected("2020-05-19T10:00:00.000000Z", symbol())

                ,
                test("Test 'HH:mm:ss' pattern start of hour exclusive forward scan")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH:mm:ss') BETWEEN '10:00:00' EXCL AND '10:00:01' ")
                // an empty result expected
                ,
                test("Test 'HH:mm:ss' pattern start of hour exclusive backward scan")
                        .setInstrument(instrumentCondition())
                        .setOrderBy("datetime DESC")
                        .setTimeRange("date_format(time, 'HH:mm:ss') BETWEEN '10:00:00' EXCL AND '10:00:01' ")
                // an empty result expected
                ,
                test("Test 'HH:mm:ss' pattern greater than condition")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH:mm:ss') > '11:04:59'")
                        .addExpected("2020-05-19T11:05:00.000000Z", symbol())
                        .addExpected("2020-05-20T11:05:00.000000Z", symbol())
                        .addExpected("2020-05-19T11:05:00.000000Z", symbolTwo())
                        .addExpected("2020-05-20T11:05:00.000000Z", symbolTwo())
                ,
                test("Test 'HH:mm:ss' pattern greater or equal condition")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH:mm:ss') >= '11:05:00'")
                        .addExpected("2020-05-19T11:05:00.000000Z", symbol())
                        .addExpected("2020-05-20T11:05:00.000000Z", symbol())
                        .addExpected("2020-05-19T11:05:00.000000Z", symbolTwo())
                        .addExpected("2020-05-20T11:05:00.000000Z", symbolTwo())
                ,
                test("Test 'HH:mm:ss' pattern equal condition")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH:mm:ss') = '11:05:00'")
                        .addExpected("2020-05-19T11:05:00.000000Z", symbol())
                        .addExpected("2020-05-20T11:05:00.000000Z", symbol())
                        .addExpected("2020-05-19T11:05:00.000000Z", symbolTwo())
                        .addExpected("2020-05-20T11:05:00.000000Z", symbolTwo())
                ,
                test("Test 'HH:mm:ss' pattern less than condition")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH:mm:ss') < '09:55:46'")
                        .addExpected("2020-05-19T09:55:45.000000Z", symbol())
                        .addExpected("2020-05-20T09:55:45.000000Z", symbol())
                        .addExpected("2020-05-19T09:55:45.000000Z", symbolTwo())
                        .addExpected("2020-05-20T09:55:45.000000Z", symbolTwo())
                ,
                test("Test 'HH:mm:ss' pattern less or equal condition")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH:mm:ss') <= '09:55:45'")
                        .addExpected("2020-05-19T09:55:45.000000Z", symbol())
                        .addExpected("2020-05-20T09:55:45.000000Z", symbol())
                        .addExpected("2020-05-19T09:55:45.000000Z", symbolTwo())
                        .addExpected("2020-05-20T09:55:45.000000Z", symbolTwo())

                // 'HH:mm:ss.SSS' pattern tests

                ,
                test("Test 'HH:mm:ss.SSS' pattern hour interval forward scan")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH:mm:ss.SSS') BETWEEN '10:00:00.000' AND '11:00:00.000'")
                        .addExpected("2020-05-19T10:00:00.000000Z", symbol())
                        .addExpected("2020-05-19T10:21:49.123000Z", symbol())
                        .addExpected("2020-05-19T10:59:59.999999Z", symbol())
                        .addExpected("2020-05-19T11:00:00.000000Z", symbol())
                        .addExpected("2020-05-20T10:00:00.000000Z", symbol())
                        .addExpected("2020-05-20T10:35:15.123000Z", symbol())
                        .addExpected("2020-05-20T10:43:03.456000Z", symbol())
                        .addExpected("2020-05-20T11:00:00.000000Z", symbol())
                        .addExpected("2020-05-19T10:00:00.000000Z", symbolTwo())
                        .addExpected("2020-05-19T10:21:49.123000Z", symbolTwo())
                        .addExpected("2020-05-19T10:59:59.999999Z", symbolTwo())
                        .addExpected("2020-05-19T11:00:00.000000Z", symbolTwo())
                        .addExpected("2020-05-20T10:00:00.000000Z", symbolTwo())
                        .addExpected("2020-05-20T10:35:15.123000Z", symbolTwo())
                        .addExpected("2020-05-20T10:43:03.456000Z", symbolTwo())
                        .addExpected("2020-05-20T11:00:00.000000Z", symbolTwo())
                ,
                test("Test 'HH:mm:ss.SSS' pattern hour interval backward scan")
                        .setInstrument(instrumentCondition())
                        .setOrderBy("datetime DESC")
                        .setTimeRange("date_format(time, 'HH:mm:ss.SSS') BETWEEN '10:00:00.000' AND '11:00:00.000'")
                        .addExpected("2020-05-20T11:00:00.000000Z", symbol())
                        .addExpected("2020-05-20T10:43:03.456000Z", symbol())
                        .addExpected("2020-05-20T10:35:15.123000Z", symbol())
                        .addExpected("2020-05-20T10:00:00.000000Z", symbol())
                        .addExpected("2020-05-19T11:00:00.000000Z", symbol())
                        .addExpected("2020-05-19T10:59:59.999999Z", symbol())
                        .addExpected("2020-05-19T10:21:49.123000Z", symbol())
                        .addExpected("2020-05-19T10:00:00.000000Z", symbol())
                ,
                test("Test 'HH:mm:ss.SSS' pattern upper boundary exclusive forward scan")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH:mm:ss.SSS') BETWEEN '10:21:49.123' AND '11:00:00.000' EXCL")
                        .addExpected("2020-05-19T10:21:49.123000Z", symbol())
                        .addExpected("2020-05-19T10:59:59.999999Z", symbol())
                        .addExpected("2020-05-20T10:35:15.123000Z", symbol())
                        .addExpected("2020-05-20T10:43:03.456000Z", symbol())
                        .addExpected("2020-05-19T10:21:49.123000Z", symbolTwo())
                        .addExpected("2020-05-19T10:59:59.999999Z", symbolTwo())
                        .addExpected("2020-05-20T10:35:15.123000Z", symbolTwo())
                        .addExpected("2020-05-20T10:43:03.456000Z", symbolTwo())
                ,
                test("Test 'HH:mm:ss.SSS' pattern upper boundary exclusive backward scan")
                        .setInstrument(instrumentCondition())
                        .setOrderBy("datetime DESC")
                        .setTimeRange("date_format(time, 'HH:mm:ss.SSS') BETWEEN '10:21:49.123' AND '11:00:00.000' EXCL")
                        .addExpected("2020-05-20T10:43:03.456000Z", symbol())
                        .addExpected("2020-05-20T10:35:15.123000Z", symbol())
                        .addExpected("2020-05-19T10:59:59.999999Z", symbol())
                        .addExpected("2020-05-19T10:21:49.123000Z", symbol())
                ,
                test("Test 'HH:mm:ss.SSS' pattern end of hour inclusive forward scan")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH:mm:ss.SSS') BETWEEN '10:59:59.999' AND '10:59:59.999' ")
                        .addExpected("2020-05-19T10:59:59.999999Z", symbol())
                        .addExpected("2020-05-19T10:59:59.999999Z", symbolTwo())
                ,
                test("Test 'HH:mm:ss.SSS' pattern end of hour inclusive backward scan")
                        .setInstrument(instrumentCondition())
                        .setOrderBy("datetime DESC")
                        .setTimeRange("date_format(time, 'HH:mm:ss.SSS') BETWEEN '10:59:59.999' AND '10:59:59.999' ")
                        .addExpected("2020-05-19T10:59:59.999999Z", symbol())
                ,
                test("Test 'HH:mm:ss.SSS' pattern end of hour exclusive forward scan")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH:mm:ss.SSS') BETWEEN '10:59:59.999' AND '10:59:59.999' EXCL ")
                // an empty result expected
                ,
                test("Test 'HH:mm:ss.SSS' pattern end of hour exclusive backward scan")
                        .setInstrument(instrumentCondition())
                        .setOrderBy("datetime DESC")
                        .setTimeRange("date_format(time, 'HH:mm:ss.SSS') BETWEEN '10:59:59.999' AND '10:59:59.999' EXCL ")
                // an empty result expected
                ,
                test("Test 'HH:mm:ss.SSS' pattern start of hour inclusive forward scan")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH:mm:ss.SSS') BETWEEN '10:00:00.000' AND '10:00:00.001' ")
                        .addExpected("2020-05-19T10:00:00.000000Z", symbol())
                        .addExpected("2020-05-20T10:00:00.000000Z", symbol())
                        .addExpected("2020-05-19T10:00:00.000000Z", symbolTwo())
                        .addExpected("2020-05-20T10:00:00.000000Z", symbolTwo())

                ,
                test("Test 'HH:mm:ss.SSS' pattern start of hour inclusive backward scan")
                        .setInstrument(instrumentCondition())
                        .setOrderBy("datetime DESC")
                        .setTimeRange("date_format(time, 'HH:mm:ss.SSS') BETWEEN '10:00:00.000' AND '10:00:00.001' ")
                        .addExpected("2020-05-20T10:00:00.000000Z", symbol())
                        .addExpected("2020-05-19T10:00:00.000000Z", symbol())

                ,
                test("Test 'HH:mm:ss.SSS' pattern start of hour exclusive forward scan")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH:mm:ss.SSS') BETWEEN '10:00:00.000' EXCL AND '10:00:00.001' ")
                // an empty result expected
                ,
                test("Test 'HH:mm:ss.SSS' pattern start of hour exclusive backward scan")
                        .setInstrument(instrumentCondition())
                        .setOrderBy("datetime DESC")
                        .setTimeRange("date_format(time, 'HH:mm:ss.SSS') BETWEEN '10:00:00.000' EXCL AND '10:00:00.001' ")
                // an empty result expected

                ,
                test("Test 'HH:mm:ss.SSS' Europe/Moscow timezone forward scan")
                        .setTimeZone("Europe/Moscow")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH:mm:ss.SSS') BETWEEN '13:21:49.123' AND '13:43:03.456' ")
                        .addExpected("2020-05-19T10:21:49.123000Z", symbol())
                        .addExpected("2020-05-20T10:35:15.123000Z", symbol())
                        .addExpected("2020-05-20T10:43:03.456000Z", symbol())
                        .addExpected("2020-05-19T10:21:49.123000Z", symbolTwo())
                        .addExpected("2020-05-20T10:35:15.123000Z", symbolTwo())
                        .addExpected("2020-05-20T10:43:03.456000Z", symbolTwo())
                ,
                test("Test 'HH:mm:ss.SSS' Europe/Moscow timezone backward scan")
                        .setTimeZone("Europe/Moscow")
                        .setInstrument(instrumentCondition())
                        .setOrderBy("datetime DESC")
                        .setTimeRange("date_format(time, 'HH:mm:ss.SSS') BETWEEN '13:21:49.123' AND '13:43:03.456' ")
                        .addExpected("2020-05-20T10:43:03.456000Z", symbol())
                        .addExpected("2020-05-20T10:35:15.123000Z", symbol())
                        .addExpected("2020-05-19T10:21:49.123000Z", symbol())
                ,
                test("Test 'HH:mm:ss.SSS' Europe/Moscow timezone in date_format function forward scan")
                        .setTimeZone("UTC")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH:mm:ss.SSS', 'Europe/Moscow') BETWEEN '13:21:49.123' AND '13:43:03.456' ")
                        .addExpected("2020-05-19T10:21:49.123000Z", symbol())
                        .addExpected("2020-05-20T10:35:15.123000Z", symbol())
                        .addExpected("2020-05-20T10:43:03.456000Z", symbol())
                        .addExpected("2020-05-19T10:21:49.123000Z", symbolTwo())
                        .addExpected("2020-05-20T10:35:15.123000Z", symbolTwo())
                        .addExpected("2020-05-20T10:43:03.456000Z", symbolTwo())
                ,
                test("Test complex condition")
                        .setInstrument(classCondition())
                        .setTimeRange("date_format(time, 'HH:mm') BETWEEN '09:00' and '10:10' and date_format(time, 'HH:mm:ss') > '09:55:50' and date_format(time, 'HH:mm:ss') < '12:00:00'")
                        .addExpected("2020-05-19T10:00:00.000000Z", symbol())
                        .addExpected("2020-05-20T10:00:00.000000Z", symbol())
                        .addExpected("2020-05-19T10:00:00.000000Z", symbolTwo())
                        .addExpected("2020-05-20T10:00:00.000000Z", symbolTwo())

        };
        return TestUtil.convertTo2DimArray(data);
    }

    private TestConfig test(String description) {
        return new TestConfig(description);
    }

    @Setter
    @Accessors(chain = true)
    @RequiredArgsConstructor
    private static class TestConfig {
        private final String description;
        private String instrument;
        private String timeRange;
        private String orderBy = "symbol, time";
        private String timeZone = "UTC";
        private List<List<String>> expected = new ArrayList<>();

        private String applyTemplate(String template) {
                return template
                        .replace("{instrument}", instrument)
                        .replace("{timeRange}", timeRange)
                        .replace("{orderBy}", orderBy)
                        .replace("{timeZone}", timeZone);
        }

        private TestConfig addExpected(String... row) {
                expected.add(Arrays.stream(row).map(String::toUpperCase).collect(Collectors.toList()));
                return this;
        }

            @Override
            public String toString() {
                    return " " + description + " ";
        }
    }
}