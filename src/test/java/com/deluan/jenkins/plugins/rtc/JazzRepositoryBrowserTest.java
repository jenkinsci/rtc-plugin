package com.deluan.jenkins.plugins.rtc;

import com.deluan.jenkins.plugins.rtc.changelog.JazzChangeSet;
import com.deluan.jenkins.plugins.rtc.changelog.JazzChangeSetList;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URL;
import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * User: deluan
 * Date: 04/11/11
 */
public class JazzRepositoryBrowserTest {

    private static final String SERVER_URL = "http://jazzserver:9443/jazz";

    @Mock
    AbstractBuild build;
    @Mock
    AbstractProject<?, ?> project;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(build.getProject()).thenReturn(project);
        when(project.getScm()).thenReturn(new JazzSCM(SERVER_URL, null, null, null, false));
    }

    @Test
    public void testGetWorkItemLink() throws Exception {
        JazzRepositoryBrowser browser = new JazzRepositoryBrowser();
        JazzChangeSet changeSet = new JazzChangeSet();
        new JazzChangeSetList(build, Arrays.asList(changeSet));
        String workItem = "503 \"This is a test\"";
        changeSet.addWorkItem(workItem);
        assertThat(browser.getWorkItemLink(changeSet, workItem), is(new URL(SERVER_URL + "/resource/itemName/com.ibm.team.workitem.WorkItem/503")));
    }
}
