package org.copperforge.mog.gitea.services;

import java.io.File;
import java.util.Collection;
import java.util.Optional;

import org.copperforge.mog.MogException;
import org.copperforge.mog.annotations.MogService;
import org.copperforge.mog.gitea.models.GiteaOrganization;
import org.copperforge.mog.gitea.models.GiteaRepository;
import org.copperforge.mog.http.MogHttpMethod;
import org.copperforge.mog.http.MogHttpRequest;
import org.copperforge.mog.http.MogHttpResponse;
import org.copperforge.mog.scm.MogScm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MogService(name = "gitea")
public class GiteaOrganizationService extends GiteaService {

    private final Logger log = LoggerFactory.getLogger(GiteaOrganizationService.class);

    @SuppressWarnings("unchecked")
    public Collection<GiteaOrganization> list(MogScm scm) throws MogException {
        String url = "/orgs";

        MogHttpRequest request = new MogHttpRequest()
                .accepts(GiteaOrganization.class).url(url).method(MogHttpMethod.GET);
        log.trace("request = " + request);
        MogHttpResponse<?> response = performRequest(scm, request);
        Collection<GiteaOrganization> organizations = (Collection<GiteaOrganization>) response.list();
        log.trace("response = " + response);

        return organizations;
    }

    public Optional<GiteaOrganization> get(MogScm scm, String name) throws MogException {
        Optional<GiteaOrganization> organization = Optional.empty();

        String url = "/orgs/" + name;

        MogHttpRequest request = new MogHttpRequest()
                .accepts(GiteaOrganization.class).url(url).method(MogHttpMethod.GET);
        log.trace("request = " + request);
        MogHttpResponse<?> response = performRequest(scm, request);
        organization = Optional.ofNullable((GiteaOrganization) response.singleton());
        log.trace("response = " + response);

        return organization;
    }

    @SuppressWarnings("unchecked")
    public Collection<GiteaRepository> repos(MogScm scm, String orgName) throws MogException {
        String url = "/orgs/" + orgName + "/repos?limit=-1";

        MogHttpRequest request = new MogHttpRequest()
                .accepts(GiteaRepository.class).url(url).method(MogHttpMethod.GET);
        log.trace("request = " + request);
        MogHttpResponse<?> response = performRequest(scm, request);
        Collection<GiteaRepository> repos = (Collection<GiteaRepository>) response.list();
        log.trace("response = " + response);

        return repos;
    }

    public Collection<GiteaRepository> cloneAll(MogScm scm, String orgName, String cloneTo) throws MogException {
        Collection<GiteaRepository> repos = repos(scm, orgName);
        for (GiteaRepository repo : repos) {
            clone(repo.getSshCloneUrl(), cloneTo + File.separator + orgName + File.separator + repo.getName());
        }
        return repos;

    }

}
