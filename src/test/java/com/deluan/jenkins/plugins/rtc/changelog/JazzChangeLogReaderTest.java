package com.deluan.jenkins.plugins.rtc.changelog;

import org.apache.commons.digester.Digester;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;

/**
 * @author deluan
 */
public class JazzChangeLogReaderTest {

    JazzChangeLogReader changeLogReader = new JazzChangeLogReader();

    @Test
    public void testEmptyChangelog() throws Exception {
        JazzChangeSetList changeSets = callParser("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<changelog></changelog>");

        assertThat(changeSets.getLogs().size(), is(0));
    }

    @Test
    public void testCompleteChangelog() throws Exception {
        JazzChangeSet originalChangeSet = createBasicChangeSet();

        JazzChangeSetList changeSets = callParser("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<changelog>" +
                "<changeset rev=\"" + originalChangeSet.getRev() + "\">" +
                "<date>" + originalChangeSet.getDateStr() + "</date>" +
                "<user>" + originalChangeSet.getUser() + "</user>" +
                "<email>" + originalChangeSet.getEmail() + "</email>" +
                "<comment>" + originalChangeSet.getMsg() + "</comment>" +
                "<files>" +
                "<file action=\"delete\">test/Class1.java</file>" +
                "<file action=\"add\">test/Class2.java</file>" +
                "</files>" +
                "<workitems>" +
                "<workitem>501 \"Just a test\"</workitem>" +
                "</workitems>" +
                "</changeset>" +
                "</changelog>");

        assertThat("There should be exactly one changeset", changeSets.getLogs().size(), is(1));

        JazzChangeSet readChangeSet = changeSets.iterator().next();

        assertThat("Rev is incorrect", readChangeSet.getRev(), is(originalChangeSet.getRev()));
        assertThat("Date is incorrect", readChangeSet.getDateStr(), is(originalChangeSet.getDateStr()));
        assertThat("User is incorrect", readChangeSet.getUser(), is(originalChangeSet.getUser()));
        assertThat("Email is incorrect", readChangeSet.getEmail(), is(originalChangeSet.getEmail()));
        assertThat("Comment is incorrect", readChangeSet.getMsg(), is(originalChangeSet.getMsg()));
        assertThat("Number of items in changeset is incorrect", readChangeSet.getItems().size(), is(2));
        assertThat("Number of work items in changeset is incorrect", readChangeSet.getWorkItems().size(), is(1));

        JazzChangeSet.Item item = readChangeSet.getItems().get(0);
        assertThat("Action is incorrect", item.getAction(), is("delete"));
        assertThat("Path is incorrect", item.getPath(), is("test/Class1.java"));

        item = readChangeSet.getItems().get(1);
        assertThat("Action is incorrect", item.getAction(), is("add"));
        assertThat("Path is incorrect", item.getPath(), is("test/Class2.java"));
        assertThat("The item's parent is not the same as the change set it belongs to", item.getParent(), is(readChangeSet));

        String workItem = readChangeSet.getWorkItems().get(0);
        assertThat("WorkItem is incorrect", workItem, is("501 \"Just a test\""));
    }

    @Test
    public void testChangelogWithTwoChangesets() throws Exception {
        JazzChangeSet originalChangeSet1 = createBasicChangeSet();
        JazzChangeSet originalChangeSet2 = createBasicChangeSet();

        JazzChangeSetList changeSets = callParser("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<changelog>" +
                "<changeset rev=\"" + originalChangeSet1.getRev() + "\">" +
                "<date>" + originalChangeSet1.getDateStr() + "</date>" +
                "<user>" + originalChangeSet1.getUser() + "</user>" +
                "<email>" + originalChangeSet1.getEmail() + "</email>" +
                "<comment>" + originalChangeSet1.getMsg() + "</comment>" +
                "</changeset>" +
                "<changeset rev=\"" + originalChangeSet2.getRev() + "\">" +
                "<date>" + originalChangeSet2.getDateStr() + "</date>" +
                "<user>" + originalChangeSet2.getUser() + "</user>" +
                "<email>" + originalChangeSet2.getEmail() + "</email>" +
                "<comment>" + originalChangeSet2.getMsg() + "</comment>" +
                "</changeset>" +
                "</changelog>");

        assertThat("There should be exactly two changeset", changeSets.getLogs().size(), is(2));

        assertThat("The change set list read should contain the two change sets",
                changeSets, hasItems(originalChangeSet1, originalChangeSet2));
    }

    private JazzChangeSetList callParser(String xml) throws IOException, SAXException {
        List<JazzChangeSet> result = new ArrayList<JazzChangeSet>();
        Reader reader = new StringReader(xml);

        Digester digester = changeLogReader.createDigester(result);
        digester.parse(reader);
        return new JazzChangeSetList(null, result);
    }

    private JazzChangeSet createBasicChangeSet() throws Exception {
        Long revNumber = Math.round(Math.random() * 10000);
        JazzChangeSet changeSet = new JazzChangeSet();
        changeSet.setRev(revNumber.toString());
        changeSet.setDate(new Date());
        changeSet.setUser("deluan");
        changeSet.setMsg("comment");
        changeSet.setEmail("deluan@email.com.br");

        return changeSet;
    }
}
