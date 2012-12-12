package com.deluan.jenkins.plugins.rtc.commands;

import org.junit.Test;

import java.io.BufferedReader;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author deluan
 */
public class VersionCommandTest extends BaseCommandTest {

    private VersionCommand cmd = new VersionCommand(config);

    @Test
    public void versionCommandArguments() throws Exception {
        assertThat(cmd.getArguments().toStringWithQuote(), is("version"));
    }

    @Test
    public void versionCommandParse_2_1_0() throws Exception {
        BufferedReader reader = getReader("scm-version-2.1.0.txt");
        String result = cmd.parse(reader);
        assertThat(result, is("2.1.0"));
    }

    @Test
    public void versionCommandParse_2_0_2() throws Exception {
        BufferedReader reader = getReader("scm-version-2.0.2.txt");
        String result = cmd.parse(reader);
        assertThat(result, is("2.0.2"));
    }

    @Test
    public void parse() throws Exception {
    	BufferedReader reader = getReader("scm-version-3.1.100.txt");
    	String result = cmd.parse(reader);

    	assertThat(result, is("3.1.100"));
    }


}
