package com.deluan.jenkins.plugins.rtc.changelog;

import com.gargoylesoftware.base.testing.EqualsTester;
import hudson.scm.EditType;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * User: deluan
 * Date: 04/11/11
 */
public class JazzChangeSetTest {

    @Test
    public void testAddItem() throws Exception {
        JazzChangeSet changeSet = new JazzChangeSet();

        JazzChangeSet.Item item = new JazzChangeSet.Item("test/Class1.java", "edit");
        assertEquals(item.getEditType(), EditType.EDIT);

        changeSet.addItem(item);

        assertEquals(1, changeSet.getItems().size());

        JazzChangeSet.Item addedItem = changeSet.getItems().get(0);
        assertSame(item, addedItem);
    }

    @Test
    public void testAddItemWithDiscretParams() throws Exception {
        JazzChangeSet changeSet = new JazzChangeSet();
        changeSet.addItem("test/Class1.java", "add");

        assertEquals(1, changeSet.getItems().size());

        JazzChangeSet.Item addedItem = changeSet.getItems().get(0);
        assertEquals("test/Class1.java", addedItem.getPath());
    }

    @Test
    public void testAddedItem() throws Exception {
        JazzChangeSet changeSet = new JazzChangeSet();
        changeSet.addItem("test/Class1.java", "add");

        assertEquals(1, changeSet.getItems().size());

        JazzChangeSet.Item addedItem = changeSet.getItems().get(0);
        assertEquals(EditType.ADD, addedItem.getEditType());
    }

    @Test
    public void testEditedItem() throws Exception {
        JazzChangeSet changeSet = new JazzChangeSet();
        changeSet.addItem("test/Class1.java", "edit");

        assertEquals(1, changeSet.getItems().size());

        JazzChangeSet.Item addedItem = changeSet.getItems().get(0);
        assertEquals(EditType.EDIT, addedItem.getEditType());
    }

    @Test
    public void testDeletedItem() throws Exception {
        JazzChangeSet changeSet = new JazzChangeSet();
        changeSet.addItem("test/Class1.java", "delete");

        assertEquals(1, changeSet.getItems().size());

        JazzChangeSet.Item addedItem = changeSet.getItems().get(0);
        assertEquals(EditType.DELETE, addedItem.getEditType());
    }

    @Test
    public void testAddWorkItem() throws Exception {
        JazzChangeSet changeSet = new JazzChangeSet();
        changeSet.addWorkItem("501 \"Just a test\"");

        assertEquals(1, changeSet.getWorkItems().size());

        String workItem = changeSet.getWorkItems().get(0);
        assertEquals("501 \"Just a test\"", workItem);
    }

    @Test
    public void testCopyItemsFrom() throws Exception {
        JazzChangeSet changeSet1 = new JazzChangeSet();
        changeSet1.setRev("1");
        changeSet1.setUser("deluan");
        changeSet1.setEmail("deluan@email.com.br");

        JazzChangeSet changeSet2 = new JazzChangeSet();
        changeSet2.addItem("test/Class1.java", "delete");
        changeSet2.addItem("test/Class2.java", "add");
        changeSet2.addWorkItem("501 \"Just a test\"");

        changeSet1.copyItemsFrom(changeSet2);

        assertEquals(2, changeSet1.getItems().size());
        assertEquals(1, changeSet1.getWorkItems().size());

        JazzChangeSet.Item item = changeSet1.getItems().get(0);
        assertEquals("test/Class1.java", item.getPath());
    }

    @Test
    public void testGetAffectedPaths() throws Exception {
        JazzChangeSet changeSet = new JazzChangeSet();
        changeSet.addItem("test/Class1.java", "delete");
        changeSet.addItem("test/Class2.java", "add");

        List<String> affectedFiles = (List<String>) changeSet.getAffectedPaths();

        assertEquals(2, affectedFiles.size());
        assertEquals("test/Class1.java", affectedFiles.get(0));
        assertEquals("test/Class2.java", affectedFiles.get(1));
    }

    @Test
    public void testSetDate() throws Exception {
        Date date = new Date();
        String dateStr = JazzChangeSet.formatter.format(date);
        JazzChangeSet changeSet = new JazzChangeSet();

        changeSet.setDate(date);
        assertEquals(dateStr, changeSet.getDateStr());
    }

    @Test
    public void testSetDateStr() throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        Date date = calendar.getTime();

        String dateStr = JazzChangeSet.formatter.format(date);
        JazzChangeSet changeSet = new JazzChangeSet();

        changeSet.setDateStr(dateStr);
        assertEquals(date, changeSet.getDate());
    }

    @Test
    public void testCompareByDates() throws Exception {
        Date date = new Date();

        JazzChangeSet changeSet1 = new JazzChangeSet();
        changeSet1.setDate(date);
        JazzChangeSet changeSet2 = new JazzChangeSet();
        changeSet2.setDate(date);

        assertEquals(0, changeSet1.compareTo(changeSet2));

        changeSet2.setDateStr("2011-11-04-10:36:00");
        assertEquals(1, changeSet1.compareTo(changeSet2));
    }

    private JazzChangeSet createChangeSet(String rev, Date date, String user, String email, String msg) {
        JazzChangeSet changeSet = new JazzChangeSet();
        changeSet.setRev(rev);
        changeSet.setDate(date);
        changeSet.setUser(user);
        changeSet.setMsg(msg);
        changeSet.setEmail(email);

        return changeSet;
    }

    @Test
    public void testChangeSetEqualsAndHash() {
        Date date = new Date();
        final JazzChangeSet a = createChangeSet("1", date, "deluan", "email@a.com", "msg"); // original JazzChangeSet
        final JazzChangeSet b = createChangeSet("1", date, "deluan", "email@a.com", "msg"); // another JazzChangeSet that has the same values as the original
        final JazzChangeSet c = createChangeSet("2", date, "user", "user@a.com", "msg2");   // another JazzChangeSet with different values
        new EqualsTester(a, b, c, null);
    }
    
    @Test
    public void testItemsEqualsAndHash() {
        final JazzChangeSet.Item a = new JazzChangeSet.Item("path", "action");
        final JazzChangeSet.Item b = new JazzChangeSet.Item("path", "action"); // another JazzChangeSet that has the same values as the original
        final JazzChangeSet.Item c = new JazzChangeSet.Item("path2", "action2"); // another JazzChangeSet with different values
        final JazzChangeSet.Item d = new JazzChangeSet.Item("path", "action") {
        };
        new EqualsTester(a, b, c, d);
    }
}
