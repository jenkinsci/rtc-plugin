package com.deluan.jenkins.plugins.rtc.commands;

import com.deluan.jenkins.plugins.rtc.changelog.JazzChangeSet;
import hudson.scm.EditType;
import hudson.util.ArgumentListBuilder;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;

public class CommandTest extends BaseCommandTest {

    /*@Test
    public void maskedLoginCredentials() throws Exception {
        LoadCommand cmd = new LoadCommand(config, null);

        ArgumentListBuilder args = cmd.getArguments();
        assertThat(args.toMaskArray(), is(equalTo(new boolean[]{false, false, false, true, false, true, false, false, false, false, false})));
    }

    @Test
    public void loadCommandArguments() throws Exception {
        LoadCommand cmd = new LoadCommand(config, null);

        assertEquals("load \"My Workspace\" -u user -P password -r https://jazz/jazz -d c:\\test -f", cmd.getArguments().toStringWithQuote());
    }*/

    @Test
    public void stopDaemonCommandArguments() throws Exception {
        StopDaemonCommand cmd = new StopDaemonCommand(config);

        assertEquals("daemon stop c:\\test", cmd.getArguments().toStringWithQuote());
    }

    @Test
    public void listCommandArguments() throws Exception {
        ListCommand cmd = new ListCommand(config, Arrays.asList(TEST_REVISIONS));

        assertEquals("list changesets -u user -P password -d c:\\test 1714 1657 1652 1651 1650 1648 1645 1640 1625", cmd.getArguments().toStringWithQuote());
    }

    @Test
    public void listCommandParse() throws Exception {
        ListCommand cmd = new ListCommand(config, Arrays.asList(TEST_REVISIONS));
        Map<String, JazzChangeSet> result = callParser(cmd, "scm-list.txt", TEST_REVISIONS);

        JazzChangeSet changeSet = result.get("1714");
        assertEquals("The number of files in the changesets was incorrect", 8, changeSet.getAffectedPaths().size());
        assertEquals("The number of work itens in the changesets was incorrect", 2, changeSet.getWorkItems().size());

        JazzChangeSet.Item item = changeSet.getItems().get(0);
        assertTrue("The file is not the expected one", item.getPath().endsWith("GerenteOferta.java"));
        assertEquals("The edit type is not the expected one", EditType.EDIT, item.getEditType());

        item = changeSet.getItems().get(4);
        assertTrue("The file is not the expected one", item.getPath().endsWith("ISERetirarOfertas.java"));
        assertEquals("The edit type is not the expected one", EditType.ADD, item.getEditType());

        String workItem = changeSet.getWorkItems().get(0);
        assertTrue("The work item is not the expected one", workItem.startsWith("516"));
    }
}
