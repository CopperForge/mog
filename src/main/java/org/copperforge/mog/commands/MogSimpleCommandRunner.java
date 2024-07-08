package org.copperforge.mog.commands;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.copperforge.mog.MogException;

public class MogSimpleCommandRunner implements MogCommandRunner {

    protected String[] prefixes() {
        return new String[] {};
    }

    protected String[] suffixes() {
        return new String[] {};
    }

    @Override
    public void run(MogCommand command) throws MogException {
        try {
            List<String> commands = new ArrayList<>();
            commands.addAll(Arrays.asList(prefixes()));
            commands.addAll(Arrays.asList(command.getCommand()));
            commands.addAll(Arrays.asList(suffixes()));

            ProcessBuilder builder = new ProcessBuilder(commands).redirectErrorStream(true);
            System.out.println("env = " + builder.environment());
            System.out.println("cmd = " + builder.command());

            Process process = builder.start();

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(
                    process.getInputStream()));
            String s = null;
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
            }
            System.out.println("process = " + process);
        } catch (IOException e) {
            throw new MogException(e);
        }
    }

}
