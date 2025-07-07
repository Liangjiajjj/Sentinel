package com.alibaba.csp.sentinel.dashboard.client;


import com.alibaba.csp.sentinel.dashboard.domain.DorisResponse;
import com.alibaba.csp.sentinel.dashboard.domain.HttpResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author liangjiajun
 */
public class ApacheHttpStreamLoadClient {

    protected static final Logger LOGGER = LoggerFactory.getLogger(ApacheHttpStreamLoadClient.class);

    private static final int MAX_TOTAL;

    private static final int DEFAULT_MAX_PER_ROUTE;

    private static final String SUCCESS = "success";

    static {
        MAX_TOTAL = Math.max(1, 100);
        DEFAULT_MAX_PER_ROUTE = Math.max(1, 100);
    }

    private final String auth;

    private final String dorisHttpUrl;

    private static final String DORIS_URI = "/api/{db}/{table}/_stream_load";

    private static final MediaType JSON = MediaType.parseMediaType("application/json; charset=utf-8");

    private final ObjectMapper objectMapper;

    private final CloseableHttpClient client;

    public ApacheHttpStreamLoadClient(String dorisHttpUrl, String auth, int connectTimeOutSeconds, int writeTimeOutSeconds,
                                      int readTimeOutSeconds, int maxIdle, int idleKeepaliveMin, ObjectMapper objectMapper) {
        this.auth = auth;
        this.dorisHttpUrl = dorisHttpUrl;
        this.objectMapper = objectMapper;
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(MAX_TOTAL);
        connectionManager.setDefaultMaxPerRoute(DEFAULT_MAX_PER_ROUTE);
        this.client = HttpClients
                .custom()
                .setConnectionManager(connectionManager)
                .setConnectionTimeToLive(idleKeepaliveMin, TimeUnit.SECONDS)
                .setRedirectStrategy(new DefaultRedirectStrategy() {
                    @Override
                    protected boolean isRedirectable(String method) {
                        return true;
                    }
                })
                .build();
    }

    public DorisResponse load(String db, String tableName, Object data) {
        return load(db, auth, tableName, data);
    }

    public DorisResponse load(String db, String auth, String tableName, Object data) {
        HashMap<String, String> headers = new HashMap<>(6);
        headers.put("Authorization", "Basic " + auth);
        headers.put("format", "json");
        if (data instanceof List) {
            headers.put("strip_outer_array", "true");
        }
        headers.put("read_json_by_line", "false");
        headers.put("Content-Type", "application/json");
        headers.put("connection", "keep-alive");
        headers.put("Expect", "100-continue");
        String url = dorisHttpUrl + DORIS_URI.replace("{db}", db).replace("{table}", tableName);
        String requestContent = null;
        try {
            requestContent = objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        HttpResponse response = put(url, requestContent, headers, new HashMap<>(0));
        Integer httpReturnCode = null;
        if (Objects.nonNull(response)) {
            httpReturnCode = response.getHttpReturnCode();
        }
        String content = null;
        if (Objects.nonNull(httpReturnCode)) {
            content = response.getContent();
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("doris batch insert, url: {}, request: {}, code: {}, response: {}", url, requestContent, httpReturnCode, content);
        }
        if (Objects.nonNull(httpReturnCode) && httpReturnCode == HttpStatus.SC_OK) {
            DorisResponse dorisResponse = null;
            try {
                dorisResponse = objectMapper.readValue(content, DorisResponse.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            if (!SUCCESS.equalsIgnoreCase(dorisResponse.getStatus())) {
                LOGGER.error("doris batch insert fail, url: {}, request: {}, code: {}, response: {}", url, requestContent, httpReturnCode, content);
                throw new RuntimeException("doris response failed, content: " + content);
            } else {
                return dorisResponse;
            }
        } else {
            LOGGER.error("doris batch insert fail, url: {}, request: {}, code: {}, response: {}", url, requestContent, httpReturnCode, content);
            throw new RuntimeException("doris response timeout or failed, http status: " + httpReturnCode);
        }
    }

    private HttpResponse put(String url, String data, Map<String, String> headers,
                             Map<String, String> querys) {
        HttpResponse httpResponse = null;
        try {
            HttpPut put = new HttpPut(url);
            StringEntity entity = new StringEntity(data, "UTF-8");
            // 构造头部
            if (headers != null && !headers.isEmpty()) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    put.setHeader(header.getKey(), header.getValue());
                }
            }
            put.setEntity(entity);
            CloseableHttpResponse response = client.execute(put);
            String content = "";
            if (response.getEntity() != null) {
                content = EntityUtils.toString(response.getEntity());
            }
            final int statusCode = response.getStatusLine().getStatusCode();
            response.getEntity().getContent();
            if (statusCode != 200) {
                throw new IOException(
                        String.format("Stream load failed, statusCode=%s load result=%s", statusCode, content));
            }
            httpResponse = new HttpResponse(statusCode, content);
            httpResponse.setHeaders(getHeaders(response));
        } catch (Throwable e) {
            LOGGER.error("http client execute failed.", e);
            httpResponse = new HttpResponse(503, "server response null");
        } finally {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("============ Apache Http Put ================");
                LOGGER.trace("url {} ", url);
                LOGGER.trace("requestHeader {}", headers);
                LOGGER.trace("requestParams {}", querys);
                LOGGER.trace("requestBody {}", data);
                if (Objects.nonNull(httpResponse)) {
                    LOGGER.trace("responseCode {} responseBody {}", httpResponse.getHttpReturnCode(),
                            httpResponse.getContent());
                }
                LOGGER.trace("=========================================");
            }
        }
        return httpResponse;
    }

    private Map<String, String> getHeaders(CloseableHttpResponse response) {
        Map<String, String> headers = new HashMap<>();
        for (Header header : response.getAllHeaders()) {
            headers.put(header.getName(), header.getValue());
        }
        return headers;
    }

}

