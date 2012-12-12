package com.deluan.jenkins.plugins.rtc.commands;

import com.deluan.jenkins.plugins.rtc.JazzConfiguration;
import hudson.util.ArgumentListBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author deluan
 */
public class VersionCommand extends AbstractCommand implements ParseableCommand<String> {
    public VersionCommand(JazzConfiguration configurationProvider) {
        super(configurationProvider);
    }

    public ArgumentListBuilder getArguments() {
        ArgumentListBuilder args = new ArgumentListBuilder();
        args.add("version");

        return args;
    }

    public String parse(BufferedReader reader) throws ParseException, IOException {
        String version = null;
        String line;
        Pattern pattern = Pattern.compile(".*(\\d+\\.\\d+\\.\\d+)\\..*");
        while ((line = reader.readLine()) != null) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                version = matcher.group(1);
                break;
            }
        }

        return version;
    }
}
