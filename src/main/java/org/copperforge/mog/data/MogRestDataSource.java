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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;

import org.copperforge.mog.data.filter.MogDataFilter;
import org.copperforge.mog.data.filter.MogJsonFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

public class MogRestDataSource extends MogDataSource {

    private final Logger log = LoggerFactory.getLogger(MogRestDataSource.class);
    private final ObjectMapper mapper = new ObjectMapper();

    private String url;

    private String token;

    @Override
    public List<MogFetchable> fetch(MogDataFilter filter) {
        try {
            MogJsonFilter jsonFilter = (MogJsonFilter) filter;

            // create client
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[] { mogTrustManager }, new SecureRandom());

            HttpClient client = HttpClient.newBuilder().sslContext(sslContext).build();

            // create request
            HttpRequest request = HttpRequest.newBuilder()
                    .header("Authorization", "Bearer " + getToken())
                    .uri(URI.create(getUrl() + jsonFilter.getSuburl()))
                    .build();

            log.debug("HttpRequest = " + request);
            HttpResponse<?> resp = client.send(request, BodyHandlers.ofString());
            log.debug("HttpResponse = " + resp);

            DocumentContext jsonContext = JsonPath.parse(resp.body().toString());

            JsonPath path = JsonPath.compile(jsonFilter.getJsonPath());
            if (path.isDefinite()) {
                Object value = jsonContext.read(path);
                log.debug("value = " + value.getClass().getCanonicalName());
                return Arrays.asList(new MogFetchable(value));
            } else {
                List<Map<String, Object>> values = jsonContext.read(path);
                log.debug("values = " + values);
                return values.stream().map(MogFetchable::new).collect(Collectors.toList());
            }
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

    @Override
    public String toString() {
        return "MogRestDataSource [mapper=" + mapper + ", url=" + url + ", token=" + token + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mapper == null) ? 0 : mapper.hashCode());
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        result = prime * result + ((token == null) ? 0 : token.hashCode());
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
