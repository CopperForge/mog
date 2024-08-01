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
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public Optional<GiteaOrganization> get(String name) throws MogException {
        Optional<GiteaOrganization> organization = Optional.empty();

        String url = "/orgs/" + name;

        MogHttpRequest request = new MogHttpRequest()
                .accepts(GiteaOrganization.class).url(url).method(MogHttpMethod.GET);
        log.trace("request = " + request);
        MogHttpResponse<?> response = performRequest(request);
        organization = Optional.ofNullable((GiteaOrganization) response.singleton());
        log.trace("response = " + response);

        return organization;
    }

    @SuppressWarnings("unchecked")
    public Collection<GiteaRepository> repos(String orgName) throws MogException {
        String url = "/orgs/" + orgName + "/repos?limit=-1";

        MogHttpRequest request = new MogHttpRequest()
                .accepts(GiteaRepository.class).url(url).method(MogHttpMethod.GET);
        log.trace("request = " + request);
        MogHttpResponse<?> response = performRequest(request);
        Collection<GiteaRepository> repos = (Collection<GiteaRepository>) response.list();
        log.trace("response = " + response);

        return repos;
    }

    public Collection<GiteaRepository> clone(String orgName, String cloneTo) throws MogException {
        try {
            Collection<GiteaRepository> repos = repos(orgName);
            File sshDir = new File("c:/users/brian", "/.ssh"); // TODO
            log.info("sshDir = " + sshDir.getAbsolutePath());
            SshdSessionFactory sshSessionFactory = new SshdSessionFactoryBuilder()
                    .setPreferredAuthentications("publickey")
                    .setHomeDirectory(FS.DETECTED.userHome())
                    .setSshDirectory(sshDir)
                    .build(null);
            log.info("sshSessionFactory = " + sshSessionFactory);

            for (GiteaRepository repo : repos(orgName)) {
                File target = new File(cloneTo + File.separator + orgName + File.separator + repo.getName());
                log.info(repo.getName() + " cloning from " + repo.getSshCloneUrl() + " to " + target.getAbsolutePath());
                CloneCommand command = Git.cloneRepository()
                        .setURI(repo.getSshCloneUrl())
                        .setDirectory(target);

                if (sshSessionFactory != null) {
                    command.setTransportConfigCallback(new TransportConfigCallback() {
                        @Override
                        public void configure(Transport transport) {
                            ((SshTransport) transport).setSshSessionFactory(sshSessionFactory);
                        }
                    });
                }

                // command.setCredentialsProvider(new
                // UsernamePasswordCredentialsProvider("beldridge2", "empire1981!")); // TODO
                command.call();
            }
            return repos;
        } catch (GitAPIException e) {
            throw new MogException(e);
        }

    }

}
