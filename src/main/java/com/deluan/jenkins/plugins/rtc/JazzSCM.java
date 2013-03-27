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
    private String streamName;
    private String username;
    private Secret password;
    private String loadRules;
	private boolean useUpdate;
    private String commonWorkspaceUNC;
    private String agentsUsingCommonWorkspace;
    
    AbstractBuild build;

    private JazzRepositoryBrowser repositoryBrowser;

    @DataBoundConstructor
    public JazzSCM(String repositoryLocation, String workspaceName, String streamName,
                   String username, String password,
                   String loadRules, boolean useUpdate) {
        this.repositoryLocation = repositoryLocation;
        this.workspaceName = workspaceName;
        this.streamName = streamName;
        this.username = username;
        this.password = StringUtils.isEmpty(password) ? null : Secret.fromString(password);
        this.loadRules = loadRules;
		this.useUpdate = useUpdate;
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
			JazzClient client = getClientInstance(launcher, listener, workspace);
			try {
				//return PollingResult.SIGNIFICANT;
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
		
        JazzClient client = getClientInstance(launcher, listener, workspace);
		
		FilePath file = build.getWorkspace();
		
		if (!useUpdate) {
			try {
				file.act(new CleanWorkspace(file.getRemote()));
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
		
		JazzConfiguration config = getConfiguration(listener);
		try {
			path.act(new com.deluan.jenkins.plugins.rtc.commands.LoadCommand.RemoteFileWriter(path.getRemote() + File.separator + build.getFullDisplayName().substring(0, build.getFullDisplayName().indexOf(" ")) + ".txt", config.getLoadRules()));
		} catch (Exception e) {
			e.printStackTrace();
			config.consoleOut("exception: " + e);
			config.consoleOut("Caused by: " + e.getCause());
		}
		
		UpdateWorkItemsCommand cmd = new UpdateWorkItemsCommand(config);

		//set parameters
		cmd.setUserName(config.getUsername());
		cmd.setPassword(config.getPassword());
		cmd.setWorkspaceName(config.getWorkspaceName());
		cmd.setTimeToCheck("" + lastBuild.getTimeInMillis());
		cmd.setLoadRulesFileName(File.separator + path.getRemote() + File.separator + build.getFullDisplayName().substring(0, build.getFullDisplayName().indexOf(" ")) + ".txt\"");
		cmd.setMessage("Workitem built by Jenkins build job " + build.getFullDisplayName());
		cmd.setURLLink("http://" + controllerName + ":" + controllerPort + "/job/" + build.getFullDisplayName().substring(0, build.getFullDisplayName().indexOf(" ")) + "/" + build.getNumber());

		StringBuffer strBuf = new StringBuffer();
		try {
			client.joinWithPossibleTimeout(client.run(cmd.getArguments(), build.getEnvironment(listener).get("TOOLS_FOLDER") + "\\RTCWorkItemLinker\\run2.bat"), true, listener, strBuf, build, config.getPassword());
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
		String workspaceName = this.workspaceName;
		if (this.build != null) {
			final EnvVars environment = build.getEnvironment(listener);
			workspaceName = environment.expand(workspaceName);
		}
		configuration.setWorkspaceName(workspaceName);

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
            jazzExecutable = Util.fixEmpty(req.getParameter("rtc.jazzExecutable").trim());
			RTCServerURL = Util.fixEmpty(req.getParameter("rtc.RTCServerURL").trim());
			RTCUserName = Util.fixEmpty(req.getParameter("rtc.RTCUserName").trim());
			RTCPassword = Secret.fromString(Util.fixEmpty(req.getParameter("rtc.RTCPassword")).trim());
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
    }
}
