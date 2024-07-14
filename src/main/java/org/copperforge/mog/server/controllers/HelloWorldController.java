package org.copperforge.mog.server.controllers;

import org.copperforge.mog.server.annotations.MogController;
import org.copperforge.mog.server.annotations.MogRequest;

import io.undertow.util.Methods;

@MogController(path="/")
public class HelloWorldController {

    @MogRequest(path="hello", method = Methods.GET_STRING, produces = "text/plain")
    public String helloWorld() {
        return "Hello from mog!";
    }
}
