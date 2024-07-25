package org.copperforge.mog.data;

import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;

import org.copperforge.mog.data.filter.MogDataFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MogRestDataSource extends MogDataSource {

    private final Logger log = LoggerFactory.getLogger(MogRestDataSource.class);
    private final ObjectMapper mapper = new ObjectMapper();

    private String url;

    private String token;

    private String keyPath;

    private String method;

    @Override
    public List<MogFetchable> fetch(MogDataFilter filter) {
        try {
            // create client
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[] { mogTrustManager }, new SecureRandom());

            HttpClient client = HttpClient.newBuilder().sslContext(sslContext).build();

            // create request
            HttpRequest request = HttpRequest.newBuilder()
                    .header("Authorization", "Bearer " + getToken())
                    .uri(URI.create(getUrl()))
                    .build();

            log.debug("HttpRequest = " + request);
            HttpResponse<?> resp = client.send(request, BodyHandlers.ofString());
            log.debug("HttpResponse = " + resp);
            TypeReference<List<Map<String, Object>>> typeRef = new TypeReference<List<Map<String, Object>>>() {
            };
            String body = resp.body().toString();
            log.trace("body = " + body);
            List<Map<String, Object>> response = mapper.readValue(body, typeRef);
            // log.info("fetchable = " + response);

            return response.stream().map(MogFetchable::new).collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
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

    public String getKeyPath() {
        return keyPath;
    }

    public void setKeyPath(String keyPath) {
        this.keyPath = keyPath;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public String toString() {
        return "MogRestDataSource [url=" + url + ", token=" + token + ", keyPath=" + keyPath + ", method=" + method
                + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mapper == null) ? 0 : mapper.hashCode());
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        result = prime * result + ((token == null) ? 0 : token.hashCode());
        result = prime * result + ((keyPath == null) ? 0 : keyPath.hashCode());
        result = prime * result + ((method == null) ? 0 : method.hashCode());
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
        if (keyPath == null) {
            if (other.keyPath != null)
                return false;
        } else if (!keyPath.equals(other.keyPath))
            return false;
        if (method == null) {
            if (other.method != null)
                return false;
        } else if (!method.equals(other.method))
            return false;
        return true;
    }

    protected static final TrustManager mogTrustManager = new X509ExtendedTrustManager() {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[0];
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)
                throws CertificateException {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
                throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket)
                throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
                throws CertificateException {
        }

    };
}
