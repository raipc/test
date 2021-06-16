package com.axibase.tsd.api.method.sql;

import com.axibase.tsd.api.model.sql.ColumnMetaData;
import com.axibase.tsd.api.model.sql.StringTable;
import com.axibase.tsd.api.util.Util;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;

import static com.axibase.tsd.api.util.TestUtil.twoDArrayToList;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.testng.AssertJUnit.*;


public abstract class SqlTest extends SqlMethod {
    private static final String DEFAULT_ASSERT_OK_REQUEST_MESSAGE = "Response status is not ok";
    private static final String DEFAULT_ASSERT_BAD_REQUEST_MESSAGE = "Response status is not bad";


    private static void assertTableRowsExist(String errorMessage, List<List<String>> expectedRows, StringTable table) {
        List<List<String>> actualRows = table.getRows();
        if (actualRows.size() != expectedRows.size()) {
            failNotEquals(errorMessage, expectedRows, actualRows);
        }
        for (int i = 0; i < actualRows.size(); i++) {
            List<String> actualRow = actualRows.get(i);
            List<String> expectedRow = expectedRows.get(i);
            int actualRowSize = actualRow.size();
            int expectedRowSize = expectedRow.size();
            if (actualRowSize != expectedRowSize) {
                failNotEquals(errorMessage, expectedRows, actualRows);
            }
            for (int j = 0; j < actualRow.size(); j++) {
                String dataType = table.getColumnMetaData(j).getDataType();
                String expectedValue = expectedRow.get(j);
                String actualValue = actualRow.get(j);
                if (!isEqualCells(expectedValue, actualValue, dataType)) {
                    failNotEquals(errorMessage, expectedRows, actualRows);
                }
            }

        }

    }

    public static void assertTableRowsExist(String errorMessage, String[][] expectedRowsArray, StringTable table) {
        assertTableRowsExist(errorMessage, twoDArrayToList(expectedRowsArray), table);
    }


    public static void assertTableRowsExist(String[][] expectedRowsArray, StringTable table) {
        assertTableRowsExist(twoDArrayToList(expectedRowsArray), table);
    }

    public static void assertTableRowsExist(List<List<String>> expectedRows, StringTable table) {
        assertTableRowsExist("Table rows must be equals", expectedRows, table);
    }

    private static Boolean isEqualCells(String expectedValue, String actualValue, String dataType) {
        if (expectedValue == null) {
            return Objects.equals(actualValue, "null");
        } else {
            try {
                switch (dataType) {
                    case "number":
                    case "decimal":
                        BigDecimal actualBigDecimalValue = new BigDecimal(actualValue);
                        BigDecimal expectedDecimalValue = new BigDecimal(expectedValue);
                        return expectedDecimalValue.compareTo(actualBigDecimalValue) == 0;
                    case "double": {
                        Double actualDoubleValue = Double.parseDouble(actualValue);
                        Double expectedDoubleValue = Double.parseDouble(expectedValue);
                        return actualDoubleValue.equals(expectedDoubleValue);
                    }
                    case "float": {
                        Float actualFloatValue = Float.parseFloat(actualValue);
                        Float expectedFloatValue = Float.parseFloat(expectedValue);
                        return actualFloatValue.equals(expectedFloatValue);
                    }
                    case "java_object": {
                        try {
                            Float actualFloatValue = Float.parseFloat(actualValue);
                            Float expectedFloatValue = Float.parseFloat(expectedValue);
                            return actualFloatValue.equals(expectedFloatValue);
                        } catch (NumberFormatException ex) {
                            return expectedValue.equals(actualValue);
                        }
                    }
                    default: {
                        return expectedValue.equals(actualValue);
                    }
                }
            } catch (NumberFormatException nfe) {
                return expectedValue.equals(actualValue);
            }
        }

    }

    private static void failNotEquals(String message, Object expected, Object actual) {
        fail(format(message, expected, actual));
    }

    private static String format(String message, Object expected, Object actual) {
        return String.format("%s expected:<%s> but was:<%s>", message, expected, actual);
    }

    public void assertRowsMatch(String message, String[][] expectedRows, StringTable resultTable, String sqlQuery) {
        assertTableRowsExist(
                String.format("%s%nWrong result of the following SQL query: %n\t%s", message, sqlQuery),
                expectedRows, resultTable
        );
    }

    public void assertSqlQueryRows(String message, List<List<String>> expectedRows, String sqlQuery) {
        StringTable resultTable;
        try {
            resultTable = queryTable(sqlQuery);
            // Some series may be not returned immediately after insert.
            // If expected result is not empty, but actual is empty, wait 100ms and try again
            // See #5057
            if (expectedRows.size() > 0) {
                for (int timeout = 100; timeout <= 1600 && resultTable.getRows().size() == 0; timeout *= 2) {
                    try {
                        Thread.sleep(timeout);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    resultTable = queryTable(sqlQuery);
                }
            }
        } catch (Exception e) {
            if (StringUtils.isNotBlank(message)) {
                String error = String.format("%s%nTest in error", message);
                throw new RuntimeException(error, e);
            }
            throw e;
        }

        assertTableRowsExist(String.format("%s%nWrong result of the following SQL query: %n\t%s", message, sqlQuery), expectedRows,
                resultTable
        );
    }

    public void assertSqlQueryRows(String message, String[][] expectedRows, String sqlQuery) {
        assertSqlQueryRows(message, twoDArrayToList(expectedRows), sqlQuery);
    }

    public void assertSqlQueryRows(List<List<String>> expectedRows, String sqlQuery) {
        assertSqlQueryRows("", expectedRows, sqlQuery);
    }

    public void assertSqlQueryRows(String[][] expectedRows, String sqlQuery) {
        assertSqlQueryRows(twoDArrayToList(expectedRows), sqlQuery);
    }

    public void assertTableContainsColumnsValues(List<List<String>> values, StringTable table, String... columnNames) {
        assertEquals(String.format("Values of columns with names: %s are not equal to expected", columnNames), values, table.filterRows(columnNames));
    }

    public void assertTableContainsColumnValues(List<String> values, StringTable table, String columnName) {
        assertEquals(String.format("Values of column with name: %s are not equal to expected", columnName), values, table.columnValues(columnName));
    }

    public void assertTableColumnsNames(List<String> expectedColumnsNames, StringTable table) {
        assertTableColumnsNames(expectedColumnsNames, table, false);
    }

    public void assertTableColumnsLabels(List<String> expectedColumnsLabels, StringTable table) {
        assertTableColumnsLabels(expectedColumnsLabels, table, false);
    }

    public void assertTableColumnsLabels(List<String> expectedColumnsLabels, StringTable table, Boolean order) {
        List<String> columnsLabels = extractColumnLabels(table.getColumnsMetaData());

        if (order) {
            assertEquals(
                    "Table columns labels contain different elements or placed in different order",
                    expectedColumnsLabels,
                    columnsLabels);
        } else {
            assertEquals(
                    "Table columns labels contain different elements",
                    new HashSet<>(expectedColumnsLabels),
                    new HashSet<>(columnsLabels));
        }
    }

    public void assertTableColumnsNames(List<String> expectedColumnsNames, StringTable table, Boolean order) {
        List<String> columnsNames = extractColumnNames(table.getColumnsMetaData());

        if (order) {
            assertEquals(
                    "Table columns names contain different elements or placed in different order",
                    expectedColumnsNames,
                    columnsNames);
        } else {
            assertEquals("Table columns names contain different elements",
                    new HashSet<>(expectedColumnsNames),
                    new HashSet<>(columnsNames));

        }
    }

    public void assertOkRequest(Response response) {
        assertOkRequest(DEFAULT_ASSERT_OK_REQUEST_MESSAGE, response);
    }

    public void assertOkRequest(String assertMessage, String sqlQuery) {
        final String formattedMessage = String.format("%s%nQuery: %s", assertMessage, sqlQuery);
        assertOkRequest(formattedMessage, queryResponse(sqlQuery));
    }

    public void assertOkRequest(String assertMessage, Response response) {
        final Response.Status.Family family = Util.responseFamily(response);
        if (Response.Status.Family.SUCCESSFUL != family) {
            try {
                final String errorMessage = extractSqlErrorMessage(response);
                fail(String.format("%s%n Reason: %s", assertMessage, errorMessage));
            } catch (JSONException ex) {
                fail(assertMessage);
            }
        }
    }

    public void assertBadSqlRequest(String expectedMessage, String sqlQuery) {
        Response response = SqlMethod.queryResponse(sqlQuery);
        assertBadRequest(expectedMessage, response);
    }

    public void assertBadRequest(String assertionMessage, String expectedMessage, String sqlQuery) {
        assertBadRequest(assertionMessage, expectedMessage, queryResponse(sqlQuery));
    }

    public void assertBadRequestWithPattern(String assertionMessage, String expectedPattern, String sqlQuery) {
        assertBadRequest(assertionMessage, expectedPattern, queryResponse(sqlQuery), true);
    }

    public void assertBadRequest(String expectedMessage, Response response) {
        assertBadRequest(DEFAULT_ASSERT_BAD_REQUEST_MESSAGE, expectedMessage, response);
    }

    public void assertBadRequest(String assertMessage, String expectedMessage, Response response) {
        assertBadRequest(assertMessage, expectedMessage, response, false);
    }

    public void assertBadRequest(String assertMessage, String expectedMessage, Response response, boolean isPattern) {
        String responseMessage;
        int code = response.getStatus();
        if (Response.Status.Family.SUCCESSFUL == Util.responseFamily(response) || BAD_REQUEST.getStatusCode() == code) {
            try {
                responseMessage = extractSqlErrorMessage(response);
            } catch (JSONException e) {
                throw new IllegalArgumentException(assertMessage +
                        ": Can't check if there is error message, because JSON is invalid.");
            }
        } else {
            throw new IllegalArgumentException(assertMessage + ": Unexpected response status code");
        }
        if (responseMessage == null) {
            fail(assertMessage + ": Response doesn't contain error message");
        }
        if (!isPattern){
            assertEquals(assertMessage + ": Error message is different from expected", expectedMessage, responseMessage);
        } else {
            Pattern pattern = Pattern.compile(expectedMessage);
            assertTrue(assertMessage, pattern.matcher(responseMessage).matches());
        }
    }

    /**
     * Retrieve column names form table column metadata list
     *
     * @param columnMetaData array of column metadata values
     * @return column names list
     */
    private List<String> extractColumnNames(ColumnMetaData[] columnMetaData) {
        List<String> columnNames = new ArrayList<>();
        for (ColumnMetaData data : columnMetaData) {
            columnNames.add(data.getName());
        }
        return columnNames;
    }

    /**
     * Retrieve column labels form table column metadata list
     *
     * @param columnMetaData array of column metadata values
     * @return column labels list
     */
    private List<String> extractColumnLabels(ColumnMetaData[] columnMetaData) {
        List<String> columnNames = new ArrayList<>();
        for (ColumnMetaData data : columnMetaData) {
            columnNames.add(data.getTitles());
        }
        return columnNames;
    }

    private String extractSqlErrorMessage(Response response) throws JSONException {
        String jsonText = response.readEntity(String.class);
        JSONObject json = new JSONObject(jsonText);
        try {
            return json.getJSONArray("errors")
                    .getJSONObject(0)
                    .getString("message");
        } catch (JSONException e) {
            try { //if response body is in different format
                return json.getString("error");
            } catch (JSONException e2) {
                return null;
            }
        }
    }

    @RequiredArgsConstructor
    public static class SqlTestConfig<T extends SqlTestConfig> {
        @Getter
        private final String description;
        private Map<String, String> queryVariables = new HashMap<>();
        @Getter
        private List<List<String>> expected = new ArrayList<>();

        public void setVariable(String key, String value) {
            queryVariables.put(key, value);
        }

        public String composeQuery(String template) {
            for (Map.Entry<String, String> entry : queryVariables.entrySet()) {
                template = template.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            return template;
        }

        public T addExpected(String... row) {
            expected.add(Arrays.asList(row));
            return (T) this;
        }

        @Override
        public String toString() {
            return " " + description + " ";
        }
    }
}
