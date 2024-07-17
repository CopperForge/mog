package org.copperforge.mog.server;

import org.copperforge.mog.MogOptions;

import picocli.CommandLine;
import picocli.CommandLine.Option;

public class MogServerOptions extends MogOptions {

    @Option(names = { "--host" }, description = "specify the listening host")
    private String host;

    @Option(names = { "--port" }, description = "specify the listening port")
    private Integer port;

    @Option(names = { "--prefix" }, description = "specify the MOG document prefix")
    private String prefix;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public String toString() {
        return "MogServerOptions [host=" + host + ", port=" + port + ", prefix=" + prefix + "]";
    }

    public static MogServerOptions parse(MogOptions options) {
        MogServerOptions serverOptions = new MogServerOptions();
        new CommandLine(serverOptions).parseArgs(options.rawArgs().toArray(new String[0]));
        return serverOptions;
    }

}
