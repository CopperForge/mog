package org.copperforge.mog.git;

import java.io.File;

import org.copperforge.mog.MogException;
import org.copperforge.mog.annotations.MogService;
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

@MogService(name = "gitService")
public class MogGitService {

    private final Logger log = LoggerFactory.getLogger(MogGitService.class);

    protected final SshdSessionFactoryBuilder builder = new SshdSessionFactoryBuilder()
            .setPreferredAuthentications("publickey")
            .setHomeDirectory(FS.DETECTED.userHome());

    public void clone(String url, String destination) throws MogException {
        try {
            File sshDir = new File(FS.DETECTED.userHome(), "/.ssh"); // TODO, allow for override through switches or
                                                                     // config
            log.info("sshDir = " + sshDir.getAbsolutePath());
            SshdSessionFactory sshSessionFactory = builder
                    .setSshDirectory(sshDir)
                    .build(null);

            File target = new File(destination);
            log.debug("Cloning from " + url + " to " + target.getAbsolutePath());
            CloneCommand command = Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(target);

            if (sshSessionFactory != null) {
                command.setTransportConfigCallback(new TransportConfigCallback() {
                    @Override
                    public void configure(Transport transport) {
                        ((SshTransport) transport).setSshSessionFactory(sshSessionFactory);
                    }
                });
            }
            command.call();
        } catch (

        GitAPIException e) {
            throw new MogException(e);
        }

    }
}