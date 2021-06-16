package com.axibase.tsd.api.method.sql.function.string;


import com.axibase.tsd.api.method.sql.SqlTest;
import io.qameta.allure.Issue;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.axibase.tsd.api.method.sql.function.string.CommonData.POSSIBLE_STRING_FUNCTION_ARGS;
import static com.axibase.tsd.api.method.sql.function.string.CommonData.insertSeriesWithMetric;
import static com.axibase.tsd.api.util.Mocks.metric;

public class LengthTest extends SqlTest {
    private static String TEST_METRIC = metric();


    @BeforeClass
    public void prepareData() throws Exception {
        insertSeriesWithMetric(TEST_METRIC);
    }

    @DataProvider(name = "applyTestProvider")
    public Object[][] provideApplyTestsData() {
        Integer size = POSSIBLE_STRING_FUNCTION_ARGS.size();
        Object[][] result = new Object[size][1];
        for (int i = 0; i < size; i++) {
            result[i][0] = POSSIBLE_STRING_FUNCTION_ARGS.get(i);
        }
        return result;
    }

    @Issue("2920")
    @Test(dataProvider = "applyTestProvider")
    public void testApply(String param) throws Exception {
        String sqlQuery = String.format("SELECT LENGTH(%s) FROM \"%s\"",
                param, TEST_METRIC
        );
        assertOkRequest(String.format("Can't apply LOWER function to %s", param), queryResponse(sqlQuery));
    }
}
