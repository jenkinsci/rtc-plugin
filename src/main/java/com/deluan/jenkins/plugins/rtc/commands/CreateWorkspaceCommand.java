package com.deluan.jenkins.plugins.rtc.commands;

import com.deluan.jenkins.plugins.rtc.JazzConfiguration;
import hudson.util.ArgumentListBuilder;

/**
 * @author gonzalez
 */
public class CreateWorkspaceCommand extends AbstractCommand {

    public CreateWorkspaceCommand(JazzConfiguration configurationProvider) {
        super(configurationProvider);
    }

    public ArgumentListBuilder getArguments() {
        ArgumentListBuilder args = new ArgumentListBuilder();
		args.add("create");
		args.add("workspace");
		args.add(getConfig().getWorkspaceName());
        addLoginArgument(args);
        addRepositoryArgument(args);
        args.add("-s");
		args.add(getConfig().getStreamName());
        
        return args;
    }

}
