package org.copperforge.mog.command;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.copperforge.mog.MogConfig;
import org.copperforge.mog.MogException;
import org.copperforge.mog.annotations.MogService;
import org.copperforge.mog.annotations.Moglet;
import org.copperforge.mog.env.MogEnvironmentService;
import org.copperforge.mog.reader.MogReader;
import org.copperforge.mog.reflection.MogAnnotationFilter;
import org.copperforge.mog.reflection.MogClassScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MogService(name = "commandService")
public class MogCommandService {

    private Logger log = LoggerFactory.getLogger(MogCommandService.class);

    private final List<MogCommand> commands = new ArrayList<>();

    @Moglet
    MogConfig config;

    @Moglet
    MogEnvironmentService environmentService;

    public MogCommandService() {
    }

    public MogCommand get(String name) {
        return commands.stream().filter(c -> c.getName().equals(name)).findFirst().orElse(null);
    }

    public List<MogCommand> find() throws MogException {
        commands.clear();

        // search command folders for *.command.mog
        commands.addAll(findCommandMogs());

        // search annotations for @MogCommand
        commands.addAll(findAnnotated());

        return commands;
    }

    /**
     * List the available MOG commands.
     * 
     * @return a list of MogCommands; possibly empty, never null
     */
    public List<? extends MogCommand> list() throws MogException {
        return commands;
    }

    protected List<? extends MogCommand> findAnnotated() throws MogException {
        return new MogClassScanner()
                .filter(MogAnnotationFilter.filter(org.copperforge.mog.annotations.MogCommand.class))
                .scan().stream().map(c -> {
                    log.info("c = " + c);
                    org.copperforge.mog.annotations.MogCommand annotation = c
                            .getAnnotation(org.copperforge.mog.annotations.MogCommand.class);
                    MogAnnotatedCommand command = new MogAnnotatedCommand();
                    command.setClassName(c.getName());
                    command.setDescription(annotation.description());
                    command.setName(annotation.name());
                    log.info("annotation = " + annotation);
                    return command;
                }).collect(Collectors.toList());

    }

    protected List<? extends MogCommand> findCommandMogs() throws MogException {
        MogReader<MogCommand> commandReader = new MogReader<MogCommand>(MogCommand.class);
        List<MogCommand> commands = new ArrayList<MogCommand>();

        for (String path : config.getCommandPaths()) {
            path = environmentService.envsubst(path);
            log.info("path = " + path);

            File p = new File(path);
            if (p.exists() && p.isDirectory()) {
                List<File> files = Stream.of(new File(path).listFiles())
                        .filter(f -> f.getName().matches(".*\\.command\\.mog")).collect(Collectors.toList());
                for (File file : files) {
                    commands.add(commandReader.read(file));
                }
            } else if (p.exists() && p.isFile()) {
                commands.add(commandReader.read(p));
            } else if (!p.exists()) {
                // throw new MogException("Resource " + path + " does not exist");
                log.warn("Resource " + path + " does not exist");
            }

        }

        return commands;
    }
}
