package org.copperforge.mog.gitea;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.copperforge.mog.Mog;
import org.copperforge.mog.MogException;
import org.copperforge.mog.MogOptions;
import org.copperforge.mog.annotations.MogCommand;
import org.copperforge.mog.annotations.Moglet;
import org.copperforge.mog.config.MogConfig;
import org.copperforge.mog.gitea.models.GiteaOrganization;
import org.copperforge.mog.gitea.services.GiteaOrganizationService;

import picocli.CommandLine;
import picocli.CommandLine.Option;

@MogCommand(name = "gitea", method = "list")
public class MogGiteaCommand {

    @Moglet
    private GiteaOrganizationService giteaService;

    @Option(names = { "--name", "--gitea" })
    private String name;

    @Option(names = { "--org", "--organization" })
    private String organization;

    @Option(names = "--clone")
    private boolean clone;

    @Option(names = { "--repo", "--repository" })
    private String repo;

    @Option(names = { "--dest", "--destination" })
    private String destination;

    private final MogConfig config = Mog.mog().config();

    public List<MogGitea> list(MogOptions options) {
        if (options != null)
            parseOptions(options);
        List<MogGitea> giteas = config.getDevops().getScms().stream().filter(s -> s.getType().equals("gitea"))
                .map(MogGitea.class::cast).collect(Collectors.toList());
        System.out.println("giteas = " + giteas);
        return giteas;
    }

    @MogCommand(name = "org")
    public void org(MogOptions options) throws MogException {
        parseOptions(options);

        if (clone == true) {
            processCloneRequest();
            return;
        }

        if (organization == null) {
            listOrganizations();
            return;
        }

        Optional<GiteaOrganization> org = giteaService.get(gitea(), organization);
        System.out.println("org = " + org); // TODO

    }

    private void processCloneRequest() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'processCloneRequest'");
    }

    private void listOrganizations() throws MogException {
        for (GiteaOrganization org : giteaService.list(gitea())) {
            System.out.println(org.getName()); // TODO
        }
    }

    private void parseOptions(MogOptions options) {
        new CommandLine(this).setUnmatchedArgumentsAllowed(true)
                .parseArgs(options.rawArgs().toArray(new String[0]));
    }

    protected MogGitea gitea() {
        return list(null).stream().filter(g -> g.getName().equals(name)).findFirst().orElse(null);
    }
}
