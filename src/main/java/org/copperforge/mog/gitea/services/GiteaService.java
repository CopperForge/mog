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
import org.copperforge.mog.git.MogGitService;
import org.copperforge.mog.gitea.MogGitea;
import org.copperforge.mog.http.MogHttpRequest;
import org.copperforge.mog.http.MogHttpResponse;
import org.copperforge.mog.http.MogTokenAuthorization;
import org.copperforge.mog.http.MogTrustManager;
import org.copperforge.mog.scm.MogScm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GiteaService extends MogGitService {

    private final Logger log = LoggerFactory.getLogger(GiteaService.class);

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected MogHttpResponse<?> performRequest(MogScm scm, MogHttpRequest request) throws MogException {
        try {
            MogGitea gitea =  (MogGitea) scm;

            // add token auth if no auth present
            if (!request.hasAuthorizationHeader()) {
                request.auth(MogTokenAuthorization.create(gitea.getToken()));
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

            HttpRequest httpRequest = requestBuilder.uri(URI.create(gitea.getApiUrl() + request.getUrl()))
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
