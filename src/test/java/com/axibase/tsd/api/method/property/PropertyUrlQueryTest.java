package com.axibase.tsd.api.method.property;


import com.axibase.tsd.api.model.property.Property;
import com.axibase.tsd.api.util.Util;
import io.qameta.allure.Issue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.lang.invoke.MethodHandles;
import java.util.Collections;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.testng.AssertJUnit.*;

public class PropertyUrlQueryTest extends PropertyMethod {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Issue("1278")
    @Test
    public void testEntityNameContainsWhilespace() {
        final Property property = new Property("urlquery-property-type-1", "urlquery entityname-1");
        assertEquals(BAD_REQUEST.getStatusCode(), urlQueryProperty(property.getType(), property.getEntity()).getStatus());
    }

    @Issue("1278")
    @Test
    public void testTypeContainsWhilespace() {
        Property property = new Property("urlquery-property type-2", "urlquery-entityname-2");
        assertEquals(BAD_REQUEST.getStatusCode(), urlQueryProperty(property.getType(), property.getEntity()).getStatus());
    }

    @Issue("1278")
    @Test
    public void testEntityNameContainsSlash() throws Exception {
        Property property = new Property("urlquery-property-type-3", "urlquery/entityname-3");
        assertUrlencodedPathHandledSuccessfullyOnUrlQuery(property);
    }

    @Issue("1278")
    @Test
    public void testTypeContainsSlash() throws Exception {
        Property property = new Property("urlquery-property/type-4", "urlquery-entityname-4");
        assertUrlencodedPathHandledSuccessfullyOnUrlQuery(property);

    }

    @Issue("1278")
    @Test
    public void testEntityNameContainsCyrillic() throws Exception {
        Property property = new Property("urlquery-property-type-5", "urlqueryйёentityname-5");
        assertUrlencodedPathHandledSuccessfullyOnUrlQuery(property);

    }

    @Issue("1278")
    @Test
    public void testTypeContainsCyrillic() throws Exception {
        Property property = new Property("urlquery-propertyйёtype-6", "urlquery-entityname-6");
        assertUrlencodedPathHandledSuccessfullyOnUrlQuery(property);

    }

    public void assertUrlencodedPathHandledSuccessfullyOnUrlQuery(final Property property) throws Exception {
        property.addTag("t1", "tv1");
        insertPropertyCheck(property);
        Response response = urlQueryProperty(property.getType(), property.getEntity());
        assertSame(Response.Status.Family.SUCCESSFUL, Util.responseFamily(response));
        String expected = jacksonMapper.writeValueAsString(Collections.singletonList(property));
        assertTrue(compareJsonString(expected, response.readEntity(String.class)));
    }


}
