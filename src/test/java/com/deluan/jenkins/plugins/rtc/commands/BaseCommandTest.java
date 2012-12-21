package com.deluan.jenkins.plugins.rtc.commands;

import com.deluan.jenkins.plugins.rtc.JazzConfiguration;
import com.deluan.jenkins.plugins.rtc.changelog.JazzChangeSet;
import hudson.FilePath;
import org.junit.Before;

import java.io.*;
import java.text.ParseException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author deluan
 */
abstract public class BaseCommandTest {

    protected static final String[] TEST_REVISIONS_2_1_0 = new String[]{"1714", "1657", "1652", "1651", "1650", "1648", "1645", "1640", "1625"};
    protected static final String[] TEST_REVISIONS_3_1_0 = new String[]{"1010", "1009", "1007", "1008", "1006", "1004"};
    protected static final String[] TEST_REVISIONS_3_1_0_JSON = new String[]{"_etjOekOlEeKJEZt6NdfIqg", "_K4DX40SbEeKJFZt6NdfIqg", "_r_6ZYUY3EeKJIJt6NdfIqg", "_d2oMcUU4EeKJGJt6NdfIqg", "_F9ue60Y7EeKJIJt6NdfIqg", "_RBKcUUiaEeKJLpt6NdfIqg"};

    protected JazzConfiguration config;

    @Before
    public void setUp() {
        config = new JazzConfiguration();
        config.setRepositoryLocation("https://jazz/jazz");
        config.setWorkspaceName("My Workspace");
        config.setStreamName("My Stream");
        config.setUsername("user");
        config.setPassword("password");
        config.setJobWorkspace(new FilePath(new File("c:\\test")));
    }

    protected Map<String, JazzChangeSet> callParser(ParseableCommand<Map<String, JazzChangeSet>> cmd, String fileName, String... revisionsExpected) throws ParseException, IOException {
        BufferedReader reader = getReader(fileName);

        Map<String, JazzChangeSet> result = cmd.parse(reader);

        assertEquals("The number of change sets in the list was incorrect", revisionsExpected.length, result.size());

        for (String rev : revisionsExpected) {
            assertNotNull("Change set (" + rev + ") not in result", result.get(rev));
        }
        return result;
    }

    protected BufferedReader getReader(String fileName) {
        InputStream in = getClass().getResourceAsStream(fileName);
        return new BufferedReader(new InputStreamReader(in));
    }
}
