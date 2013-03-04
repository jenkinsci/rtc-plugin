package com.deluan.jenkins.plugins.rtc;
import hudson.FilePath;
import hudson.model.*;
import java.io.*;

/**
 * @author deluan
 */
public class JazzConfiguration implements Cloneable {
    private String repositoryLocation;
    private String workspaceName;
    private String defaultWorkspaceName;
    private String commonWorkspaceUNC;
    private String nodeName;
    private String jobName;
    private String agentsUsingCommonWorkspace;
    private String streamName;
    private String loadRules;
    private AbstractBuild build;
    private String username;
    private String password;
	private boolean useUpdate;
    private FilePath jobWorkspace;
    TaskListener listener;

    public String getRepositoryLocation() {
        return repositoryLocation;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }
	
    public String getDefaultWorkspaceName() {
        return defaultWorkspaceName;
    }
	
	public boolean getUseUpdate() {
		return useUpdate;
	}
	
	public void setUseUpdate(boolean useUpdate) {
		this.useUpdate = useUpdate;
	}
	
    public String getCommonWorkspaceUNC() {
        return commonWorkspaceUNC;
    }

    public String getAgentsUsingCommonWorkspace() {
        return agentsUsingCommonWorkspace;
    }

    public AbstractBuild getBuild() {
        return build;
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getJobName() {
        return jobName;
    }

    public String getStreamName() {
        return streamName;
    }

    public String getLoadRules() {
        return loadRules;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public TaskListener getTaskListener() {
        return listener;
    }

    public FilePath getJobWorkspace() {
        return jobWorkspace;
    }

    public void setRepositoryLocation(String repositoryLocation) {
        this.repositoryLocation = repositoryLocation;
    }

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }
    public void setDefaultWorkspaceName(String defaultWorkspaceName) {
        this.defaultWorkspaceName = defaultWorkspaceName;
    }

    public void setCommonWorkspaceUNC(String commonWorkspaceUNC) {
        this.commonWorkspaceUNC = commonWorkspaceUNC;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

	// This method serves as the initialization and is called as early as possible.
    public void setBuild(AbstractBuild build) {
        this.build = build;
        
   		try {
	        agentsUsingCommonWorkspace = build.getEnvironment(null).get("RTC_AGENTS_USING_COMMON_WORSPACE");
   	        jobName = build.getEnvironment(null).get("JOB_NAME");
	        commonWorkspaceUNC = build.getEnvironment(null).get("RTC_COMMON_WORSPACE_UNC");
		} catch (Exception e) {
			listener.error("*** Failed to get retrieve variable" + e);
		}
		Node node = build.getBuiltOn();
        nodeName = node.getNodeName();
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public void setAgentsUsingCommonWorkspace(String agentsUsingCommonWorkspace) {
        this.agentsUsingCommonWorkspace = agentsUsingCommonWorkspace;
    }

    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }

    public void setLoadRules(String loadRules) {
        this.loadRules = loadRules;
    }

    public void setTaskListener(TaskListener listener) {
        this.listener = listener;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setJobWorkspace(FilePath jobWorkspace) {
        this.jobWorkspace = jobWorkspace;
    }

    @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException", "CloneDoesntCallSuperClone"})
    @Override
    public JazzConfiguration clone() {
        JazzConfiguration clone = new JazzConfiguration();

        clone.repositoryLocation = this.repositoryLocation;
        clone.workspaceName = this.workspaceName;
        clone.defaultWorkspaceName = this.defaultWorkspaceName;
        clone.commonWorkspaceUNC = this.commonWorkspaceUNC;
        clone.nodeName = this.nodeName;
        clone.jobName = this.jobName;
        clone.agentsUsingCommonWorkspace = this.agentsUsingCommonWorkspace;
        clone.streamName = this.streamName;
        clone.loadRules = this.loadRules;
        clone.username = this.username;
        clone.password = this.password;
        clone.jobWorkspace = this.jobWorkspace;

        return clone;
    }
 
 
	public boolean isUsingSharedWorkspace() {
		
		if (agentsUsingCommonWorkspace != null && nodeName != null) {
			
			agentsUsingCommonWorkspace = agentsUsingCommonWorkspace.toLowerCase();
			nodeName = nodeName.toLowerCase();
			nodeName = nodeName.trim();
			
			//See if the current node is in the list of shared workspace agents.
			int match = agentsUsingCommonWorkspace.indexOf(nodeName);
			
			if (match != -1) {
				consoleOut("  ---------------------------------------------");
				consoleOut("  --- Using common (shared) workspace ");
				consoleOut("          * Agent name: [" + nodeName + "]");
				consoleOut("          * Common Workspace: [" + commonWorkspaceUNC + "]");
				consoleOut("          * All Agents configured to use common workspace: [" + agentsUsingCommonWorkspace + "]");
				consoleOut("  ---------------------------------------------");
				return true;
			}
			else {				
				return false;					
			}
		}	
		return false;
	}   
	
    public void display(String sLabel) {
    	
    	// Display the configuration contents for debugging.
    	if (listener != null) {
    		PrintStream output = listener.getLogger();
    		if (sLabel != "") {
    			output.println("-----------------------------------------------");
    			output.println("          " + sLabel);
    			output.println("-----------------------------------------------");
    		}
    		
   		    output.println("   * commonWorkspaceUNC: [" + commonWorkspaceUNC + "]");
		    output.println("   * nodeName: [" + nodeName + "]");
		    output.println("   * jobName: [" + jobName + "]");
		    output.println("   * agentsUsingCommonWorkspace: [" + agentsUsingCommonWorkspace + "]");
		    output.println("   * streamName: [" + streamName + "]");
		    output.println("   * loadRules: [" + loadRules + "]");
    	}
    }
    
    public void display() {
    	display("");
    }
    
    public void consoleOut(String sOutString) {
    	// Display the configuration contents for debugging.
    	if (listener != null) {
    		PrintStream output = listener.getLogger();
    		output.println(sOutString);
    	}
    }
}
