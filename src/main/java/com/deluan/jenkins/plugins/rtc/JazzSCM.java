package com.deluan.jenkins.plugins.rtc;

import com.deluan.jenkins.plugins.rtc.changelog.JazzChangeLogReader;
import com.deluan.jenkins.plugins.rtc.changelog.JazzChangeLogWriter;
import com.deluan.jenkins.plugins.rtc.changelog.JazzChangeSet;
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
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Date;

/**
 * @author deluan
 */
@SuppressWarnings("UnusedDeclaration")
public class JazzSCM extends SCM {

    private static final Logger logger = Logger.getLogger(JazzClient.class.getName());

    private String repositoryLocation;
    private String workspaceName;
	private String workspaceName2;
    private String streamName;
    private String username;
    private Secret password;
	private static AbstractBuild build;

    private JazzRepositoryBrowser repositoryBrowser;

    @DataBoundConstructor
    public JazzSCM(String repositoryLocation, String workspaceName, String streamName,
                   String username, String password) {

        this.repositoryLocation = repositoryLocation;
        this.workspaceName = workspaceName;
        this.streamName = streamName;
        this.username = username;
        this.password = StringUtils.isEmpty(password) ? null : Secret.fromString(password);
    }
	
	//public String getDefaultWS() {
	//	return defaultWS;
	//}
	
	public static AbstractBuild getAbstractBuild() {
		return build;
	}

    public String getRepositoryLocation() {
        return repositoryLocation;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }
	
	public String getDefaultWorkspaceName() {
        return StringUtils.defaultString(workspaceName, "${NODE_NAME}_${JOB_NAME}");
    }

    public String getStreamName() {
        return streamName;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return Secret.toString(password);
    }

    private JazzClient getClientInstance(Launcher launcher, TaskListener listener, FilePath jobWorkspace) {
        return new JazzClient(launcher, listener, jobWorkspace, getDescriptor().getJazzExecutable(),
                getConfiguration());
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
			String nodeName = node.getNodeName();
			workspaceName2 = workspaceName.replace("${NODE_NAME}", nodeName);
			workspaceName2 = workspaceName2.replace("${JOB_NAME}", build.getProject().getName());
			//listener.error("compare remote rev");
			try {
				FileOutputStream fos = new FileOutputStream("N:\\temp\\RTCPluginLog.txt", true);
				fos.write("compare remote rev\n".getBytes());
				fos.flush();
				fos.close();
			} catch (Exception e) {
			}
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
		Node node = build.getBuiltOn();
        String nodeName = node.getNodeName();
		workspaceName2 = workspaceName.replace("${NODE_NAME}", nodeName);
		workspaceName2 = workspaceName2.replace("${JOB_NAME}", build.getProject().getName());
		/*listener.error("workspaceName = " + workspaceName);
		listener.error("nodeName = " + nodeName);
		//listener.error("job name = " + build.getDisplayName());
		listener.error("job name = " + build.getProject().getName());
		listener.error("job f name = " + build.getProject().getFullName());
		listener.error("job d name = " + build.getProject().getDisplayName());*/
		
		//listener.error("workspaceName = " + workspaceName);
		
        JazzClient client = getClientInstance(launcher, listener, workspace);
		
		//check if workspace exists and if not than create it
		boolean result = client.workspaceExists();
		
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

        if (!changes.isEmpty()) {
            JazzChangeLogWriter writer = new JazzChangeLogWriter();
            writer.write(changes, changelogFile);
        } else {
            createEmptyChangeLog(changelogFile, listener, "changelog");
        }

        return true;
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

    public JazzConfiguration getConfiguration() {
        JazzConfiguration configuration = new JazzConfiguration();
        configuration.setUsername(username);
        configuration.setPassword(Secret.toString(password));
        configuration.setRepositoryLocation(repositoryLocation);
        configuration.setStreamName(streamName);
        configuration.setWorkspaceName(workspaceName2);

        return configuration;
    }

    @Extension
    public static class DescriptorImpl extends SCMDescriptor<JazzSCM> {
        private String jazzExecutable;
		private String defaultWS = "${NODE_NAME}_${JOB_NAME}";
		private String RTCServerURL = "defaultURL";

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
            if (RTCServerURL == null) {
                return "";
            } else {
                return RTCServerURL;
            }
        }

        public FormValidation doExecutableCheck(@QueryParameter String value) {
            return FormValidation.validateExecutable(value);
        }
    }
}
