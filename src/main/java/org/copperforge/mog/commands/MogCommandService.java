package org.copperforge.mog.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.copperforge.mog.MogException;
import org.copperforge.mog.annotations.MogService;
import org.copperforge.mog.reflection.MogAnnotationFilter;
import org.copperforge.mog.reflection.MogClassScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MogService(name = "commandService")
public class MogCommandService {

    private Logger log = LoggerFactory.getLogger(MogCommandService.class);

    /**
     * List the available MOG commands.
     * 
     * @return a list of MogCommands; possibly empty, never null
     */
    public List<MogCommand> list() throws MogException {
        List<MogCommand> commands = new ArrayList<>();

        // search command folders for *.command.mog

        // search annotations for @MogCommand
        Set<Class<?>> annotatedCommands = new MogClassScanner().filter(MogAnnotationFilter.filter(MogCommand.class))
                .scan();
        log.info("annotatedCommands = " + annotatedCommands);

        return commands;
    }

}
