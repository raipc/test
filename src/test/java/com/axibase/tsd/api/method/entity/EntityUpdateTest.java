package com.axibase.tsd.api.method.entity;

import com.axibase.tsd.api.model.entity.Entity;
import com.axibase.tsd.api.util.Util;
import io.qameta.allure.Issue;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.testng.AssertJUnit.*;

public class EntityUpdateTest extends EntityMethod {


    @Issue("1278")
    @Test
    public void testEntityNameContainsWhitespace() {
        Entity entity = new Entity("updateentity 1");
        assertEquals("Method should fail if entityName contains whitespace", BAD_REQUEST.getStatusCode(), updateEntity(entity).getStatus());
    }

    @Issue("1278")
    @Test
    public void testEntityNameContainsSlash() throws Exception {
        final Entity entity = new Entity("update_entity/2");
        entity.addTag("t1", "tv1");
        createOrReplaceEntityCheck(entity);

        Map<String, String> newTags = new HashMap<>();
        newTags.put("t2", "tv2");
        assertUrlencodedPathHandledSuccessfullyOnUpdate(entity, newTags);
    }

    @Issue("1278")
    @Test
    public void testEntityNameContainsCyrillic() throws Exception {
        Entity entity = new Entity("update_йёentity3");
        entity.addTag("t1", "tv1");
        createOrReplaceEntityCheck(entity);

        Map<String, String> newTags = new HashMap<>();
        newTags.put("t2", "tv2");
        assertUrlencodedPathHandledSuccessfullyOnUpdate(entity, newTags);
    }

    private void assertUrlencodedPathHandledSuccessfullyOnUpdate(final Entity entity, Map newTags) throws Exception {
        entity.setTags(newTags);
        assertSame("Fail to execute updateEntity", Response.Status.Family.SUCCESSFUL, Util.responseFamily(updateEntity(entity)));
        assertTrue("Entity in response does not match to updated entity", entityExist(entity));
    }
}
