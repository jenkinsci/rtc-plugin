package com.deluan.jenkins.plugins.rtc.commands;

import com.deluan.jenkins.plugins.rtc.changelog.JazzChangeSet;
import hudson.scm.EditType;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.*;

public class AcceptCommandTest extends BaseCommandTest {

    /*@Test
    public void acceptCommandArguments() throws Exception {
        AcceptCommand cmd = new AcceptCommand(config, Arrays.asList(TEST_REVISIONS), "2.1.0");

        assertEquals("accept -u user -P password -d c:\\test --flow-components -o -v -c 1714 1657 1652 1651 1650 1648 1645 1640 1625", cmd.getArguments().toStringWithQuote());
    }*/

    @Test
    public void createCommandForVersion_2_0_2() throws Exception {
        AcceptCommand cmd = new AcceptCommand(config, Arrays.asList(TEST_REVISIONS), "2.0.2");
        assertThat(cmd.oldFormat, is(true));
    }

    @Test
    public void createCommandForVersion_2_1_0() throws Exception {
        AcceptCommand cmd = new AcceptCommand(config, Arrays.asList(TEST_REVISIONS), "2.1.0");
        assertThat(cmd.oldFormat, is(false));
    }

    @Test
    public void createCommandForVersion_3_0_0() throws Exception {
        AcceptCommand cmd = new AcceptCommand(config, Arrays.asList(TEST_REVISIONS), "3.0.0");
        assertThat(cmd.oldFormat, is(false));
    }

    @Test
    public void acceptCommandParse_2_1_0() throws Exception {
        AcceptCommand cmd = new AcceptCommand(config, Arrays.asList(TEST_REVISIONS), "2.1.0");
        Map<String, JazzChangeSet> result = callParser(cmd, "scm-accept-2.1.0.txt", TEST_REVISIONS);

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
        assertThat("The work item is not the expected one", workItem, startsWith("516"));
    }

    @Test
    public void acceptCommandParse_Chinese() throws Exception {
        AcceptCommand cmd = new AcceptCommand(config, Arrays.asList(TEST_REVISIONS), "2.0.2");
        Map<String, JazzChangeSet> result = callParser(cmd, "scm-accept-chinese.txt", "1019", "1021", "1020");

        JazzChangeSet changeSet = result.get("1020");
        assertEquals("The number of files in the changesets was incorrect", 2, changeSet.getAffectedPaths().size());
        assertEquals("The number of work itens in the changesets was incorrect", 1, changeSet.getWorkItems().size());

        JazzChangeSet.Item item = changeSet.getItems().get(0);
        assertTrue("The file is not the expected one", item.getPath().endsWith("com_tps_eppic_ConfigValues_core_properties"));
        assertEquals("The edit type is not the expected one", EditType.EDIT, item.getEditType());

        String workItem = changeSet.getWorkItems().get(0);
        assertThat("The work item is not the expected one", workItem, startsWith("11764"));
    }

    @Test
    public void acceptCommandParse_2_0_2() throws Exception {
        AcceptCommand cmd = new AcceptCommand(config, Arrays.asList(TEST_REVISIONS), "2.0.2");
        Map<String, JazzChangeSet> result = callParser(cmd, "scm-accept-2.0.2.txt", "1002", "1001", "1008", "1009");

        JazzChangeSet changeSet = result.get("1002");
        assertEquals("The number of files in the changesets was incorrect", 5, changeSet.getAffectedPaths().size());
        assertEquals("The number of work itens in the changesets was incorrect", 1, changeSet.getWorkItems().size());

        JazzChangeSet.Item item = changeSet.getItems().get(3);
        assertTrue("The file is not the expected one", item.getPath().endsWith("FabricaEJBBean.java"));
        assertEquals("The edit type is not the expected one", EditType.EDIT, item.getEditType());

        item = changeSet.getItems().get(2);
        assertTrue("The file is not the expected one", item.getPath().endsWith("pom.xml"));
        assertEquals("The edit type is not the expected one", EditType.ADD, item.getEditType());

        item = changeSet.getItems().get(0);
        assertEquals("The edit type is not the expected one", EditType.EDIT, item.getEditType());

        item = changeSet.getItems().get(4);
        assertTrue("The file is not the expected one", item.getPath().endsWith("ConsultaConta.java"));
        assertEquals("The edit type is not the expected one", EditType.DELETE, item.getEditType());

        String workItem = changeSet.getWorkItems().get(0);
        assertThat("The work item is not the expected one", workItem, is("3076: Build process for Solution Accelerator"));
    }

}
