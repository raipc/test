package com.axibase.tsd.api.method.message;

import com.axibase.tsd.api.model.Period;
import com.axibase.tsd.api.model.TimeUnit;
import com.axibase.tsd.api.model.message.Message;
import com.axibase.tsd.api.model.message.MessageQuery;
import com.axibase.tsd.api.util.ResponseAsList;
import com.axibase.tsd.api.util.Util;
import io.qameta.allure.Issue;
import org.skyscreamer.jsonassert.JSONAssert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.util.*;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

public class MessageQueryTest extends MessageMethod {
    private static final Message message;

    static {
        message = new Message("message-query-test-timezone");
        message.setMessage("hello");
        message.setDate("2105-05-21T00:00:00.000Z");
    }

    @BeforeMethod
    public void prepare() throws Exception {
        insertMessageCheck(message);
    }


    @Issue("2850")
    @Test
    public void testISOTimezoneZ() throws Exception {
        MessageQuery messageQuery = buildMessageQuery();
        messageQuery.setStartDate("2105-05-21T00:00:00Z");

        List<Message> storedMessageList = queryMessageResponse(messageQuery).readEntity(ResponseAsList.ofMessages());
        Message storedMessage = storedMessageList.get(0);

        assertEquals("Incorrect message entity", message.getEntity(), storedMessage.getEntity());
        assertEquals("Incorrect message text", message.getMessage(), storedMessage.getMessage());
        assertEquals("Incorrect message date", message.getDate(), storedMessage.getDate());
    }

    @Issue("2850")
    @Test
    public void testISOTimezonePlusHourMinute() throws Exception {
        MessageQuery messageQuery = buildMessageQuery();
        messageQuery.setStartDate("2105-05-21T01:23:00+01:23");

        List<Message> storedMessageList = queryMessageResponse(messageQuery).readEntity(ResponseAsList.ofMessages());
        Message storedMessage = storedMessageList.get(0);

        assertEquals("Incorrect message entity", message.getEntity(), storedMessage.getEntity());
        assertEquals("Incorrect message text", message.getMessage(), storedMessage.getMessage());
        assertEquals("Incorrect message date", message.getDate(), storedMessage.getDate());
    }

    @Issue("2850")
    @Test
    public void testISOTimezoneMinusHourMinute() throws Exception {
        MessageQuery messageQuery = buildMessageQuery();
        messageQuery.setStartDate("2105-05-20T22:37:00-01:23");

        List<Message> storedMessageList = queryMessageResponse(messageQuery).readEntity(ResponseAsList.ofMessages());
        Message storedMessage = storedMessageList.get(0);

        assertEquals("Incorrect message entity", message.getEntity(), storedMessage.getEntity());
        assertEquals("Incorrect message text", message.getMessage(), storedMessage.getMessage());
        assertEquals("Incorrect message date", message.getDate(), storedMessage.getDate());
    }

    @Issue("2850")
    @Test
    public void testLocalTimeUnsupported() throws Exception {
        final String startDate = "2105-07-21 00:00:00";
        MessageQuery messageQuery = buildMessageQuery().setStartDate(startDate);

        Response response = queryMessageResponse(messageQuery);

        assertEquals("Incorrect response status code", BAD_REQUEST.getStatusCode(), response.getStatus());
        JSONAssert.assertEquals("{\"error\":\"IllegalArgumentException: Wrong startDate syntax: " + startDate + "\"}", response.readEntity(String.class), true);

    }

    @Issue("2850")
    @Issue("5272")
    @Test
    public void testRfc822TimezoneOffsetSupported() throws Exception {
        MessageQuery messageQuery = buildMessageQuery();
        messageQuery.setStartDate("2105-05-20T22:50:00-0110");

        Message storedMessage = queryMessageResponse(messageQuery)
                .readEntity(ResponseAsList.ofMessages())
                .get(0);

        assertEquals("Incorrect message entity", message.getEntity(), storedMessage.getEntity());
        assertEquals("Incorrect message text", message.getMessage(), storedMessage.getMessage());
        assertEquals("Incorrect message date", message.getDate(), storedMessage.getDate());
    }

    @Issue("2850")
    @Test
    public void testMillisecondsUnsupported() throws Exception {
        MessageQuery messageQuery = buildMessageQuery();
        messageQuery.setStartDate("1500595200000");

        Response response = queryMessageResponse(messageQuery);

        assertEquals("Incorrect response status code", BAD_REQUEST.getStatusCode(), response.getStatus());
        JSONAssert.assertEquals("{\"error\":\"IllegalArgumentException: Wrong startDate syntax: 1500595200000\"}", response.readEntity(String.class), true);
    }

    @Issue("2979")
    @Test
    public void testEntitiesWildcardStarChar() throws Exception {
        Message message = new Message("message-query-wildcard-2-1");
        message.setMessage("msgtext");
        message.setDate("2105-01-01T00:00:00.000Z");
        insertMessageCheck(message);

        Map<String, Object> query = new HashMap<>();
        query.put("entities", Arrays.asList("message-query-wildcard-2*"));
        query.put("startDate", message.getDate());
        query.put("endDate", Util.addOneMS(message.getDate()));

        final String given = queryMessageResponse(query).readEntity(String.class);
        final String expected = jacksonMapper.writeValueAsString(Arrays.asList(message));
        assertTrue("Message in response does not match to inserted", compareJsonString(expected, given));
    }

    @Issue("2979")
    @Test
    public void testEntitiesWildcardQuestionChar() throws Exception {
        Message message = new Message("message-query-wildcard-3-1");
        message.setMessage("msgtext");
        message.setDate("2105-01-01T00:00:00.000Z");
        insertMessageCheck(message);

        Map<String, Object> query = new HashMap<>();
        query.put("entities", Arrays.asList("message-query-wildcard-3-?"));
        query.put("startDate", message.getDate());
        query.put("endDate", Util.addOneMS(message.getDate()));

        final String given = queryMessageResponse(query).readEntity(String.class);
        final String expected = jacksonMapper.writeValueAsString(Arrays.asList(message));
        assertTrue("Message in response does not match to inserted", compareJsonString(expected, given));
    }

    @Issue("2979")
    @Test
    public void testEntityEntitiesWildcardSame() throws Exception {
        Message message = new Message("message-query-wildcard-4-1");
        message.setMessage("msgtext");
        message.setDate("2105-01-01T00:00:00.000Z");
        insertMessageCheck(message);
        message.setEntity("message-query-wildcard-4-2");
        insertMessageCheck(message);
        message.setEntity("message-query-wildcard-4-3");
        insertMessageCheck(message);

        final String pattern = "message-query-wildcard-4*";

        Map<String, Object> query = new HashMap<>();
        query.put("entity", pattern);
        query.put("startDate", message.getDate());
        query.put("endDate", Util.addOneMS(message.getDate()));

        final String entitiesResponse = queryMessageResponse(query).readEntity(String.class);

        query.remove("entity");
        query.put("entities", Collections.singletonList(pattern));

        final String entityResponse = queryMessageResponse(query).readEntity(String.class);
        assertEquals("Message in response does not match to inserted", entitiesResponse, entityResponse);
    }

    private MessageQuery buildMessageQuery() {
        MessageQuery messageQuery = new MessageQuery();
        messageQuery.setEntity(message.getEntity());
        messageQuery.setInterval(new Period(1, TimeUnit.MILLISECOND));
        return messageQuery;
    }
}
