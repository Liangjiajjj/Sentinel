package com.alibaba.csp.sentinel.dashboard.domain;

import java.util.Map;

public class HttpResponse {
    private int httpReturnCode;
    private String content;
    private Map<String, String> headers;

    public HttpResponse(int httpReturnCode, String content) {
        this.httpReturnCode = httpReturnCode;
        this.content = content;
    }

    public int getHttpReturnCode() {
        return this.httpReturnCode;
    }

    public String getContent() {
        return this.content;
    }

    public boolean isSuccess() {
        return this.httpReturnCode == 200;
    }

    public Map<String, String> getHeaders() {
        return this.headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getHeader(String key) {
        return this.headers == null ? "null" : (String)this.headers.get(key);
    }

    public String toString() {
        return "OKHttpResponse [httpReturnCode=" + this.httpReturnCode + ", content=" + this.content + "]";
    }
}
