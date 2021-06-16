package com.axibase.tsd.api.method.sql.function.string;

import com.axibase.tsd.api.method.series.SeriesMethod;
import com.axibase.tsd.api.method.sql.SqlTest;
import com.axibase.tsd.api.model.series.Series;
import com.axibase.tsd.api.model.sql.StringTable;
import com.axibase.tsd.api.util.Mocks;
import io.qameta.allure.Issue;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.axibase.tsd.api.method.sql.function.string.CommonData.POSSIBLE_STRING_FUNCTION_ARGS;
import static com.axibase.tsd.api.method.sql.function.string.CommonData.insertSeriesWithMetric;
import static com.axibase.tsd.api.util.Mocks.metric;
import static org.testng.AssertJUnit.assertEquals;


public class IsNullTest extends SqlTest {
    private static String TEST_METRIC = metric();

    @BeforeClass
    public void prepareData() throws Exception {
        insertSeriesWithMetric(TEST_METRIC);
    }

    @DataProvider(name = "applyTestProvider")
    public Iterator<Object[]> provideApplyTestsData() {
        List<Object[]> list = new ArrayList<>();
        for (String argLeft : POSSIBLE_STRING_FUNCTION_ARGS) {
            for (String argRight : POSSIBLE_STRING_FUNCTION_ARGS) {
                list.add(new String[]{String.format("%s, %s", argLeft, argRight)});
            }
        }
        return list.iterator();
    }

    @Issue("2920")
    @Test(dataProvider = "applyTestProvider")
    public void testApply(String params) throws Exception {
        String sqlQuery = String.format("SELECT ISNULL(%s) FROM \"%s\"",
                params, TEST_METRIC
        );
        assertOkRequest(String.format("Can't apply ISNULL function to %s", params), queryResponse(sqlQuery));
    }

    @BeforeClass
    public void beforeTestTypePreserved() throws Exception {
        Series series = new Series("e-test-type-preserve-1", "m-test-type-preserve-1");
        series.addSamples(Mocks.SAMPLE);
        SeriesMethod.insertSeriesCheck(series);
    }

    @Issue("3675")
    @DataProvider(name = "typeCheckTestProvider")
    public Object[][] provideCheckTestData() {
        return new Object[][]{
                // Not Null
                {"metric", "'2'", "string"},
                {"metric", "2", "java_object"},
                {"metric", "metric", "string"},
                {"metric", "value+1", "java_object"},
                {"'m'", "'2'", "string"},
                {"'m'", "2", "java_object"},
                {"'m'", "metric", "string"},
                {"'m'", "value+1", "java_object"},

                {"value", "'2'", "java_object"},
                {"value", "2", "double"},
                {"value", "metric", "java_object"},
                {"value", "value+1", "double"},
                {"1", "'2'", "java_object"},
                {"1", "2", "bigint"},
                {"1", "metric", "java_object"},
                {"1", "value+1", "double"},

                // Null
                {"metric.tags", "'2'", "string"},
                {"metric.tags", "2", "java_object"},
                {"metric.tags", "metric", "string"},
                {"metric.tags", "value+1", "java_object"},

                {"metric.label", "'2'", "string"},
                {"metric.label", "2", "java_object"},
                {"metric.label", "metric", "string"},
                {"metric.label", "value+1", "java_object"},

                {"value * 0 / 0", "'2'", "java_object"},
                {"value * 0 / 0", "2", "double"},
                {"value * 0 / 0", "metric", "java_object"},
                {"value * 0 / 0", "value+1", "double"},
                {"0 / 0", "'2'", "java_object"},
                {"0 / 0", "2", "double"},
                {"0 / 0", "metric", "java_object"},
                {"0 / 0", "value+1", "double"},
        };
    }

    @Issue("3675")
    @Test(dataProvider = "typeCheckTestProvider")
    public void testTypePreserved(String from, String to, String type) throws Exception {
        String sqlQuery = String.format("SELECT ISNULL(%s, %s) FROM \"m-test-type-preserve-1\"", from, to);
        StringTable table = queryTable(sqlQuery, 1);
        assertEquals("wrong ISNULL result data type", type, table.getColumnMetaData(0).getDataType());
    }


    @DataProvider(name = "functionResultProvider")
    public Object[][] provideSelectTestsData() {
        return new Object[][]{
                {"'','VaLuE'", ""},
                {"text,'VaLuE'", "VaLuE"},
                {"text,text", "null"},
                {"tags.a,'text'", "text"},
                {"tags.a, metric", TEST_METRIC},
                {"tags.a, CONCAT('text-', metric)", "text-" + TEST_METRIC}

        };
    }

    @Test(dataProvider = "functionResultProvider")
    public void testFunctionResult(String text, String expectedValue) throws Exception {
        String sqlQuery = String.format(
                "SELECT ISNULL(%s) FROM \"%s\"",
                text, TEST_METRIC
        );
        String actualValue = queryTable(sqlQuery).getValueAt(0, 0);
        String assertMessage = String.format("Incorrect result of ISNULL function with params (%s) .%n\tQuery: %s%n",
                text, sqlQuery
        );
        assertEquals(assertMessage, actualValue, expectedValue);
    }

    @Issue("3844")
    @Test
    public void testIsNullInExpression() throws Exception {
        String sqlQuery = String.format("SELECT ROUND(100 - ISNULL(value, 0)) FROM \"%s\"", TEST_METRIC);

        StringTable resultTable = queryResponse(sqlQuery).readEntity(StringTable.class);

        String[][] expectedRows = {{"-23.0"}};

        assertTableRowsExist(expectedRows, resultTable);
    }
}
