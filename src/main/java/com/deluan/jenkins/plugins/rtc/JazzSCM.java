package com.deluan.jenkins.plugins.rtc;

import com.deluan.jenkins.plugins.rtc.changelog.JazzChangeLogReader;
import com.deluan.jenkins.plugins.rtc.changelog.JazzChangeLogWriter;
import com.deluan.jenkins.plugins.rtc.changelog.JazzChangeSet;
import com.deluan.jenkins.plugins.rtc.commands.UpdateWorkItemsCommand;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.*;
import hudson.scm.*;
import hudson.util.FormValidation;
import hudson.util.LogTaskListener;
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.GregorianCalendar;

/**
 * @author deluan
 */
@SuppressWarnings("UnusedDeclaration")
public class JazzSCM extends SCM {

    private static final Logger logger = Logger.getLogger(JazzClient.class.getName());

    private String repositoryLocation;
    private String workspaceName;
	private String workspaceNameDynamic = null;
    private String streamName;
    private String username;
    private Secret password;
    
    private boolean useTimeout;
    private Long timeoutValue;
    
    private String loadRules;
	private boolean loadRulesValid = true;
	private boolean useUpdate;
    private String commonWorkspaceUNC;
    private String agentsUsingCommonWorkspace;
    
    AbstractBuild build;

    private JazzRepositoryBrowser repositoryBrowser;

    @DataBoundConstructor
    public JazzSCM(String repositoryLocation, String workspaceName, String streamName,
                   String username, String password,
                   boolean useTimeout, Long timeoutValue,
                   String loadRules, boolean useUpdate) {
        this.repositoryLocation = repositoryLocation;
        this.workspaceName = workspaceName;
        this.streamName = streamName;
        this.username = username;
        this.password = StringUtils.isEmpty(password) ? null : Secret.fromString(password);
        this.useTimeout = useTimeout;
        this.timeoutValue = timeoutValue;
        
        this.loadRules = loadRules;
		this.useUpdate = useUpdate;
		
    }
	
	private boolean validateLoadRules(String loadRules) {
		boolean validLoadRules = true;

		if (loadRules != null) {
			String[] aLoadRuleLines = loadRules.split("\n");
			for (int i = 0; i < aLoadRuleLines.length; i++) {
				String nextLoadRule = aLoadRuleLines[i];
				if (!nextLoadRule.contains(":") && nextLoadRule.length() > 2) {
					validLoadRules = false;
				}
				//Might be able to do something bad if ".." is allowed.
				if (nextLoadRule.contains("..")) {
					validLoadRules = false;
				}
			}
		}
		return validLoadRules;
	}
    
    public String getRepositoryLocation() {
        return repositoryLocation;
    }
	
	public boolean getUseUpdate() {
        return useUpdate;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }
	
    public String getCommonWorkspaceUNC() {
        return commonWorkspaceUNC;
    }
	
    public String getAgentsUsingCommonWorkspace() {
        return agentsUsingCommonWorkspace;
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
        return Secret.toString(password);
    }

    public boolean getUseTimeout() {
        return useTimeout;
    }
    
    public Long getTimeoutValue() {
        return timeoutValue;
    }
    
    private JazzClient getClientInstance(Launcher launcher, TaskListener listener, FilePath jobWorkspace) throws IOException, InterruptedException {
        return new JazzClient(launcher, listener, jobWorkspace, getDescriptor().getJazzExecutable(),
                getConfiguration(listener));
    }

    @Override
    public SCMRevisionState calcRevisionsFromBuild(AbstractBuild<?, ?> build, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
        return null; // This implementation is not necessary, as this information is obtained from the remote RTC's repository
    }

    @Override
    protected PollingResult compareRemoteRevisionWith(AbstractProject<?, ?> project, Launcher launcher, FilePath workspace, TaskListener listener, SCMRevisionState baseline) throws IOException, InterruptedException {
		PollingResult result;

		AbstractBuild<?, ?> build = project.getSomeBuildWithWorkspace();
		this.build = build;
		if (build == null) {
			listener.error("Build was null. Not sure what scenarios cause this.");
			result = PollingResult.BUILD_NOW;
		} else {
			Node node = build.getBuiltOn();
			workspaceNameDynamic = workspaceName.replace("${NODE_NAME}", node.getNodeName());
			JazzClient client = getClientInstance(launcher, listener, workspace);
			try {
				JazzConfiguration configuration = getConfiguration(listener);
				String wsName = configuration.getWorkspaceName();
				
				result = (client.hasChanges()) ? PollingResult.SIGNIFICANT : PollingResult.NO_CHANGES;
			} catch (Exception e) {
				result = PollingResult.NO_CHANGES;
			}
		}
		return result;
    }
	
    @Override
    public boolean checkout(AbstractBuild<?, ?> build, Launcher launcher, FilePath workspace, BuildListener listener, File changelogFile) throws IOException, InterruptedException {
		this.build = build;
		
		//Null this out so that configuration will not use the NODE_NAME from last build
		workspaceNameDynamic = null;
		
		loadRulesValid = validateLoadRules(loadRules);
		
		if (!loadRulesValid) {
			listener.error("Load rules are not valid. Each line should contain a colon separating component and path.");
			return false;
		}
		
        JazzClient client = getClientInstance(launcher, listener, workspace);

		JazzConfiguration config = getConfiguration(listener);
		FilePath file = build.getWorkspace();
		
		if (!useUpdate) {
			try {
				file.act(new CleanWorkspace(file.getRemote()));
			} catch (Exception e) {
				e.printStackTrace();
				listener.error("exception: " + e);
				listener.error("Caused by: " + e.getCause());
			}
		} else {
			try {
				file.act(new RemoveOldSandboxes(file.getRemote(), config.getLoadRules()));
			} catch (Exception e) {
				e.printStackTrace();
				listener.error("exception: " + e);
				listener.error("Caused by: " + e.getCause());
			}
		}
		
		//check if workspace exists and if not than create it
		boolean result = client.workspaceExists(build);
		
		if (result == false) {
			client.createWorkspace();
		}

        // Forces a load of the workspace. If it's already loaded, the scm command will do nothing.
		FilePath path = null;
		try {
			path = build.getWorkspace();
		} catch (Exception e) {
			listener.error("error = " + e);
		}

		// needed because OS on controller may be different than node
		String remoteSeparator = "\\";
		if( file.getRemote().startsWith("/") )
		{
			remoteSeparator = "/";
		}

		try {
			path.act(new com.deluan.jenkins.plugins.rtc.commands.LoadCommand.RemoteFileWriter(path.getRemote() + remoteSeparator + build.getFullDisplayName().substring(0, build.getFullDisplayName().indexOf(" ")) + ".txt", config.getLoadRules()));
		} catch (Exception e) {
			e.printStackTrace();
			config.consoleOut("exception: " + e);
			config.consoleOut("Caused by: " + e.getCause());
		}
        client.load();
				
        // Accepts all incoming changes
        List<JazzChangeSet> changes;
        try {
            changes = client.accept();
        } catch (IOException e) {
            return false;
        }

		try {
			//External tool built to interface with RTC workitems. Does not come with the plugin
			if (new File(build.getEnvironment(listener).get("TOOLS_FOLDER") + "\\RTCWorkItemLinker\\run2.bat").exists()) {
				List<JazzChangeSet> changes2 = updateWorkItems(build, listener, client);
				if (changes2 != null) changes = changes2;
			}
		} catch (Exception e) {
			listener.error("JazzSCM: " + e);
		}
		
		if (changes != null) {
			PrintStream output = listener.getLogger();
			output.println(changes.size() + " changes found.");
			
			if (!changes.isEmpty()) {
				JazzChangeLogWriter writer = new JazzChangeLogWriter();
				writer.write(changes, changelogFile);
			} else {
				createEmptyChangeLog(changelogFile, listener, "changelog");
			}
		}

        return true;
    }
	
	public static class CleanWorkspace implements FilePath.FileCallable<Void>, Serializable {
		String fileName = null;
		
		public CleanWorkspace(String fileName) {
			this.fileName = fileName;
		}
		
		public Void invoke(File f, hudson.remoting.VirtualChannel channel) {
			
			try {
				deleteSubFiles(f);
			} catch (Exception e) {
				printError(fileName, e.toString());
			}
			
			return null;
		}
		
		private void printError(String fileName, String error) {
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(fileName + File.separator + "error.txt");
				fos.write(error.getBytes(), 0, error.length());
				fos.close();
			} catch (Exception e) {
				try {
					if (fos != null) {
						fos.close();
					}
				} catch (Exception ee) {}
			}
		}
		
		void deleteSubFiles(File f) throws IOException {
			for (File c : f.listFiles()) {
				delete(c);
			}
		}
		
		void delete(File f) throws IOException {
			if (f.isDirectory()) {
				for (File c : f.listFiles()) {
					delete(c);
				}
			}
			if (!f.delete()) throw new FileNotFoundException("Failed to delete file: " + f);
		}
	}
	
	//This class will delete any RTC sandboxes that are leftover and not compatible with the current sandbox
	public static class RemoveOldSandboxes implements FilePath.FileCallable<Void>, Serializable {
		String fileName = null;
		String loadRules = null;
		
		public RemoveOldSandboxes(String fileName, String loadRules) {
			this.fileName = fileName;
			this.loadRules = loadRules;
		}
		
		public Void invoke(File f, hudson.remoting.VirtualChannel channel) {
			//if .metadata doesnt exist at the load rule location then delete everything in that subfolder
			//check each folder up the tree for .metadata. if you find it delete it
			String[] aLoadRuleLines = loadRules.split("\n");
			for (int i = 0; i < aLoadRuleLines.length; i++) {
				String[] aLoadRuleParts = aLoadRuleLines[i].split(":");
				
				//Clean up load rule syntax
				if(aLoadRuleParts[1].startsWith("/")) {
					aLoadRuleParts[1] = aLoadRuleParts[1].substring(1, aLoadRuleParts[1].length());
				}
				if (File.separator.equals("\\")) {
					aLoadRuleParts[1] = aLoadRuleParts[1].replace('/', '\\');
				} else {
					aLoadRuleParts[1] = aLoadRuleParts[1].replace('\\', '/');
				}
				
				//Check if the sandbox is already in the right place
				File loadRuleMetaDataFile = new File(f.getAbsolutePath() + File.separator + aLoadRuleParts[1] + File.separator + ".metadata");
				if (!loadRuleMetaDataFile.exists()) {
					//Delete everything in this folder
					//printError(fileName, "Delete everything in " + f.getAbsolutePath() + File.separator + aLoadRuleParts[1] + " to make room for new sandbox.\n");
					try {
						deleteSubFiles(new File(f.getAbsolutePath() + File.separator + aLoadRuleParts[1]));
					} catch (Exception e) {
						printError(fileName, e.toString());
					}

					String parentFolder = aLoadRuleParts[1];
					String fileSeparator = null;
					if (File.separator.equals("\\")) {
						//If the separator is a backslash we need to set this to 4 backslashes before it is sent into the split function
						fileSeparator = "\\\\";
					} else {
						fileSeparator = "/";
					}
					String[] loadRuleFolders = parentFolder.split(fileSeparator);
					String currentFolderToCheck = "";
					for (int j = 0; j < loadRuleFolders.length; j++) {
						if (loadRuleFolders[j].length() > 0) {
							currentFolderToCheck += loadRuleFolders[j] + File.separator;
							//printError(fileName, "Looking at " + f.getAbsolutePath() + File.separator + currentFolderToCheck + ".metadata" + "\n");
							File scanForMetaDataFile = new File(f.getAbsolutePath() + File.separator + currentFolderToCheck + ".metadata");
							if (scanForMetaDataFile.exists()) {
								//printError(fileName, "Delete everything in " + f.getAbsolutePath() + File.separator + currentFolderToCheck + ".\n");
								try {
									deleteSubFiles(new File(f.getAbsolutePath() + File.separator + currentFolderToCheck));
								} catch (Exception e) {
									printError(fileName, e.toString());
								}
							}
						}
					}
				}
			}
			
			return null;
		}
		
		private void printError(String fileName, String error) {
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(fileName + File.separator + "error.txt", true);
				fos.write(error.getBytes(), 0, error.length());
				fos.close();
			} catch (Exception e) {
				try {
					if (fos != null) {
						fos.close();
					}
				} catch (Exception ee) {}
			}
		}
		
		void deleteSubFiles(File f) throws IOException {
			for (File c : f.listFiles()) {
				delete(c);
			}
		}
		
		void delete(File f) throws IOException {
			if (f.isDirectory()) {
				for (File c : f.listFiles()) {
					delete(c);
				}
			}
			if (!f.delete()) throw new FileNotFoundException("Failed to delete file: " + f);
		}
	}
	
	private List<JazzChangeSet> updateWorkItems(AbstractBuild<?, ?> build, BuildListener listener, JazzClient client) throws IOException, InterruptedException {
		List<JazzChangeSet> changes = null;
		FilePath path = null;
		try {
			path = build.getWorkspace();
		} catch (Exception e) {
			listener.error("error = " + e);
		}
		
		//Getting previous build is only valid if the previous build didn't fail before it got to this point. A better implementation
		//would check if the previous build completed this section of code and if not go to the build before that.
		Run lastBuild = build.getPreviousBuild();
		if (lastBuild == null) {
			return null;
		}
		
		String controllerName = null;
		String controllerPort = null;
		try {
			controllerName = build.getEnvironment(null).get("CONTROLLER_NAME");
			controllerPort = build.getEnvironment(null).get("CONTROLLER_PORT");
		} catch (Exception e) {
			listener.error("error2 = " + e);
		}
		if (controllerName == null) {
			controllerName = "127.0.0.1";
		}
		if (controllerPort == null) {
			controllerPort = "8080";
		}
		
		UpdateWorkItemsCommand cmd = new UpdateWorkItemsCommand(getConfiguration(listener));

		//set parameters
		JazzConfiguration config = getConfiguration(listener);
		String remoteSeparator = "\\";
		if( build.getWorkspace().getRemote().startsWith("/") )
		{
			remoteSeparator = "/";
		}
		cmd.setUserName(config.getUsername());
		cmd.setPassword(config.getPassword());
		cmd.setWorkspaceName(config.getWorkspaceName());
		cmd.setTimeToCheck("" + lastBuild.getTimeInMillis());
		cmd.setLoadRulesFileName("\"" + path.getRemote() + remoteSeparator + build.getFullDisplayName().substring(0, build.getFullDisplayName().indexOf(" ")) + ".txt\"");
		cmd.setMessage("Workitem built by Jenkins build job " + build.getFullDisplayName());
		cmd.setURLLink("http://" + controllerName + ":" + controllerPort + "/job/" + build.getFullDisplayName().substring(0, build.getFullDisplayName().indexOf(" ")) + "/" + build.getNumber());

		StringBuffer strBuf = new StringBuffer();
		try {
			client.joinWithPossibleTimeout(client.run(cmd.getArguments(), build.getEnvironment(listener).get("TOOLS_FOLDER") + "\\RTCWorkItemLinker\\run2.bat"), listener, strBuf, build, config.getPassword());
		} catch (Exception e) {
			listener.error("" + e);
			listener.error("Continuing");
		}
		String stdOut = strBuf.toString();
		
		if (stdOut.indexOf("List of all change sets since last build for ") > 0) {
			changes = new ArrayList();
		}

		//parse stdOut to assign value to change log
		int componentIndex = 0;
		while (stdOut.indexOf("List of all change sets since last build for ", componentIndex) > 0) {
			int startIndex = stdOut.indexOf("List of all change sets since last build for ", componentIndex);
			componentIndex = startIndex+1;
			StringTokenizer strtok = new StringTokenizer(stdOut.substring(startIndex), "\n");
			String nextLine = strtok.nextToken();
			//go through each line and find all workItems
			while (strtok.hasMoreElements() && !nextLine.contains("Done listing changesets.")) {
				
				if (nextLine.contains("CHANGESET: ")) {
					JazzChangeSet nextChangeSet = new JazzChangeSet();
					nextLine = nextLine.substring(nextLine.indexOf("CHANGESET: ") + 11);
					nextChangeSet.setMsg(nextLine);
					
					nextLine = strtok.nextToken();//date
					while(!nextLine.contains("DATE = ")) {
						nextLine = strtok.nextToken();//date
					}
					
					nextLine = nextLine.substring(nextLine.indexOf("DATE = ") + 7);

					GregorianCalendar calendar = new GregorianCalendar(Integer.parseInt(nextLine.substring(0, 4)),
						Integer.parseInt(nextLine.substring(5, 7)),
						Integer.parseInt(nextLine.substring(8, 10)),
						Integer.parseInt(nextLine.substring(11, 13)),
						Integer.parseInt(nextLine.substring(14, 16)),
						Integer.parseInt(nextLine.substring(17, 19)));
					nextChangeSet.setDate(calendar.getTime());
					
					nextChangeSet.setRev(nextLine);
					
					nextLine = strtok.nextToken();//user
					nextLine = nextLine.substring(nextLine.indexOf("USER = ") + 7);
					nextChangeSet.setUser(nextLine);
					
					nextLine = strtok.nextToken();//email
					nextLine = nextLine.substring(nextLine.indexOf("EMAIL = ") + 8);
					nextChangeSet.setEmail(nextLine);
					
					nextLine = strtok.nextToken();//files
					boolean done = false;
					while(!nextLine.contains("CHANGESET: ") && !done && !nextLine.contains("Done listing changesets.")) {
						if (nextLine.contains(" WORKITEM = ")) {
							nextLine = nextLine.substring(nextLine.indexOf("WORKITEM = ") + 11);
							nextChangeSet.addWorkItem(nextLine);
						} else {
							if (nextLine.contains(" FILE = ")) {
								nextLine = nextLine.substring(nextLine.indexOf("FILE = ") + 7);
								nextChangeSet.addItem(nextLine, "Affected File List");
							}
						}
						if (strtok.hasMoreElements()) {
							nextLine = strtok.nextToken();//files
						} else {
							done = true;
						}
					}
					
					changes.add(0, nextChangeSet);
				} else {
					nextLine = strtok.nextToken();
				}
			}
		}
		//Create a list of those workitems and apply it to the changes page
		return changes;
	}
	
    @Override
    public ChangeLogParser createChangeLogParser() {
        return new JazzChangeLogReader();
    }

    @Override
    public JazzRepositoryBrowser getBrowser() {
        return repositoryBrowser;
    }

    @Override
    public boolean processWorkspaceBeforeDeletion(AbstractProject<?, ?> project, FilePath workspace, Node node) throws IOException, InterruptedException {
        LogTaskListener listener = new LogTaskListener(logger, Level.INFO);
        Launcher launcher = node.createLauncher(listener);

        // Stop any daemon started for the workspace
        JazzClient client = getClientInstance(launcher, listener, workspace);
        client.stopDaemon();

        return true;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    JazzConfiguration getConfiguration(final TaskListener listener) throws IOException, InterruptedException {
        final JazzConfiguration configuration = new JazzConfiguration();
        
        final DescriptorImpl globalConfig = getDescriptor();
        
        // Note: with all of the fallbacks below, we perform the fallback here on demand
        // rather than in the .jelly file: if the global configuration changes after
        // the job is created, we want that new global configuration value to be used,
        // not just the one that was present when this job's configation was created.
        
        // Use job-specific username if specified, otherwise fall back to globally-configured
        String username = this.username;        
        if (StringUtils.isEmpty(username)) {
        	username = globalConfig.getRTCUserName();
        }
        configuration.setUsername(username);
        
        // Use job-specific password if specified, otherwise fall back to globally-configured
        Secret password = this.password;
        if (password == null || StringUtils.isEmpty(Secret.toString(password))) {
        	password = globalConfig.getRTCPassword();
        }
        configuration.setPassword(Secret.toString(password));
        
        // Use job-specific repo if specified, otherwise fall back to globally-configured
        String repositoryLocation = this.repositoryLocation;
        if (StringUtils.isEmpty(repositoryLocation)) {
            repositoryLocation = globalConfig.getRTCServerURL();
        }        
        configuration.setRepositoryLocation(repositoryLocation);
        
		// Expand environment variables such as NODE_NAME and JOB_NAME to produce the actual workspace name.
		String workspaceName = null;
		if (this.workspaceNameDynamic != null) {
			workspaceName = this.workspaceNameDynamic;
		} else {
			workspaceName = this.workspaceName;
		}
		if (this.build != null) {
			final EnvVars environment = build.getEnvironment(listener);
			workspaceName = environment.expand(workspaceName);
		}
		configuration.setWorkspaceName(workspaceName);

		configuration.setUseTimeout(useTimeout);
		configuration.setTimeoutValue(timeoutValue != null ? timeoutValue : JazzConfiguration.DEFAULT_TIMEOUT);
        
        configuration.setStreamName(streamName);
        configuration.setLoadRules(loadRules);
        configuration.setUseUpdate(useUpdate);

		return configuration;
    }

    @Extension
    public static class DescriptorImpl extends SCMDescriptor<JazzSCM> {
        private String jazzExecutable;
		private String defaultWS = "${NODE_NAME}_${JOB_NAME}";
		//private String defaultWS = "${JOB_NAME}";
		private String RTCServerURL = "defaultURL";
		private String RTCUserName = "defaultUser";
		private Secret RTCPassword = null;
		private boolean defaultUseUpdate = true;

        public DescriptorImpl() {
            super(JazzSCM.class, JazzRepositoryBrowser.class);
            load();
        }

        @Override
        public String getDisplayName() {
            return "RTC";
        }
		
		public String getDefaultWS() {
            return defaultWS;
        }
		
		public String getRTCUserName() {
            return RTCUserName;
        }
		
		public Secret getRTCPassword() {
            return RTCPassword;
        }
		
        @Override
        public SCM newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            JazzSCM scm = (JazzSCM) super.newInstance(req, formData);
            scm.repositoryBrowser = RepositoryBrowsers.createInstance(
                    JazzRepositoryBrowser.class,
                    req,
                    formData,
                    "browser");
            return scm;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            jazzExecutable = Util.fixEmptyAndTrim(req.getParameter("rtc.jazzExecutable"));
            RTCServerURL = Util.fixEmptyAndTrim(req.getParameter("rtc.RTCServerURL"));
            RTCUserName = Util.fixEmptyAndTrim(req.getParameter("rtc.RTCUserName"));
            String pass = Util.fixEmptyAndTrim(req.getParameter("rtc.RTCPassword"));
            RTCPassword = (pass == null) ? null : Secret.fromString(pass);
            
            save();
            return true;
        }

        public String getJazzExecutable() {
            if (jazzExecutable == null) {
                return JazzClient.SCM_CMD;
            } else {
                return jazzExecutable;
            }
        }
		
		public String getRTCServerURL() {
            return Util.fixEmpty(RTCServerURL);
        }
		
        public FormValidation doExecutableCheck(@QueryParameter String value) {
            return FormValidation.validateExecutable(value);
        }
        
        public FormValidation doCheckTimeoutValue(@QueryParameter String value) {
            if (StringUtils.isEmpty(value)) {
                return FormValidation.ok();
            }
            return FormValidation.validatePositiveInteger(value);
        
        }
    }
}
