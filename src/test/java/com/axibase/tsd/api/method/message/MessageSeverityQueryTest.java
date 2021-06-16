package com.axibase.tsd.api.method.message;

import com.axibase.tsd.api.model.message.Message;
import com.axibase.tsd.api.model.message.MessageQuery;
import com.axibase.tsd.api.model.message.Severity;
import com.axibase.tsd.api.model.message.SeverityAlias;
import com.axibase.tsd.api.util.ResponseAsList;
import com.axibase.tsd.api.util.Util;
import io.qameta.allure.Issue;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.axibase.tsd.api.model.message.Severity.*;
import static com.axibase.tsd.api.util.Util.MAX_QUERYABLE_DATE;
import static com.axibase.tsd.api.util.Util.MIN_QUERYABLE_DATE;
import static org.testng.AssertJUnit.*;


public class MessageSeverityQueryTest extends MessageMethod {
    private Message message;
    private MessageQuery messageQuery;
    private Calendar calendar = Calendar.getInstance();

    @BeforeClass
    public void insertMessages() throws Exception {
        message = new Message("message-query-test-severity");
        message.setMessage("message-text");
        calendar.setTime(new Date());
        calendar.add(Calendar.YEAR, -1);
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        message.setDate(calendar.getTime());
        for (Severity severity : values()) {
            message.setSeverity(severity.name());
            insertMessageCheck(message);
            message.setDate(Util.addOneMS(message.getDate()));
        }
    }

    @BeforeMethod
    public void prepareQuery() {
        messageQuery = new MessageQuery();
        messageQuery.setEntity(message.getEntity());
        messageQuery.setStartDate(MIN_QUERYABLE_DATE);
        messageQuery.setEndDate(MAX_QUERYABLE_DATE);

    }

    @Issue("2917")
    @Test(
            dataProvider = "unknownSeverities",
            description = "unknown severity name or code raise error"
    )
    public void testUnknownSeverityRaiseError(Object o) throws Exception {
        messageQuery.setSeverity(String.valueOf(o));
        String response = queryMessageResponse(messageQuery).readEntity(String.class);
        JSONObject error = new JSONObject(response);
        assertTrue("Error if not raised", error.has("error"));
    }

    @Issue("2917")
    @Test(
            dataProvider = "aliases",
            description = "alias processed correctly"
    )
    public void testAliasProcessedCorrectly(SeverityAlias alias) throws Exception {
        messageQuery.setSeverity(alias.name());
        List<Message> messages = queryMessageResponse(messageQuery).readEntity(ResponseAsList.ofMessages());
        String severity = messages.get(0).getSeverity();
        assertEquals("Alias processed wrong", alias.getSeverity().name(), severity);
    }

    @Issue("2917")
    @Test(
            dataProvider = "severities",
            description = "minSeverity is case insensitive"
    )
    public void testMinSeverityCaseInsensitive(Severity severity) throws Exception {
        messageQuery.setMinSeverity(properCase(severity.name()));
        List<Message> messages = queryMessageResponse(messageQuery).readEntity(ResponseAsList.ofMessages());
        Integer minimumSeverity = severity.getNumVal();
        for (Message m : messages) {
            int actualSeverity = valueOf(m.getSeverity()).getNumVal();
            String errMessage = String.format("Received severity (%d) should be greater than minSeverity (%d)",
                    actualSeverity, minimumSeverity);
            assertTrue(errMessage, actualSeverity >= minimumSeverity);
        }
    }

    @Issue("2917")
    @Test(
            dataProvider = "severities",
            description = "severity is case insensitive"
    )
    public void testSeverityCaseInsensitive(Severity s) throws Exception {
        messageQuery.setSeverity(properCase(s.name()));
        List<Message> messages = queryMessageResponse(messageQuery).readEntity(ResponseAsList.ofMessages());
        String severity = messages.get(0).getSeverity();
        assertEquals("Severity is case sensitive", s.name(), severity);
    }

    @Issue("2917")
    @Test(
            dataProvider = "severities",
            description = "response contains severity as name (text) not as numeric code"
    )
    public void testResponseSeverityNotNumeric(Severity s) throws Exception {
        messageQuery.setSeverity(s.name());
        List<Message> messages = queryMessageResponse(messageQuery).readEntity(ResponseAsList.ofMessages());
        String severity = messages.get(0).getSeverity();
//            str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
        String errMessage = String.format("Received severity (%s) should not be numeric", severity);
        assertFalse(errMessage, severity.matches("-?\\d+(\\.\\d+)?"));
    }

    @Issue("2917")
    @Test(
            dataProvider = "severities",
            description = "minSeverity is >= filter"
    )
    public void testMinSeverityFilter(Severity severity) throws Exception {
        String key = severity.name();
        Integer minimumSeverity = severity.getNumVal();
        messageQuery.setMinSeverity(key);
        List<Message> messages = queryMessageResponse(messageQuery).readEntity(ResponseAsList.ofMessages());
        for (Message m : messages) {
            int actualSeverity = valueOf(m.getSeverity()).getNumVal();
            String errMessage = String.format("Received severity (%d) should be greater than minSeverity (%d)",
                    actualSeverity, minimumSeverity);
            assertTrue(errMessage, actualSeverity >= minimumSeverity);
        }
    }

    @Issue("2917")
    @Test(description = "severities should return messages with the same severities names as in the request")
    public void testActualSeveritiesCorrespondRequired() throws Exception {
        String[] allSeverities = names();
        messageQuery.setSeverities(Arrays.asList(allSeverities));
        List<Message> messages = queryMessageResponse(messageQuery).readEntity(ResponseAsList.ofMessages());
        assertEquals(allSeverities.length, messages.size());
    }

    @DataProvider(name = "severities")
    public Object[][] provideSeverities() {
        return new Object[][]{
                {UNDEFINED},
                {UNKNOWN},
                {NORMAL},
                {WARNING},
                {MINOR},
                {MAJOR},
                {CRITICAL},
                {FATAL}
        };
    }

    @DataProvider(name = "aliases")
    public Object[][] provideAliases() {
        return new Object[][]{
                {SeverityAlias.ERROR},
                {SeverityAlias.INFO},
                {SeverityAlias.WARN}
        };
    }

    @DataProvider(name = "unknownSeverities")
    public Object[][] provideUnknownSeverities() {
        return new Object[][]{
                {"HELLO"},
                {32}
        };
    }

    private String properCase(String inputVal) {
        if (inputVal.length() == 0) return "";
        if (inputVal.length() == 1) return inputVal.toUpperCase();
        return inputVal.substring(0, 1).toUpperCase()
                + inputVal.substring(1).toLowerCase();
    }
}
