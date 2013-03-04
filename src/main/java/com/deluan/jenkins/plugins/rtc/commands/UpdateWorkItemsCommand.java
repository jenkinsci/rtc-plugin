package com.deluan.jenkins.plugins.rtc.commands;

import com.deluan.jenkins.plugins.rtc.JazzConfiguration;
import hudson.util.ArgumentListBuilder;

/**
 * @author gonzalez
 */
public class UpdateWorkItemsCommand extends AbstractCommand {
	private String userName = null;
	private String password = null;
	private String workspaceName = null;
	private String timeToCheck = null;
	private String loadRulesFileName = null;
	private String message = null;
	private String URLLink = null;

    public UpdateWorkItemsCommand(JazzConfiguration configurationProvider) {
        super(configurationProvider);
    }

    public ArgumentListBuilder getArguments() {
        ArgumentListBuilder args = new ArgumentListBuilder();
		args.add(userName);
		args.addMasked(password);
		args.add(workspaceName);
		args.add(timeToCheck);
		args.add(loadRulesFileName);
		args.add(message); // will spaces work here?
		args.add(URLLink);
        
        return args;
    }
	
	public void setUserName(String userName) {
		this.userName = userName;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public void setWorkspaceName(String workspaceName) {
		this.workspaceName = workspaceName;
	}
	
	public void setTimeToCheck(String timeToCheck) {
		this.timeToCheck = timeToCheck;
	}
	
	public void setLoadRulesFileName(String loadRulesFileName) {
		this.loadRulesFileName = loadRulesFileName;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public void setURLLink(String URLLink) {
		this.URLLink = URLLink;
	}
}
