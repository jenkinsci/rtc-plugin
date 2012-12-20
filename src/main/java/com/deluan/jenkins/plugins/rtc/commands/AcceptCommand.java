package com.deluan.jenkins.plugins.rtc.commands;

import com.deluan.jenkins.plugins.rtc.JazzConfiguration;
import com.deluan.jenkins.plugins.rtc.changelog.JazzChangeSet;
import com.deluan.jenkins.plugins.rtc.commands.accept.AcceptOutputParser_3_1_0;
import com.deluan.jenkins.plugins.rtc.commands.accept.AcceptNewOutputParser;
import com.deluan.jenkins.plugins.rtc.commands.accept.AcceptOldOutputParser;
import com.deluan.jenkins.plugins.rtc.commands.accept.BaseAcceptOutputParser;
import hudson.util.ArgumentListBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * @author deluan
 */
public class AcceptCommand extends AbstractCommand implements ParseableCommand<Map<String, JazzChangeSet>> {

    public static final String FORMAT_VERSION_2_1_0 = "2.1.0";
    public static final String FORMAT_VERSION_3_1_0 = "3.1.0";
    private Collection<String> changeSets;
    private BaseAcceptOutputParser parser;
    protected boolean oldFormat = false;

    public AcceptCommand(JazzConfiguration configurationProvider, Collection<String> changeSets, String version) {
        super(configurationProvider);
        this.changeSets = new LinkedHashSet<String>(changeSets);

        if (version.compareTo(FORMAT_VERSION_3_1_0) == 0) {
            parser = new AcceptOutputParser_3_1_0();
        } else {
            this.oldFormat = (version.compareTo(FORMAT_VERSION_2_1_0) < 0);
            parser = (oldFormat) ? new AcceptOldOutputParser() : new AcceptNewOutputParser();
        }
    }

    public ArgumentListBuilder getArguments() {
        ArgumentListBuilder args = new ArgumentListBuilder();

        args.add("accept");
        addLoginArgument(args);
        addLocalWorkspaceArgument(args);
        addSourceStream(args);
        args.add("--flow-components", "-o", "-v");
        if (hasAnyChangeSets()) {
            addChangeSets(args);
        }

        return args;
    }

    private void addSourceStream(ArgumentListBuilder args) {
        args.add("-s", getConfig().getStreamName());
    }

    private boolean hasAnyChangeSets() {
        return changeSets != null && !changeSets.isEmpty();
    }

    private void addChangeSets(ArgumentListBuilder args) {
        args.add("-c");
        for (String changeSet : changeSets) {
            args.add(changeSet);
        }
    }

    public Map<String, JazzChangeSet> parse(BufferedReader reader) throws ParseException, IOException {
        return parser.parse(reader);
    }
}
