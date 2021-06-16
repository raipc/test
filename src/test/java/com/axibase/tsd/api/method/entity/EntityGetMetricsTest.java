package com.axibase.tsd.api.method.entity;

import com.axibase.tsd.api.method.series.SeriesMethod;
import com.axibase.tsd.api.model.metric.Metric;
import com.axibase.tsd.api.model.series.Sample;
import com.axibase.tsd.api.model.series.Series;
import com.axibase.tsd.api.util.ResponseAsList;
import com.axibase.tsd.api.util.Util;
import io.qameta.allure.Issue;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertSame;

public class EntityGetMetricsTest extends EntityMethod {


    @Issue("1278")
    @Test
    public void testEntityNameContainsWhitespace() {
        final String name = "getmetricsentity 1";
        assertEquals("Method should fail if entityName contains whitespace", BAD_REQUEST.getStatusCode(), queryEntityMetrics(name).getStatus());
    }


    @Issue("1278")
    @Test
    public void testEntityNameContainsSlash() throws Exception {
        final Series series = new Series("getmetrics/entity2", "getmetrics-metric2");
        series.addSamples(Sample.ofDateInteger("1970-01-01T00:00:00.000Z", 1));
        SeriesMethod.insertSeriesCheck(Collections.singletonList(series));

        assertUrlencodedPathHandledSuccessfullyOnGetMetrics(series);
    }

    @Issue("1278")
    @Test
    public void testEntityNameContainsCyrillic() throws Exception {
        final Series series = new Series("getmetricsйё/entity3", "getmetrics-metric3");
        series.addSamples(Sample.ofDateInteger("1970-01-01T00:00:00.000Z", 1));
        SeriesMethod.insertSeriesCheck(Collections.singletonList(series));

        assertUrlencodedPathHandledSuccessfullyOnGetMetrics(series);
    }

    private void assertUrlencodedPathHandledSuccessfullyOnGetMetrics(final Series series) {
        Response response = queryEntityMetrics(series.getEntity());
        assertSame("Fail to execute queryEntityMetric", Response.Status.Family.SUCCESSFUL, Util.responseFamily(response));
        List<Metric> metricList = response.readEntity(ResponseAsList.ofMetrics());
        assertEquals("Entity should have only 1 metric", 1, metricList.size());
        assertEquals("Metric in response does not match to inserted metric", series.getMetric(), metricList.get(0).getName());
    }
}
