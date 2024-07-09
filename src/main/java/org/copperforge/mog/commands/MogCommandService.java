package org.copperforge.mog.commands;

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
import org.copperforge.mog.reflection.MogAnnotationFilter;
import org.copperforge.mog.reflection.MogClassScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MogService(name = "commandService")
public class MogCommandService {

    private Logger log = LoggerFactory.getLogger(MogCommandService.class);

    @Moglet
    MogConfig config;
    @Moglet
    MogEnvironmentService environmentService;

    public MogCommandService() {
    }

    /**
     * List the available MOG commands.
     * 
     * @return a list of MogCommands; possibly empty, never null
     */
    public List<MogCommand> list() throws MogException {
        List<MogCommand> commands = new ArrayList<>();

        // search command folders for *.command.mog
        for (String path : config.getCommandPaths()) {
            path = environmentService.envsubst(path);
            log.info("path = " + path);

            File p = new File(path);
            if (p.exists() && p.isDirectory()) {
                log.info("contents = " + Stream.of(new File(path).listFiles()).collect(Collectors.toList()));
            } else if (p.exists() && p.isFile()) {

            } else if (!p.exists()) {
                // throw new MogException("Resource " + path + " does not exist");
                log.warn("Resource " + path + " does not exist");
            }
                
        }

        // search annotations for @MogCommand
        commands.addAll(new MogClassScanner()
                .filter(MogAnnotationFilter.filter(org.copperforge.mog.annotations.MogCommand.class))
                .scan().stream().map(c -> {
                    log.info("c = " + c);
                    org.copperforge.mog.annotations.MogCommand annotation = c
                            .getAnnotation(org.copperforge.mog.annotations.MogCommand.class);
                    MogCommand command = new MogCommand();
                    command.setDescription(annotation.description());
                    command.setName(annotation.name());
                    log.info("annotation = " + annotation);
                    return command;
                }).collect(Collectors.toList()));

        return commands;
    }

}
