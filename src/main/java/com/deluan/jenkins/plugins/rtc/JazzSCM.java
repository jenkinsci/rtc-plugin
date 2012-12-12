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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private Boolean useTimeout;
    private Long timeoutValue;
    private JazzRepositoryBrowser repositoryBrowser;
    private String version;

    @DataBoundConstructor
    public JazzSCM(String repositoryLocation, String workspaceName, String streamName,
                   String username, String password, Boolean useTimeout, Long timeoutValue) {

        this.repositoryLocation = repositoryLocation;
        this.workspaceName = workspaceName;
        this.streamName = streamName;
        this.username = username;
        this.password = StringUtils.isEmpty(password) ? null : Secret.fromString(password);
        this.useTimeout = useTimeout;
        this.timeoutValue = timeoutValue;
    }

    public String getRepositoryLocation() {
        return repositoryLocation;
    }

    public String getWorkspaceName() {
        return workspaceName;
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

    public Boolean getUseTimeout() {
        return useTimeout;
    }

    public Long getTimeoutValue() {
        return timeoutValue;
    }

    private String getVersion() {
        if (this.version == null) {
            try {
                this.version = getDescriptor().retrieveScmVersion(getDescriptor().getJazzExecutable());
                logger.info("Detected scm version: " + this.version);
            } catch (Exception e) {
            	logger.log(Level.SEVERE, "Could not instantiate a JazzClient!", e);
            }

            if (this.version == null) {
                throw new RuntimeException("Could not determine scm version!");
            }
        }

        return this.version;
    }

    private JazzClient getClientInstance(Launcher launcher, TaskListener listener, FilePath jobWorkspace) {
        JazzClient client = new JazzClient(getDescriptor().getJazzExecutable(), jobWorkspace, getConfiguration(), launcher, listener);
        client.setVersion(getVersion());
        return client;
    }

    @Override
    public SCMRevisionState calcRevisionsFromBuild(AbstractBuild<?, ?> build, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
        return null; // This implementation is not necessary, as this information is obtained from the remote RTC's repository
    }

    @Override
    protected PollingResult compareRemoteRevisionWith(AbstractProject<?, ?> project, Launcher launcher, FilePath workspace, TaskListener listener, SCMRevisionState baseline) throws IOException, InterruptedException {
        JazzClient client = getClientInstance(launcher, listener, workspace);
        try {
            return (client.hasChanges()) ? PollingResult.SIGNIFICANT : PollingResult.NO_CHANGES;
        } catch (Exception e) {
            return PollingResult.NO_CHANGES;
        }
    }

    @Override
    public boolean checkout(AbstractBuild<?, ?> build, Launcher launcher, FilePath workspace, BuildListener listener, File changelogFile) throws IOException, InterruptedException {
        JazzClient client = getClientInstance(launcher, listener, workspace);

        // Forces a load of the workspace. If it's already loaded, the 'scm load' command will do nothing.
        boolean loadOk = client.load();
        if (!loadOk) {
            return false;
        }

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
        configuration.setWorkspaceName(workspaceName);
        configuration.setUseTimeout(useTimeout);
        configuration.setTimeoutValue(timeoutValue != null ? timeoutValue : JazzConfiguration.DEFAULT_TIMEOUT);

        return configuration;
    }

    @Extension
    public static class DescriptorImpl extends SCMDescriptor<JazzSCM> {
        private String jazzExecutable;

        public DescriptorImpl() {
            super(JazzSCM.class, JazzRepositoryBrowser.class);
            load();
        }

        @Override
        public String getDisplayName() {
            return "RTC";
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

        private String retrieveScmVersion(String exePath) throws IOException, InterruptedException {
            JazzConfiguration configuration = new JazzConfiguration();
            configuration.setUseTimeout(true);
            configuration.setTimeoutValue(60L);
            JazzClient client = new JazzClient(exePath, null, configuration);
            return client.getVersion();
        }

        public FormValidation doExecutableCheck(@QueryParameter String value) {
            return FormValidation.validateExecutable(value, new FormValidation.FileValidator() {
                @Override
                public FormValidation validate(File f) {
                    String exePath = f.getAbsolutePath();
                    try {
                        String version = retrieveScmVersion(exePath);
                        if (version != null) {
                            return FormValidation.ok("Version " + version + " found");
                        }
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Error validating executable '" + exePath + "'", e);
                    }
                    return FormValidation.error("Couldn't execute '" + exePath + " version' command");
                }
            });
        }

        public FormValidation doCheckTimeoutValue(@QueryParameter String value) {
            if (StringUtils.isEmpty(value)) {
                return FormValidation.ok();
            }
            return FormValidation.validatePositiveInteger(value);
        }
    }
}
