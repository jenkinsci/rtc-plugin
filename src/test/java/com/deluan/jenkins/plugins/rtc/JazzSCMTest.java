package com.deluan.jenkins.plugins.rtc;

import org.jvnet.hudson.test.HudsonTestCase;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class JazzSCMTest extends HudsonTestCase {

    public void testWorkspaceName() throws Exception {
        JazzSCM scm = new JazzSCM("http://xxx", "workspace", "stream", "load rules", false);

        assertThat(scm.getWorkspaceName(), is("workspace"));
    }

}
