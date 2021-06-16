package com.axibase.tsd.api.method;

import com.axibase.tsd.api.Config;
import com.axibase.tsd.api.util.NotCheckedException;
import com.axibase.tsd.logging.LoggingFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.RequestEntityProcessing;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.text.SimpleDateFormat;
import java.util.function.Function;
import java.util.logging.Level;

public abstract class BaseMethod {
    public static final long REQUEST_INTERVAL = 200L;
    public static final long UPPER_BOUND_FOR_CHECK = 100000L;

    private static final int DEFAULT_BORROW_MAX_TIME_MS = 3000;
    private static final int DEFAULT_MAX_TOTAL = 8;
    private static final int DEFAULT_MAX_IDLE = 8;

    private static final GenericObjectPool<HttpClient> apiTargetPool;
    private static final GenericObjectPool<HttpClient> rootTargetPool;
    private static final GenericObjectPool<HttpClient> tokenRootTargetPool;
    
    private static final int DEFAULT_CONNECT_TIMEOUT = 180000;
    private static final Logger logger = LoggerFactory.getLogger(BaseMethod.class);

    protected static final ObjectMapper jacksonMapper;

    static {
        java.util.logging.LogManager.getLogManager().reset();
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger("");
        julLogger.setLevel(Level.FINEST);

        Config config = Config.getInstance();
        ClientConfig clientConfig = new ClientConfig()
                .connectorProvider(new ApacheConnectorProvider())
                .register(MultiPartFeature.class)
                .register(HttpAuthenticationFeature.basic(config.getLogin(), config.getPassword()))
                .property(ClientProperties.READ_TIMEOUT, DEFAULT_CONNECT_TIMEOUT)
                .property(ClientProperties.CONNECT_TIMEOUT, DEFAULT_CONNECT_TIMEOUT)
                .property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.BUFFERED);
        ClientConfig tokenConfig = new ClientConfig();
        tokenConfig.connectorProvider(new ApacheConnectorProvider())
                .property(ClientProperties.READ_TIMEOUT, DEFAULT_CONNECT_TIMEOUT)
                .property(ClientProperties.CONNECT_TIMEOUT, DEFAULT_CONNECT_TIMEOUT)
                .property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.BUFFERED);

        GenericObjectPoolConfig<HttpClient> objectPoolConfig = new GenericObjectPoolConfig<>();
        objectPoolConfig.setMaxTotal(DEFAULT_MAX_TOTAL);
        objectPoolConfig.setMaxIdle(DEFAULT_MAX_IDLE);

        rootTargetPool = new GenericObjectPool<>(
                new HttpClientFactory(clientConfig, config, ""), objectPoolConfig);
        apiTargetPool = new GenericObjectPool<>(
                new HttpClientFactory(clientConfig, config, config.getApiPath()), objectPoolConfig);
        tokenRootTargetPool = new GenericObjectPool<>(
                new HttpClientFactory(tokenConfig, config, ""), objectPoolConfig);

        jacksonMapper = new ObjectMapper();
        jacksonMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));
    }

    protected static WebTarget addParameters(WebTarget target, MethodParameters parameters) {
        if (parameters != null) {
            target = parameters.appendTo(target);
        }
        return target;
    }

    public static ObjectMapper getJacksonMapper() {
        return jacksonMapper;
    }

    public static boolean compareJsonString(String expected, String given) {
        return compareJsonString(expected, given, false);
    }

    public static boolean compareJsonString(String expected, String given, boolean strict) {
        try {
            JSONAssert.assertEquals(expected, given, strict ? JSONCompareMode.NON_EXTENSIBLE : JSONCompareMode.LENIENT);
            return true;
        } catch (JSONException e) {
            logger.error("Can not deserialize response:\n{}", given);
            throw new IllegalStateException("Can not deserialize response");
        } catch (AssertionError e) {
            return false;
        }

    }

    public static int calculateJsonArraySize(String jsonArrayString) throws JSONException {
        return new JSONArray(jsonArrayString).length();
    }

    public static String extractErrorMessage(Response response) throws Exception {
        String jsonText = response.readEntity(String.class);

        JSONObject json;
        try {
            json = new JSONObject(jsonText);
        } catch (JSONException e) {
            throw new JSONException("Fail to parse response as JSON");
        }
        try {
            return json.getString("error");
        } catch (JSONException e) {
            throw new IllegalStateException("Fail to get error message from response. Perhaps response does not contain error message when it should.");
        }
    }

    public static String responseAsString(Response response) {
        try {
            return response.readEntity(String.class);
        } catch (Exception e) {
            logger.warn("Could not read response: {}", e.getMessage());
            return "";
        }
    }

    public static JsonNode responseAsTree(Response response) throws JsonProcessingException {
        return jacksonMapper.readTree(responseAsString(response));
    }

    public static Response executeRootRequest(Function<WebTarget, Response> requestFunction) {
        return executeRequest(rootTargetPool, requestFunction);
    }

    public static Response executeApiRequest(Function<WebTarget, Response> requestFunction) {
        return executeRequest(apiTargetPool, requestFunction);
    }

    public static Response executeTokenRootRequest(Function<WebTarget, Response> requestFunction) {
        return executeRequest(tokenRootTargetPool, requestFunction);
    }

    private static Response executeRequest(
            GenericObjectPool<HttpClient> pool,
            Function<WebTarget, Response> requestFunction) {
        HttpClient client;
        try {
            client = pool.borrowObject(DEFAULT_BORROW_MAX_TIME_MS);
        } catch (Exception e) {
            throw new NotCheckedException("Could not borrow HTTP client from pool: " + e.getMessage());
        }

        try {
            return requestFunction.apply(client.target);
        } finally {
            pool.returnObject(client);
        }
    }

    private static class HttpClient {
        private final Client client;
        private final WebTarget target;

        HttpClient(ClientConfig clientConfig, Config targetConfig, String targetUri) {
            client = ClientBuilder.newClient(clientConfig);
            client.register(new LoggingFilter());

            target = client.target(UriBuilder.fromPath(targetUri)
                    .scheme(targetConfig.getProtocol())
                    .host(targetConfig.getServerName())
                    .port(targetConfig.getHttpPort())
                    .build());
        }

        public WebTarget getTarget() {
            return target;
        }

        public void close() {
            client.close();
        }
    }

    private static class HttpClientFactory extends BasePooledObjectFactory<HttpClient> {
        private final ClientConfig clientConfig;
        private final Config targetConfig;
        private final String targetUri;

        HttpClientFactory(ClientConfig clientConfig, Config targetConfig, String targetUri) {
            this.clientConfig = clientConfig;
            this.targetConfig = targetConfig;
            this.targetUri = targetUri;
        }

        @Override
        public HttpClient create() throws Exception {
            return new HttpClient(clientConfig, targetConfig, targetUri);
        }

        @Override
        public PooledObject<HttpClient> wrap(HttpClient client) {
            return new DefaultPooledObject<>(client);
        }

        @Override
        public void destroyObject(PooledObject<HttpClient> p) throws Exception {
            p.getObject().close();
        }
    }
}
