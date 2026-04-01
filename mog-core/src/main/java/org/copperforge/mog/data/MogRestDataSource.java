package org.copperforge.mog.data;

import java.io.IOException;
import java.time.Duration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.copperforge.mog.MogException;
import org.copperforge.mog.data.filter.MogJsonFilter;
import org.copperforge.mog.http.MogHttpMethod;
import org.copperforge.mog.http.MogTrustManager;
import org.copperforge.mog.runtime.MogContext;
import org.copperforge.mog.var.MogVariableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class MogRestDataSource extends MogJsonDataSource {

    private final Logger log = LoggerFactory.getLogger(MogRestDataSource.class);
    private final ObjectMapper mapper = new ObjectMapper();

    private String url;

    private String token;

    private Boolean insecureTls;

    private String connectTimeoutMs;

    private String requestTimeoutMs;

    @Override
    protected String json(MogJsonFilter filter, MogContext context) throws MogException {
        try {
            MogVariableService vars = new MogVariableService(context);
            HttpClient client = createClient(vars, Boolean.TRUE.equals(getInsecureTls()));
            HttpRequest request = createRequest(vars, filter);

            log.debug("HttpRequest = " + request);
            HttpResponse<String> resp = client.send(request, BodyHandlers.ofString());
            log.debug("HttpResponse = " + resp);

            int statusCode = resp.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new MogException("REST datasource '" + getName() + "' request failed with HTTP " + statusCode
                        + " for " + request.method() + " " + request.uri());
            }

            return resp.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MogException(e);
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException | IllegalArgumentException e) {
            throw new MogException(e);
        }
    }

    protected HttpClient createClient(MogVariableService vars, boolean insecureTls)
            throws NoSuchAlgorithmException, KeyManagementException, MogException {
        HttpClient.Builder builder = HttpClient.newBuilder();
        Duration connectTimeout = resolveTimeout(vars, getConnectTimeoutMs(), "connectTimeoutMs");
        if (connectTimeout != null) {
            builder.connectTimeout(connectTimeout);
        }
        if (insecureTls) {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[] { new MogTrustManager() }, new SecureRandom());
            builder.sslContext(sslContext);
        }
        return builder.build();
    }

    protected HttpRequest createRequest(MogVariableService vars, MogJsonFilter filter) throws MogException {
        String resolvedUrl = vars.envsubst(getUrl());
        String resolvedSuburl = vars.envsubst(filter != null ? filter.getSuburl() : null);
        URI uri = URI.create((resolvedUrl != null ? resolvedUrl : "") + (resolvedSuburl != null ? resolvedSuburl : ""));

        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(uri);
        Duration requestTimeout = resolveTimeout(vars, getRequestTimeoutMs(), "requestTimeoutMs");
        if (requestTimeout != null) {
            builder.timeout(requestTimeout);
        }
        String resolvedToken = vars.envsubst(getToken());
        if (resolvedToken != null && !resolvedToken.isBlank()) {
            builder.header("Authorization", "Bearer " + resolvedToken);
        }

        MogHttpMethod method = resolveMethod(filter != null ? filter.getMethod() : null);
        return builder.method(method.name(), BodyPublishers.noBody()).build();
    }

    protected MogHttpMethod resolveMethod(String rawMethod) {
        if (rawMethod == null || rawMethod.isBlank()) {
            return MogHttpMethod.GET;
        }
        try {
            return MogHttpMethod.valueOf(rawMethod.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported HTTP method '" + rawMethod + "'");
        }
    }

    protected Duration resolveTimeout(MogVariableService vars, String rawValue, String fieldName) throws MogException {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        String resolved = vars.envsubst(rawValue);
        if (resolved == null || resolved.isBlank()) {
            return null;
        }
        try {
            long millis = Long.parseLong(resolved.trim());
            if (millis <= 0) {
                throw new MogException("REST datasource '" + getName() + "' field '" + fieldName
                        + "' must be greater than 0");
            }
            return Duration.ofMillis(millis);
        } catch (NumberFormatException ex) {
            throw new MogException("REST datasource '" + getName() + "' field '" + fieldName
                    + "' must be a whole number of milliseconds");
        }
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Boolean getInsecureTls() {
        return insecureTls;
    }

    public void setInsecureTls(Boolean insecureTls) {
        this.insecureTls = insecureTls;
    }

    public String getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(String connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public String getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public void setRequestTimeoutMs(String requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
    }

    @Override
    public String toString() {
        return "MogRestDataSource [mapper=" + mapper + ", url=" + url + ", token=" + token
                + ", insecureTls=" + insecureTls + ", connectTimeoutMs=" + connectTimeoutMs
                + ", requestTimeoutMs=" + requestTimeoutMs + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((connectTimeoutMs == null) ? 0 : connectTimeoutMs.hashCode());
        result = prime * result + ((mapper == null) ? 0 : mapper.hashCode());
        result = prime * result + ((requestTimeoutMs == null) ? 0 : requestTimeoutMs.hashCode());
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        result = prime * result + ((token == null) ? 0 : token.hashCode());
        result = prime * result + ((insecureTls == null) ? 0 : insecureTls.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MogRestDataSource other = (MogRestDataSource) obj;
        if (mapper == null) {
            if (other.mapper != null)
                return false;
        } else if (!mapper.equals(other.mapper))
            return false;
        if (connectTimeoutMs == null) {
            if (other.connectTimeoutMs != null)
                return false;
        } else if (!connectTimeoutMs.equals(other.connectTimeoutMs))
            return false;
        if (requestTimeoutMs == null) {
            if (other.requestTimeoutMs != null)
                return false;
        } else if (!requestTimeoutMs.equals(other.requestTimeoutMs))
            return false;
        if (url == null) {
            if (other.url != null)
                return false;
        } else if (!url.equals(other.url))
            return false;
        if (token == null) {
            if (other.token != null)
                return false;
        } else if (!token.equals(other.token))
            return false;
        if (insecureTls == null) {
            if (other.insecureTls != null)
                return false;
        } else if (!insecureTls.equals(other.insecureTls))
            return false;
        return true;
    }

}
