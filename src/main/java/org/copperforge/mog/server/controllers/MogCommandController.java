package org.copperforge.mog.server.controllers;

import org.copperforge.mog.MogException;
import org.copperforge.mog.MogOptions;
import org.copperforge.mog.MogServiceManager;
import org.copperforge.mog.command.MogCommand;
import org.copperforge.mog.command.MogCommandRequest;
import org.copperforge.mog.command.MogCommandResponse;
import org.copperforge.mog.command.MogCommandService;
import org.copperforge.mog.server.MogResponse;
import org.copperforge.mog.server.annotations.MogBody;
import org.copperforge.mog.server.annotations.MogController;
import org.copperforge.mog.server.annotations.MogRequest;

import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;

@MogController(path="/command")
public class MogCommandController {

    @MogRequest(path="/execute", method = Methods.POST_STRING)
    public MogResponse<MogCommandResponse> execute(@MogBody MogCommandRequest request) throws MogException {
        MogCommandResponse response = new MogCommandResponse();
        MogCommandService commandService = (MogCommandService) MogServiceManager.instance().get(MogCommandService.class);
        MogCommand command = commandService.get(request.getCommand());

        MogOptions options = MogOptions.parse(request.getRawArgs().toArray(new String[0]));
        commandService.runner(command.getClass()).run(command, options);

        return new MogResponse<MogCommandResponse>(response, StatusCodes.OK);
    }

}
