package com.deluan.jenkins.plugins.rtc;

import com.deluan.jenkins.plugins.rtc.changelog.JazzChangeSet;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.scm.RepositoryBrowser;
import hudson.scm.SCM;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author deluan
 */
public class JazzRepositoryBrowser extends RepositoryBrowser<JazzChangeSet> {

    @DataBoundConstructor
    public JazzRepositoryBrowser() {
    }

    private String getBaseUrlString(JazzChangeSet changeSet) throws MalformedURLException {
        AbstractProject<?, ?> project = changeSet.getParent().build.getProject();
        SCM scm = project.getScm();
        if (scm instanceof JazzSCM) {
            return ((JazzSCM) scm).getRepositoryLocation();
        } else {
            throw new IllegalStateException("Jazz repository browser used on a non Jazz SCM");
        }
    }

    @Override
    public URL getChangeSetLink(JazzChangeSet changeSet) throws IOException {
        return new URL(getBaseUrlString(changeSet)); // TODO
    }

    // ${repositoryUrl}/resource/itemName/com.ibm.team.workitem.WorkItem/${alias}
    public URL getWorkItemLink(JazzChangeSet changeSet, String workItem) throws IOException {
        String[] parts = workItem.split(" ");
        String baseUrl = getBaseUrlString(changeSet);
        if(baseUrl.endsWith("/")) {
        	baseUrl = baseUrl.substring(0, baseUrl.length() -1);
        }
        String url = baseUrl + "/resource/itemName/com.ibm.team.workitem.WorkItem/" + parts[0];
        return new URL(url);
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<RepositoryBrowser<?>> {

        public DescriptorImpl() {
            super(JazzRepositoryBrowser.class);
        }

        @Override
        public String getDisplayName() {
            return "Jazz Web Client";
        }

    }


}
