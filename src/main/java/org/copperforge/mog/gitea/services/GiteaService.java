package org.copperforge.mog.gitea.services;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.copperforge.mog.MogBean;
import org.copperforge.mog.MogException;
import org.copperforge.mog.http.MogHttpRequest;
import org.copperforge.mog.http.MogHttpResponse;
import org.copperforge.mog.http.MogTokenAuthorization;
import org.copperforge.mog.http.MogTrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GiteaService {

    private final Logger log = LoggerFactory.getLogger(GiteaService.class);
    protected final String token = "cea8a5bde4a1e65b4619932ce30a730c285d5333"; // TODO
    protected final String baseUrl = "http://192.168.50.29/git/api/v1"; // TODO

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected MogHttpResponse<?> performRequest(MogHttpRequest request) throws MogException {
        try {

            // add token auth if no auth present
            if (!request.hasAuthorizationHeader()) {
                request.auth(MogTokenAuthorization.create(token));
            }

            // create client
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[] { new MogTrustManager() }, new SecureRandom());

            HttpClient client = HttpClient.newBuilder().sslContext(sslContext).build();

            // create request
            Builder requestBuilder = HttpRequest.newBuilder();

            for (String key : request.getHeaders().keySet()) {
                requestBuilder.header(key, request.getHeaders().get(key));
            }

            HttpRequest httpRequest = requestBuilder.uri(URI.create(baseUrl + request.getUrl()))
                    .build();

            log.debug("HttpRequest = " + request);
            MogHttpResponse<? extends MogBean> response = new MogHttpResponse(
                    client.send(httpRequest, BodyHandlers.ofString()),
                    request.getAccepts());
            log.debug("GiteaResponse = " + response);

            return response;
        } catch (IOException | KeyManagementException | NoSuchAlgorithmException | InterruptedException e) {
            log.error("Unable to perform HTTP request", e);
            throw new MogException(e);
        }
    }
}
