package com.axibase.tsd.api.method.entitygroup;

import com.axibase.tsd.api.model.entitygroup.EntityGroup;
import com.axibase.tsd.api.util.Util;
import io.qameta.allure.Issue;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;

import static org.testng.AssertJUnit.assertSame;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author Dmitry Korchagin.
 */
public class EntityGroupCreateOrReplaceTest extends EntityGroupMethod {

    @Issue("1278")
    @Test
    public void testNameContainsWhitespace() throws Exception {
        EntityGroup entityGroup = new EntityGroup("urlencodecreateReplace entitygroup1");
        assertUrlEncodePathHandledCorrectly(entityGroup);

    }

    @Issue("1278")
    @Test
    public void testNameContainsSlash() throws Exception {
        EntityGroup entityGroup = new EntityGroup("urlencodecreateReplace/entitygroup2");
        assertUrlEncodePathHandledCorrectly(entityGroup);

    }

    @Issue("1278")
    @Test
    public void testNameContainsCyrillic() throws Exception {
        EntityGroup entityGroup = new EntityGroup("urlencodecreateReplaceйёentitygroup3");
        assertUrlEncodePathHandledCorrectly(entityGroup);

    }

    public void assertUrlEncodePathHandledCorrectly(final EntityGroup entityGroup) throws Exception {
        Response response = createOrReplaceEntityGroup(entityGroup);
        assertSame("Fail to execute createOrReplaceEntityGroup query", Response.Status.Family.SUCCESSFUL, Util.responseFamily(response));
        assertTrue("Entity not found", entityGroupExist(entityGroup));
    }
}
