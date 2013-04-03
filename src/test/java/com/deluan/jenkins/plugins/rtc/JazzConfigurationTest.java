package com.deluan.jenkins.plugins.rtc;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.Node;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

import net.vidageek.mirror.dsl.Mirror;

import org.junit.Before;
import org.junit.Test;

import com.gargoylesoftware.base.testing.EqualsTester;

/**
 * @author deluan
 */
public class JazzConfigurationTest {

    JazzConfiguration configuration;

    @Before
    public void setUp() throws Exception {
        configuration = createConfiguration();
    }

    @Test
    public void testAllSettersCalled() throws Exception {
        assertNoNullFields(configuration);
    }

    @Test
    public void testClone() throws Exception {
        JazzConfiguration clone = new JazzConfiguration(configuration);

        assertThat(clone, equalTo(configuration));
    }

    @Test
    public void testEqualsAndHash() {
        final JazzConfiguration a = new JazzConfiguration(configuration);
        final JazzConfiguration b = new JazzConfiguration(configuration); // another JazzChangeSet that has the same values as the original
        final JazzConfiguration c = new JazzConfiguration(); // another JazzChangeSet with different values
        new EqualsTester(a, b, c, null);
    }

    private void assertNoNullFields(JazzConfiguration clone) {
        List<Field> fields = new Mirror().on(JazzConfiguration.class).reflectAll().fields();

        for (Field field : fields) {
            Object value = new Mirror().on(clone).get().field(field);
            assertNotNull("Field " + field.getName() + " is null", value);
        }
    }

    private JazzConfiguration createConfiguration() throws Exception {
        JazzConfiguration configuration = new JazzConfiguration();

        configuration.setRepositoryLocation("repo");
        configuration.setWorkspaceName("workspace");
        configuration.setStreamName("stream");
        configuration.setUsername("user");
        configuration.setPassword("password");
        configuration.setJobWorkspace(new FilePath(new File("job")));
                
        AbstractBuild mockBuild = mock(AbstractBuild.class);
        EnvVars mockEnv = mock(EnvVars.class);
        TaskListener mockListener = mock(TaskListener.class);
        Node mockNode = mock(Node.class);
        when(mockBuild.getEnvironment(null)).thenReturn(mockEnv);
        when(mockBuild.getBuiltOn()).thenReturn(mockNode);
        configuration.setBuild(mockBuild);
        configuration.setTaskListener(mockListener);
        configuration.setDefaultWorkspaceName("wsn");
        configuration.setCommonWorkspaceUNC("cwu");
        configuration.setNodeName("nodename");
        configuration.setJobName("jobname");
        configuration.setAgentsUsingCommonWorkspace("aucw");

        configuration.setLoadRules("loadRules");
        
        configuration.setUseTimeout(true);
        configuration.setTimeoutValue(1L);
        return configuration;
    }
}
