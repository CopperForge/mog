package org.copperforge.mog.command;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.copperforge.mog.MogException;
import org.copperforge.mog.MogFinder;
import org.copperforge.mog.annotations.MogRunner;
import org.copperforge.mog.annotations.MogService;
import org.copperforge.mog.annotations.Moglet;
import org.copperforge.mog.command.runner.MogCommandRunner;
import org.copperforge.mog.config.MogConfig;
import org.copperforge.mog.reader.MogReader;
import org.copperforge.mog.reflection.MogAnnotationFilter;
import org.copperforge.mog.reflection.MogClassScanner;
import org.copperforge.mog.var.MogVariableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MogService(name = "commandService")
public class MogCommandService {

    private Logger log = LoggerFactory.getLogger(MogCommandService.class);

    private final List<MogCommand> commands = new ArrayList<>();
    private final Map<String, Object> runners = new HashMap<>();


    @Moglet
    MogConfig config;

    @Moglet
    MogVariableService environmentService;

    public MogCommandService() throws MogException {
        discoverRunners();
        // find();
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

        log.debug("Found commands :: " + commands);
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
                    org.copperforge.mog.annotations.MogCommand annotation = c
                            .getAnnotation(org.copperforge.mog.annotations.MogCommand.class);
                    MogAnnotatedCommand command = new MogAnnotatedCommand();
                    command.setClassName(c.getName());
                    command.setDescription(annotation.description());
                    command.setName(annotation.name());
                    return command;
                }).collect(Collectors.toList());

    }

    protected List<? extends MogCommand> findCommandMogs() throws MogException {
        MogFinder<MogCommand, MogReader<MogCommand>> finder = new MogFinder<>(new MogReader<MogCommand>(MogCommand.class));
        List<MogCommand> commands = new ArrayList<>();

        for (String path : config.getSearchPaths().getCommands()) {
            path = environmentService.envsubst(path);
            log.debug("path = " + path);

            if (new File(path).exists()) {
                commands.addAll(finder.find(path, "*.command.mog"));
            } else {
                log.warn("Path '" + path + "' does not exist.");
            }
        }

        return commands;
    }

    @SuppressWarnings({ "unchecked" })
    protected void discoverRunners() throws MogException {
        // discover all the services available and put them in the service map
        try {
            MogClassScanner scanner = new MogClassScanner();
            log.debug("Finding MogRunners ...");
            Set<Class<?>> runnerClasses = scanner.filter(MogAnnotationFilter.filter(MogRunner.class)).scan();
            log.debug("Found classes = " + runnerClasses);
            
            Set<Class<? extends MogCommandRunner<?>>> mogRunners = runnerClasses.stream()
                    .map(c -> (Class<? extends MogCommandRunner<?>>) c).collect(Collectors.toSet());

            MogRunner annote;
            Object instance;
            log.debug("Finding MogRunners ...");
            for (Class<? extends MogCommandRunner<?>> runner : mogRunners) {
                log.debug("Found runner : " + runner);
                annote = runner.getAnnotation(MogRunner.class);
                instance = runner.getDeclaredConstructor().newInstance();
                runners.put(annote.commandClass().getName(), instance);
            }
            log.debug("Found :: " + runners.toString());
        } catch (Exception e) {
            log.error("Unable to process MogServiceManager initialization", e);
            throw new MogException(e);
        }

    }

    public Map<String, Object> runners() {
        return runners;
    }

    @SuppressWarnings("unchecked")
    public MogCommandRunner<? extends MogCommand> runner(Class<? extends MogCommand> commandClass) {
        return (MogCommandRunner<? extends MogCommand>) runners.get(commandClass.getName());
    }

}
