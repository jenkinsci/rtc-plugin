package com.deluan.jenkins.plugins.rtc.commands;

import com.deluan.jenkins.plugins.rtc.JazzConfiguration;
import hudson.util.ArgumentListBuilder;

/**
 * @author gonzalez
 */
public class HistoryCommand extends AbstractCommand {

    public HistoryCommand(JazzConfiguration configurationProvider) {
        super(configurationProvider);
    }

    public ArgumentListBuilder getArguments() {
        ArgumentListBuilder args = new ArgumentListBuilder();
		args.add("history");
		args.add("-w");
		args.add(getConfig().getWorkspaceName());
        addLoginArgument(args);
        addRepositoryArgument(args);
		args.add("-c");
		args.add("stubComponentNameHere");
        
        return args;
    }
}
