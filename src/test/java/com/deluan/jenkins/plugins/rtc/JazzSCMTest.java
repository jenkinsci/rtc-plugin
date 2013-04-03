package com.deluan.jenkins.plugins.rtc;

import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import hudson.EnvVars;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.util.Secret;
import hudson.util.SecretTestHelper;

import org.junit.Before;
import org.junit.Test;

import com.deluan.jenkins.plugins.rtc.JazzSCM.DescriptorImpl;

public class JazzSCMTest {

    private TaskListener mockedTaskListener;
    private DescriptorImpl mockDescriptor;
    private JazzConfiguration config;

    @Before
    public void setUp() {
        SecretTestHelper.setSecret("secretKey");
        
        this.mockedTaskListener = mock(TaskListener.class);
        this.mockDescriptor = mock(DescriptorImpl.class);

        when(mockDescriptor.getRTCServerURL()).thenReturn("globalserverurl");
        when(mockDescriptor.getRTCUserName()).thenReturn("globalusername");
        when(mockDescriptor.getRTCPassword()).thenReturn(Secret.fromString("globalpassword"));
        
        // A sample configuration object
        this.config = new JazzConfiguration();
        this.config.setRepositoryLocation("repo");
        this.config.setWorkspaceName("workspace");
        this.config.setStreamName("streamname");
        this.config.setUsername("username");
        this.config.setPassword("password");
        this.config.setLoadRules("loadRules");
        this.config.setUseUpdate(true);
    }
    
    @Test
    public void testWorkspaceName() throws Exception {
        config.setWorkspaceName("my workspace name");
        JazzSCM scm = createTestableJazzSCM(config);
        assertThat(scm.getWorkspaceName(), is("my workspace name"));
    }
    
    // Tests that check we persist our values and build our JazzConfiguration object correctly   

    @Test
    public void jobSpecificUsername() throws Exception {
        config.setUsername("jobusername");
        JazzSCM scm = createTestableJazzSCM(config);
        
        JazzConfiguration createdConfig = scm.getConfiguration(this.mockedTaskListener);
        
        assertThat(createdConfig.getUsername(), is("jobusername"));
        assertThat(scm.getUsername(), is("jobusername"));
    }

    @Test
    public void usernameFallingBackToGlobal() throws Exception {
        config.setUsername("");
        JazzSCM scm = createTestableJazzSCM(config);
        
        JazzConfiguration createdConfig = scm.getConfiguration(this.mockedTaskListener);

        assertThat(createdConfig.getUsername(), is("globalusername"));    
        assertThat("Should persist job-specific blank username", scm.getUsername(), is(""));
    }

    @Test
    public void jobSpecificPassword() throws Exception {
        config.setPassword("jobpassword");
        JazzSCM scm = createTestableJazzSCM(config);
        
        JazzConfiguration createdConfig = scm.getConfiguration(this.mockedTaskListener);
        
        assertThat(createdConfig.getPassword(), is("jobpassword"));
        assertThat(scm.getPassword(), is("jobpassword"));
    }

    @Test
    public void passwordFallingBackToGlobal() throws Exception {
        config.setPassword("");
        JazzSCM scm = createTestableJazzSCM(config);
        
        JazzConfiguration createdConfig = scm.getConfiguration(this.mockedTaskListener);

        assertThat(createdConfig.getPassword(), is("globalpassword"));    
        assertThat("Should persist job-specific blank password", scm.getPassword(), is(""));
    }

    @Test
    public void jobSpecificRepositoryURL() throws Exception {
        config.setRepositoryLocation("jobrepo");
        JazzSCM scm = createTestableJazzSCM(config);
        
        JazzConfiguration createdConfig = scm.getConfiguration(this.mockedTaskListener);
        
        assertThat(createdConfig.getRepositoryLocation(), is("jobrepo"));
        assertThat(scm.getRepositoryLocation(), is("jobrepo"));
    }

    @Test
    public void repositoryURLFallingBackToGlobal() throws Exception {
        config.setRepositoryLocation("");
        JazzSCM scm = createTestableJazzSCM(config);
        
        JazzConfiguration createdConfig = scm.getConfiguration(this.mockedTaskListener);

        assertThat(createdConfig.getRepositoryLocation(), is("globalserverurl"));    
        assertThat("Should persist job-specific blank repo URL", scm.getRepositoryLocation(), is(""));
    }

    @Test
    public void workspaceNameInConfigExpandsVariables() throws Exception {
        config.setWorkspaceName("Workspace ${JOB_NAME}_${NODE_NAME}");
        JazzSCM scm = createTestableJazzSCM(config);
        
        AbstractBuild mockBuild = mock(AbstractBuild.class);
        scm.build = mockBuild;
        EnvVars envVars = new EnvVars();
        envVars.put("JOB_NAME", "jobname");
        envVars.put("NODE_NAME", "nodename");        
        when(mockBuild.getEnvironment(mockedTaskListener)).thenReturn(envVars);
        
        JazzConfiguration createdConfig = scm.getConfiguration(this.mockedTaskListener);

        assertThat(createdConfig.getWorkspaceName(), is("Workspace jobname_nodename"));    
        assertThat("We should persist the unexpanded version of the workspace name",
                scm.getWorkspaceName(), is("Workspace ${JOB_NAME}_${NODE_NAME}"));
    }

    /**
     * Creates a JazzSCM object with values taken from a sample config object (to make it easier to pass values around).
     */
    private JazzSCM createTestableJazzSCM(JazzConfiguration jobConfig) {
        JazzSCMWithCustomDescriptor result = new JazzSCMWithCustomDescriptor(
                jobConfig.getRepositoryLocation(),
                jobConfig.getWorkspaceName(),
                jobConfig.getStreamName(),
                jobConfig.getUsername(),
                jobConfig.getPassword(),
                jobConfig.isUseTimeout(),
                jobConfig.getTimeoutValue(),
                jobConfig.getLoadRules(),
                jobConfig.getUseUpdate());
        
        result.setDescriptor(this.mockDescriptor);
        return result;
    }
    
    /**
     * Subclass of JazzSCM which allows us to take control of the returned descriptor.
     */
    private static class JazzSCMWithCustomDescriptor extends JazzSCM {
        private DescriptorImpl descriptor;

        public JazzSCMWithCustomDescriptor(String repositoryLocation,
                String workspaceName, String streamName,
                String username, String password,
                boolean useTimeout, Long timeoutValue,
                String loadRules, boolean useUpdate) {
            super(repositoryLocation, workspaceName, streamName, username, password,
                    useTimeout, timeoutValue,
                    loadRules, useUpdate);
        }
        
        public void setDescriptor(DescriptorImpl descriptor) {
            this.descriptor = descriptor;
        }
        
        @Override
        public DescriptorImpl getDescriptor() {
            return this.descriptor;
        }    
    }
}
