package com.deluan.jenkins.plugins.rtc.commands.accept;

import com.deluan.jenkins.plugins.rtc.changelog.JazzChangeSet;
import hudson.scm.EditType;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Chris Cosby <ccosby@gmail.com>
 */

abstract public class JsonAcceptOutputParser {
    protected JsonAcceptOutputParser() {
    }

    /*
     * The intention here is to parse JSON output of 'scm accept' but I'm out of time.
     */
    protected void doSomething(BufferedReader reader) {
        String line;
        StringBuilder json = new StringBuilder();
        try {
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // And this is parsing the output of the compare command, not accept
        JSONObject jazzCompareObj = JSONObject.fromObject(json.toString());
        JSONArray jazzDirections = jazzCompareObj.getJSONArray("direction");
        for (
                Iterator<JSONObject> jazzCompareIter = jazzDirections.iterator();
                jazzCompareIter.hasNext(); )

        {
            JSONObject direction = jazzCompareIter.next();
            JSONArray components = direction.getJSONArray("components");
            boolean incomingChanges = direction.getBoolean("incoming-changes");
            boolean outgoingChanges = direction.getBoolean("outgoing-changes");

            if (!incomingChanges) continue;

            for (Iterator<JSONObject> jazzComponentIter = components.iterator(); jazzComponentIter.hasNext(); ) {
                JSONObject component = jazzComponentIter.next();
                System.out.println("uuid: " + component.getString("uuid"));
                JSONArray changesets = component.getJSONArray("changesets");

                for (Iterator<JSONObject> jazzChangesetIter = changesets.iterator(); jazzChangesetIter.hasNext(); ) {
                    JSONObject changeset = jazzChangesetIter.next();
                }
            }
        }
    }


    protected Pattern startChangesetPattern;
    protected Pattern filePattern;
    protected Pattern workItemPattern;

    public JsonAcceptOutputParser(String startChangesetPattern, String filePattern, String workItemPattern) {
        this.workItemPattern = Pattern.compile(workItemPattern);
        this.startChangesetPattern = Pattern.compile(startChangesetPattern);
        this.filePattern = Pattern.compile(filePattern);
    }

    public Map<String, JazzChangeSet> parse(BufferedReader reader) throws ParseException, IOException {
        Map<String, JazzChangeSet> result = new HashMap<String, JazzChangeSet>();

        String line;
        JazzChangeSet changeSet = null;
        Matcher matcher;

        while ((line = reader.readLine()) != null) {
            if ((matcher = startChangesetPattern.matcher(line)).matches()) {
                if (changeSet != null) {
                    result.put(changeSet.getRev(), changeSet);
                }
                changeSet = new JazzChangeSet();
                changeSet.setRev(matcher.group(1));
            } else if ((matcher = filePattern.matcher(line)).matches()) {
                assert changeSet != null;
                String action = parseAction(matcher.group(1));
                String path = parsePath(matcher.group(2));
                changeSet.addItem(path, action);
            } else if ((matcher = workItemPattern.matcher(line)).matches()) {
                assert changeSet != null;
                changeSet.addWorkItem(parseWorkItem(matcher.group(2)));
            }
        }

        if (changeSet != null) {
            result.put(changeSet.getRev(), changeSet);
        }

        return result;
    }

    abstract protected String parseWorkItem(String string);

    abstract protected String parseEditFlag(String string);

    protected String parsePath(String string) {
        String path = string.replaceAll("\\\\", "/").trim();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }

    protected String parseAction(String string) {
        String flag = parseEditFlag(string);
        String action = EditType.EDIT.getName();
        if ("a".equals(flag)) {
            action = EditType.ADD.getName();
        } else if ("d".equals(flag)) {
            action = EditType.DELETE.getName();
        }
        return action;
    }
}
