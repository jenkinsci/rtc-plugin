package com.deluan.jenkins.plugins.rtc.commands.accept;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.deluan.jenkins.plugins.rtc.changelog.JazzChangeSet;

/**
 * Test for {@link AcceptCustomOutputParser}.
 * @author vvidovic
 */
public class AcceptCustomOutputParserTest {

	public static final int RTCOUT_ACTIONFLAG_POSITION = 3;
	public static final String RTCOUT_PATTERN_FILE = "^\\s{10}---(.)-\\s+(.*)";
	public static final String RTCOUT_PATTERN_STARTCHANGESET = "^\\s{6}\\((\\d+)\\)\\s(.*)";
	public static final String RTCOUT_PATTERN_WORKITEM = "^\\s{10}[^\\d\\s]+(\\d+)[^\\d]+(.*)";
	
	public static final String TEST_OUT = "Workspace: (1002) \"RTC-workspace\"\n" +
"  Component: (1003) \"Stream MavenModule\"\n" +
"    Change sets:\n" +
"      (1159) ---$  \"jenkins test.\" 03-sij-2013 04:32 PM\n" +
"        Changes:\n" +
"          ---c- \\MOdule\\3-design\\use-case-realizations\\17-razno\\ucr-something.sequence-diagram\n" +
"        Work items:\n" +
"          (1134) 49781 \"Osposobiti CI okolinu\"";

	@Test
	public void testParse() throws ParseException, IOException {
		AcceptCustomOutputParser parser = new AcceptCustomOutputParser(RTCOUT_PATTERN_STARTCHANGESET, RTCOUT_PATTERN_FILE, RTCOUT_PATTERN_WORKITEM);
		
		StringReader sr = new StringReader(TEST_OUT);
		BufferedReader br = new BufferedReader(sr);
		Map<String, JazzChangeSet> result = parser.parse(br);
		
		Assert.assertNotNull(result);
	}

}
