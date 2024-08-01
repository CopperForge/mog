package org.copperforge.mog.gitea.services;

import java.util.Collection;

import org.apache.commons.lang3.NotImplementedException;
import org.copperforge.mog.MogException;
import org.copperforge.mog.annotations.MogService;
import org.copperforge.mog.gitea.models.GiteaOrganization;
import org.copperforge.mog.http.MogHttpMethod;
import org.copperforge.mog.http.MogHttpRequest;
import org.copperforge.mog.http.MogHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

@MogService(name = "gitea")
public class GiteaOrganizationService extends GiteaService {

    private final Logger log = LoggerFactory.getLogger(GiteaOrganizationService.class);

    @SuppressWarnings("unchecked")
    public Collection<GiteaOrganization> list() throws MogException {
        String url = "/orgs";

        MogHttpRequest request = new MogHttpRequest()
                .accepts(GiteaOrganization.class).url(url).method(MogHttpMethod.GET);
        log.trace("request = " + request);
        MogHttpResponse<?> response = performRequest(request);
        Collection<GiteaOrganization> organizations = (Collection<GiteaOrganization>) response.list();
        log.trace("response = " + response);

        return organizations;
    }

    public Optional<GiteaOrganization> get(String name) {
        throw new NotImplementedException();
    }

}
