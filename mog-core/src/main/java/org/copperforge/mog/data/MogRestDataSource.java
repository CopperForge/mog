package org.copperforge.mog.data;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
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
import org.copperforge.mog.http.MogTrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class MogRestDataSource extends MogJsonDataSource {

    private final Logger log = LoggerFactory.getLogger(MogRestDataSource.class);
    private final ObjectMapper mapper = new ObjectMapper();

    private String url;

    private String token;

    @Override
    protected String json(MogJsonFilter filter) throws MogException {
        try {
            // create client
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[] { new MogTrustManager() }, new SecureRandom());

            HttpClient client = HttpClient.newBuilder().sslContext(sslContext).build();

            // create request
            HttpRequest request = HttpRequest.newBuilder()
                    .header("Authorization", "Bearer " + getToken())
                    .uri(URI.create(getUrl() + filter.getSuburl()))
                    .build();

            log.debug("HttpRequest = " + request);
            HttpResponse<?> resp = client.send(request, BodyHandlers.ofString());
            log.debug("HttpResponse = " + resp);

            return resp.body().toString();
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException | InterruptedException e) {
            throw new MogException(e);
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

}
