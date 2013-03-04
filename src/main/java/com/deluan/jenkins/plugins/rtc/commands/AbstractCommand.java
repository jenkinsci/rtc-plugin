package com.deluan.jenkins.plugins.rtc.commands;

import com.deluan.jenkins.plugins.rtc.JazzConfiguration;
import hudson.util.ArgumentListBuilder;
import org.apache.commons.lang.StringUtils;
import java.io.*;

/**
 * @author deluan
 */
public abstract class AbstractCommand implements Command {

    private final JazzConfiguration config;

    protected JazzConfiguration getConfig() {
        return config;
    }

    public AbstractCommand(JazzConfiguration configurationProvider) {
        this.config = configurationProvider;
    }

    protected ArgumentListBuilder addLoginArgument(ArgumentListBuilder arguments) {
        if (StringUtils.isNotBlank(config.getUsername())) {
            arguments.add("-u");
            arguments.addMasked(config.getUsername());
        }
        if (StringUtils.isNotBlank(config.getPassword())) {
            arguments.add("-P");
            arguments.addMasked(config.getPassword());
        }

        return arguments;
    }

    protected ArgumentListBuilder addRepositoryArgument(ArgumentListBuilder args) {
        return args.add("-r", getConfig().getRepositoryLocation());
    }

    protected ArgumentListBuilder addLocalWorkspaceArgument(ArgumentListBuilder args) {				
        args.add("-d");
        args.add(getConfig().getJobWorkspace());		   
        
        return args;
    }
	
    protected ArgumentListBuilder addSharedWorkspaceArgument(ArgumentListBuilder args) {
        args.add("-d");
		String sPath = getConfig().getCommonWorkspaceUNC() + getConfig().getJobName() + "\\";
		args.add(sPath);

        return args;
    }
}
